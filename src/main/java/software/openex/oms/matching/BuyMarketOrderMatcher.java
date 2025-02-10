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
import software.openex.oms.binary.order.BuyMarketOrder;
import software.openex.oms.binary.order.LimitOrder;
import software.openex.oms.binary.order.SellLimitOrder;
import software.openex.oms.binary.trade.Trade;
import software.openex.oms.binary.trade.TradeBinaryRepresentation;
import software.openex.oms.matching.event.MatchEvent;
import software.openex.oms.storage.ThreadSafeAtomicFile;

import java.util.PriorityQueue;

import static java.lang.String.format;
import static java.lang.foreign.Arena.ofConfined;
import static java.math.BigDecimal.ZERO;
import static java.time.Instant.now;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A market order matcher as a runnable to be submitted into engine's single threaded executor.
 *
 * @author Alireza Pourtaghi
 */
public final class BuyMarketOrderMatcher implements Runnable {
    private static final Logger logger = getLogger(BuyMarketOrderMatcher.class);

    private final PriorityQueue<LimitOrder> sellOrders;
    private final ThreadSafeAtomicFile tradesFile;
    private final BuyMarketOrder buyMarketOrder;

    public BuyMarketOrderMatcher(final PriorityQueue<LimitOrder> sellOrders, final ThreadSafeAtomicFile tradesFile,
                                 final BuyMarketOrder buyMarketOrder) {

        this.sellOrders = sellOrders;
        this.tradesFile = tradesFile;
        this.buyMarketOrder = buyMarketOrder;
    }

    @Override
    public void run() {
        for (; ; ) {
            final var sellOrdersHead = sellOrders.peek();
            if (buyMarketOrder.get_remaining().compareTo(ZERO) > 0 && sellOrdersHead != null) {
                trade((SellLimitOrder) sellOrdersHead);
            } else {
                break;
            }
        }
    }

    private void trade(final SellLimitOrder sellOrder) {
        final var event = new MatchEvent(buyMarketOrder.getSymbol());
        event.begin();

        logger.trace("match: buy: {} sell: {}", buyMarketOrder, sellOrder);
        switch (buyMarketOrder.get_remaining().compareTo(sellOrder.get_remaining())) {
            case 0 -> handleEquality(sellOrder);
            case 1 -> handleGreaterThan(sellOrder);
            case -1 -> handleLessThan(sellOrder);
        }

        event.end();
        event.commit();
    }

    private void handleEquality(final SellLimitOrder sellOrder) {
        // Sell order must be polled.
        final var now = now();
        final var trade = new Trade(
                buyMarketOrder.getId(),
                sellOrder.getId(),
                buyMarketOrder.getSymbol(),
                buyMarketOrder.get_remaining().toPlainString(),
                ZERO.toPlainString(),
                sellOrder.getPrice(),
                format("bor:%s;sor:%s", ZERO, ZERO),
                now.toEpochMilli());

        append(trade);
        buyMarketOrder.set_remaining(ZERO);
        sellOrder.set_remaining(ZERO);
        sellOrders.poll();

        logger.trace("poll: sell: {}", sellOrder);
    }

    private void handleGreaterThan(final SellLimitOrder sellOrder) {
        // Sell order must be polled.
        final var now = now();
        final var remaining = buyMarketOrder.get_remaining().subtract(sellOrder.get_remaining());
        final var trade = new Trade(
                buyMarketOrder.getId(),
                sellOrder.getId(),
                buyMarketOrder.getSymbol(),
                sellOrder.get_remaining().toPlainString(),
                ZERO.toPlainString(),
                sellOrder.getPrice(),
                format("bor:%s;sor:%s", remaining, ZERO),
                now.toEpochMilli());

        append(trade);
        buyMarketOrder.set_remaining(remaining);
        sellOrder.set_remaining(ZERO);
        sellOrders.poll();

        logger.trace("poll: sell: {}", sellOrder);
    }

    private void handleLessThan(final SellLimitOrder sellOrder) {
        final var now = now();
        final var remaining = sellOrder.get_remaining().subtract(buyMarketOrder.get_remaining());
        final var trade = new Trade(
                buyMarketOrder.getId(),
                sellOrder.getId(),
                buyMarketOrder.getSymbol(),
                buyMarketOrder.get_remaining().toPlainString(),
                ZERO.toPlainString(),
                sellOrder.getPrice(),
                format("bor:%s;sor:%s", ZERO, remaining),
                now.toEpochMilli());

        append(trade);
        buyMarketOrder.set_remaining(ZERO);
        sellOrder.set_remaining(remaining);
    }

    private void append(final Trade trade) {
        try (final var arena = ofConfined()) {
            final var binary = new TradeBinaryRepresentation(arena, trade);
            binary.encodeV1();

            tradesFile.append(binary.segment());
        }
    }
}
