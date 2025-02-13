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

import org.slf4j.Logger;
import software.openex.oms.binary.order.LimitOrder;
import software.openex.oms.binary.trade.Trade;
import software.openex.oms.binary.trade.TradeBinaryRepresentation;
import software.openex.oms.matching.event.MatchEvent;
import software.openex.oms.storage.ThreadSafeAtomicFile;

import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;

import static java.lang.String.format;
import static java.lang.foreign.Arena.ofConfined;
import static java.math.BigDecimal.ZERO;
import static java.time.Instant.now;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * An orders matcher as a runnable to be submitted into engine's single threaded executor.
 *
 * @author Alireza Pourtaghi
 */
public final class Matcher implements Runnable {
    private static final Logger logger = getLogger(Matcher.class);

    private final ExecutorService executor;
    private final PriorityQueue<LimitOrder> buyOrders;
    private final PriorityQueue<LimitOrder> sellOrders;
    private final ThreadSafeAtomicFile tradesFile;

    public Matcher(final ExecutorService executor, final PriorityQueue<LimitOrder> buyOrders,
                   final PriorityQueue<LimitOrder> sellOrders, final ThreadSafeAtomicFile tradesFile) {

        this.executor = executor;
        this.buyOrders = buyOrders;
        this.sellOrders = sellOrders;
        this.tradesFile = tradesFile;
    }

    @Override
    public void run() {
        try {
            final var buyOrdersHead = buyOrders.peek();
            if (buyOrdersHead != null) {
                final var sellOrdersHead = sellOrders.peek();
                if (sellOrdersHead != null) {
                    // If the price of the head of buy orders is greater than or equal to that of the head of sell orders.
                    if (buyOrdersHead.get_price().compareTo(sellOrdersHead.get_price()) >= 0) {
                        trade(buyOrdersHead, sellOrdersHead);
                    }
                }
            }
        } finally {
            if (!executor.isShutdown()) executor.execute(this);
        }
    }

    private void trade(final LimitOrder buyOrder, final LimitOrder sellOrder) {
        final var event = new MatchEvent(buyOrder.getSymbol());
        event.begin();

        logger.trace("match: buy: {} sell: {}", buyOrder, sellOrder);
        switch (buyOrder.get_remaining().compareTo(sellOrder.get_remaining())) {
            case 0 -> handleEquality(buyOrder, sellOrder);
            case 1 -> handleGreaterThan(buyOrder, sellOrder);
            case -1 -> handleLessThan(buyOrder, sellOrder);
        }

        event.end();
        event.commit();
    }

    private void handleEquality(final LimitOrder buyOrder, final LimitOrder sellOrder) {
        // Both orders must be polled.
        final var now = now();
        final var trade = new Trade(
                buyOrder.getId(),
                sellOrder.getId(),
                buyOrder.getSymbol(),
                buyOrder.get_remaining().toPlainString(),
                buyOrder.getPrice(),
                sellOrder.getPrice(),
                format("bor:%s;sor:%s", ZERO, ZERO),
                now.toEpochMilli());

        append(trade);
        buyOrder.set_remaining(ZERO);
        sellOrder.set_remaining(ZERO);
        buyOrders.poll();
        sellOrders.poll();

        logger.trace("poll: buy: {}", buyOrder);
        logger.trace("poll: sell: {}", sellOrder);
    }

    private void handleGreaterThan(final LimitOrder buyOrder, final LimitOrder sellOrder) {
        // Sell order must be polled.
        final var now = now();
        final var remaining = buyOrder.get_remaining().subtract(sellOrder.get_remaining());
        final var trade = new Trade(
                buyOrder.getId(),
                sellOrder.getId(),
                buyOrder.getSymbol(),
                sellOrder.get_remaining().toPlainString(),
                buyOrder.getPrice(),
                sellOrder.getPrice(),
                format("bor:%s;sor:%s", remaining, ZERO),
                now.toEpochMilli());

        append(trade);
        buyOrder.set_remaining(remaining);
        sellOrder.set_remaining(ZERO);
        sellOrders.poll();

        logger.trace("poll: sell: {}", sellOrder);
    }

    private void handleLessThan(final LimitOrder buyOrder, final LimitOrder sellOrder) {
        // Buy order must be polled.
        final var now = now();
        final var remaining = sellOrder.get_remaining().subtract(buyOrder.get_remaining());
        final var trade = new Trade(
                buyOrder.getId(),
                sellOrder.getId(),
                buyOrder.getSymbol(),
                buyOrder.get_remaining().toPlainString(),
                buyOrder.getPrice(),
                sellOrder.getPrice(),
                format("bor:%s;sor:%s", ZERO, remaining),
                now.toEpochMilli());

        append(trade);
        buyOrder.set_remaining(ZERO);
        sellOrder.set_remaining(remaining);
        buyOrders.poll();

        logger.trace("poll: buy: {}", buyOrder);
    }

    private void append(final Trade trade) {
        try (final var arena = ofConfined()) {
            final var binary = new TradeBinaryRepresentation(arena, trade);
            binary.encodeV1();

            tradesFile.append(binary.segment());
        }
    }
}
