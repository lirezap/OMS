/*
 * ISC License
 *
 * Copyright (c) 2024, Alireza Pourtaghi <lirezap@protonmail.com>
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
package software.openex.oms.context;

import org.slf4j.Logger;
import software.openex.oms.binary.order.BuyOrder;
import software.openex.oms.binary.order.CancelOrder;
import software.openex.oms.binary.order.SellOrder;
import software.openex.oms.binary.order.book.FetchOrderBook;
import software.openex.oms.matching.Engine;
import software.openex.oms.matching.Engine.OrderBook;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A component that holds all matching engines.
 *
 * @author Alireza Pourtaghi
 */
public final class MatchingEngines implements Closeable {
    private static final Logger logger = getLogger(MatchingEngines.class);

    private final ConcurrentHashMap<String, Engine> engines;
    private final int initialCap;

    public MatchingEngines(final Configuration configuration) {
        this.engines = new ConcurrentHashMap<>();
        this.initialCap = configuration.loadInt("matching.engine.queues_initial_cap");
    }

    public CompletableFuture<Void> offer(final BuyOrder order) {
        return engines.computeIfAbsent(order.getSymbol(), symbol -> new Engine(symbol, initialCap))
                .offer(order);
    }

    public CompletableFuture<Void> offer(final SellOrder order) {
        return engines.computeIfAbsent(order.getSymbol(), symbol -> new Engine(symbol, initialCap))
                .offer(order);
    }

    public CompletableFuture<Boolean> cancel(final CancelOrder order) {
        return engines.computeIfAbsent(order.getSymbol(), symbol -> new Engine(symbol, initialCap))
                .cancel(order);
    }

    public CompletableFuture<OrderBook> orderBook(final FetchOrderBook fetchOrderBook) {
        return engines.computeIfAbsent(fetchOrderBook.getSymbol(), symbol -> new Engine(symbol, initialCap))
                .orderBook(fetchOrderBook);
    }

    @Override
    public void close() throws IOException {
        logger.info("Closing matching engines ...");

        engines.forEach((symbol, engine) -> {
            try {
                engine.close();
            } catch (Exception ex) {
                logger.error("could not close matching engine of symbol: {}", symbol);
            }
        });
    }
}
