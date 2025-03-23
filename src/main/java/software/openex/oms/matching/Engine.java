/*
 * ISC License
 *
 * Copyright (c) 2025, Alireza Pourtaghi <lirezap@protonmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package software.openex.oms.matching;

import jdk.jfr.Event;
import org.slf4j.Logger;
import software.openex.oms.binary.order.*;
import software.openex.oms.binary.order.book.FetchOrderBook;
import software.openex.oms.matching.event.CancelOrderEvent;
import software.openex.oms.matching.event.FetchOrderBookEvent;
import software.openex.oms.storage.ThreadSafeAtomicFile;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.foreign.Arena.ofConfined;
import static java.math.BigDecimal.ZERO;
import static java.nio.file.Path.of;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;
import static software.openex.oms.context.AppContext.context;

/**
 * A buy/sell orders matching engine.
 *
 * @author Alireza Pourtaghi
 */
public final class Engine implements Closeable {
    private static final Logger logger = getLogger(Engine.class);

    private final ExecutorService executor;
    private final ExecutorService eventsSynchronizerExecutor;
    private final PriorityQueue<LimitOrder> buyOrders;
    private final PriorityQueue<LimitOrder> sellOrders;
    private final ThreadSafeAtomicFile eventsFile;
    private final Matcher matcher;
    private final EventsSynchronizer eventsSynchronizer;

    public Engine(final String symbol, final int initialCapacity, final boolean start) {
        this.executor = newSingleThreadExecutor();
        this.eventsSynchronizerExecutor = newSingleThreadExecutor();
        this.buyOrders = new PriorityQueue<>(initialCapacity);
        this.sellOrders = new PriorityQueue<>(initialCapacity);
        this.eventsFile = eventsFile(symbol);
        this.matcher = new Matcher(this.executor, this.buyOrders, this.sellOrders, this.eventsFile);
        this.eventsSynchronizer = new EventsSynchronizer(this.eventsSynchronizerExecutor, this.eventsFile);

        if (start) {
            startMatching();
            startSyncing();
        }
    }

    public void startMatching() {
        executor.execute(matcher);
    }

    public void startSyncing() {
        eventsSynchronizerExecutor.execute(eventsSynchronizer);
    }

    public boolean isInSync() {
        return eventsSynchronizer.isInSync();
    }

    public CompletableFuture<Void> offer(final BuyLimitOrder order) {
        final var future = new CompletableFuture<Void>();
        executor.execute(() -> {
            if (buyOrders.offer(order)) {
                future.complete(null);
                logger.trace("offer: buy: {}", order);
            } else {
                future.completeExceptionally(new RuntimeException("could not insert buy order into queue!"));
            }
        });

        return future;
    }

    public CompletableFuture<Void> offer(final SellLimitOrder order) {
        final var future = new CompletableFuture<Void>();
        executor.execute(() -> {
            if (sellOrders.offer(order)) {
                future.complete(null);
                logger.trace("offer: sell: {}", order);
            } else {
                future.completeExceptionally(new RuntimeException("could not insert sell order into queue!"));
            }
        });

        return future;
    }

    public void offer(final BuyMarketOrder order) {
        executor.execute(new BuyMarketOrderMatcher(sellOrders, eventsFile, order));
    }

    public void offer(final SellMarketOrder order) {
        executor.execute(new SellMarketOrderMatcher(buyOrders, eventsFile, order));
    }

    public CompletableFuture<Boolean> cancel(final CancelOrder order) {
        final var event = new CancelOrderEvent();
        event.begin();

        final var future = new CompletableFuture<Boolean>();
        executor.execute(() -> {
            var found = false;

            final var bids = buyOrders.iterator();
            while (!found && bids.hasNext()) {
                final var buyOrder = bids.next();
                if (buyOrder.equals(order)) {
                    found = true;
                    if (order.get_quantity().equals(ZERO) ||
                            buyOrder.get_remaining().compareTo(order.get_quantity()) == 0) {

                        buyOrders.remove(buyOrder);
                        buyOrderCanceled(future, order, buyOrder, event);
                    } else if (buyOrder.get_remaining().compareTo(order.get_quantity()) > 0) {
                        buyOrder.set_remaining(buyOrder.get_remaining().subtract(order.get_quantity()));
                        buyOrderPartiallyCanceled(future, order, buyOrder, event);
                    } else {
                        // Found order's remaining is less than requested cancel order's quantity.
                        found = false;
                    }
                }
            }

            if (!found) {
                final var asks = sellOrders.iterator();
                while (!found && asks.hasNext()) {
                    final var sellOrder = asks.next();
                    if (sellOrder.equals(order)) {
                        found = true;
                        if (order.get_quantity().equals(ZERO) ||
                                sellOrder.get_remaining().compareTo(order.get_quantity()) == 0) {

                            sellOrders.remove(sellOrder);
                            sellOrderCanceled(future, order, sellOrder, event);
                        } else if (sellOrder.get_remaining().compareTo(order.get_quantity()) > 0) {
                            sellOrder.set_remaining(sellOrder.get_remaining().subtract(order.get_quantity()));
                            sellOrderPartiallyCanceled(future, order, sellOrder, event);
                        } else {
                            // Found order's remaining is less than requested cancel order's quantity.
                            found = false;
                        }
                    }
                }
            }

            if (!found) {
                future.complete(FALSE);
                event.end();
                event.commit();
            }
        });

        return future;
    }

    public CompletableFuture<OrderBook> orderBook(final FetchOrderBook fetchOrderBook) {
        final var event = new FetchOrderBookEvent();
        event.begin();

        final var future = new CompletableFuture<OrderBook>();
        executor.execute(() -> {
            final var size = fetchOrderBook.getFetchSize();
            future.complete(new OrderBook(bidsReferences(size), asksReferences(size)));
            event.end();
            event.commit();
        });

        return future;
    }

    private void buyOrderCanceled(final CompletableFuture<Boolean> future, final CancelOrder order,
                                  final Order buyOrder, final Event event) {

        try {
            append(order);
            future.complete(TRUE);
            event.end();
            event.commit();

            logger.trace("cancel: buy: {}", order);
        } catch (RuntimeException ex) {
            // Re-offer the buy order at previous index.
            offer((BuyLimitOrder) buyOrder);
            future.completeExceptionally(ex);
        }
    }

    private void buyOrderPartiallyCanceled(final CompletableFuture<Boolean> future, final CancelOrder order,
                                           final Order buyOrder, final Event event) {

        try {
            append(order);
            future.complete(TRUE);
            event.end();
            event.commit();

            logger.trace("partially: cancel: buy: {}", order);
        } catch (RuntimeException ex) {
            // Re-add the subtracted quantity.
            buyOrder.set_remaining(buyOrder.get_remaining().add(order.get_quantity()));
            future.completeExceptionally(ex);
        }
    }

    private void sellOrderCanceled(final CompletableFuture<Boolean> future, final CancelOrder order,
                                   final Order sellOrder, final Event event) {

        try {
            append(order);
            future.complete(TRUE);
            event.end();
            event.commit();

            logger.trace("cancel: sell: {}", order);
        } catch (RuntimeException ex) {
            // Re-offer the sell order at previous index.
            offer((SellLimitOrder) sellOrder);
            future.completeExceptionally(ex);
        }
    }

    private void sellOrderPartiallyCanceled(final CompletableFuture<Boolean> future, final CancelOrder order,
                                            final Order sellOrder, final Event event) {

        try {
            append(order);
            future.complete(TRUE);
            event.end();
            event.commit();

            logger.trace("partially: cancel: sell: {}", order);
        } catch (RuntimeException ex) {
            // Re-add the subtracted quantity.
            sellOrder.set_remaining(sellOrder.get_remaining().add(order.get_quantity()));
            future.completeExceptionally(ex);
        }
    }

    private ArrayList<BuyLimitOrder> bidsReferences(final int size) {
        final var bids = new ArrayList<BuyLimitOrder>(size);
        try {
            for (int i = 1; i <= size; i++) {
                if (buyOrders.peek() != null) {
                    bids.add((BuyLimitOrder) buyOrders.poll());
                } else {
                    break;
                }
            }
        } finally {
            for (final var bid : bids) {
                buyOrders.offer(bid);
            }
        }

        return bids;
    }

    private ArrayList<SellLimitOrder> asksReferences(final int size) {
        final var asks = new ArrayList<SellLimitOrder>(size);
        try {
            for (int i = 1; i <= size; i++) {
                if (sellOrders.peek() != null) {
                    asks.add((SellLimitOrder) sellOrders.poll());
                } else {
                    break;
                }
            }
        } finally {
            for (final var ask : asks) {
                sellOrders.offer(ask);
            }
        }

        return asks;
    }

    private void append(final CancelOrder cancelOrder) {
        try (final var arena = ofConfined()) {
            final var binary = new OrderBinaryRepresentation(arena, cancelOrder);
            binary.encodeV1();

            eventsFile.append(binary.segment());
        }
    }

    private ThreadSafeAtomicFile eventsFile(final String symbol) {
        try {
            final var dataDirectoryPath = of(context().config().loadString("matching.engine.data_directory_path"));
            return new ThreadSafeAtomicFile(dataDirectoryPath.resolve(symbol + ".events"), 1000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        logger.info("Closing matching engine ...");

        try {
            executor.shutdown();
            final var timeout = ofSeconds(60);
            if (!executor.awaitTermination(timeout.toSeconds(), SECONDS)) {
                // Safe to ignore runnable list!
                executor.shutdownNow();
            }

            eventsSynchronizerExecutor.shutdown();
            if (!eventsSynchronizerExecutor.awaitTermination(timeout.toSeconds(), SECONDS)) {
                // Safe to ignore runnable list!
                eventsSynchronizerExecutor.shutdownNow();
            }

            eventsFile.close();
        } catch (Exception ex) {
            logger.error("{}", ex.getMessage());
        }
    }

    /**
     * @author Alireza Pourtaghi
     */
    public static final class OrderBook {
        private final ArrayList<BuyLimitOrder> bids;
        private final ArrayList<SellLimitOrder> asks;

        public OrderBook(final ArrayList<BuyLimitOrder> bids, final ArrayList<SellLimitOrder> asks) {
            this.bids = bids;
            this.asks = asks;
        }

        public ArrayList<BuyLimitOrder> getBids() {
            return bids;
        }

        public ArrayList<SellLimitOrder> getAsks() {
            return asks;
        }
    }
}
