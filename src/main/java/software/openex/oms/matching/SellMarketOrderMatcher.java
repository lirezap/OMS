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
import software.openex.oms.binary.order.BuyLimitOrder;
import software.openex.oms.binary.order.LimitOrder;
import software.openex.oms.binary.order.SellMarketOrder;
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
public final class SellMarketOrderMatcher implements Runnable {
    private static final Logger logger = getLogger(SellMarketOrderMatcher.class);

    private final PriorityQueue<LimitOrder> buyOrders;
    private final ThreadSafeAtomicFile tradesFile;
    private final SellMarketOrder sellMarketOrder;

    public SellMarketOrderMatcher(final PriorityQueue<LimitOrder> buyOrders, final ThreadSafeAtomicFile tradesFile,
                                  final SellMarketOrder sellMarketOrder) {

        this.buyOrders = buyOrders;
        this.tradesFile = tradesFile;
        this.sellMarketOrder = sellMarketOrder;
    }

    @Override
    public void run() {
        for (; ; ) {
            final var buyOrdersHead = buyOrders.peek();
            if (sellMarketOrder.get_remaining().compareTo(ZERO) > 0 && buyOrdersHead != null) {
                trade((BuyLimitOrder) buyOrdersHead);
            } else {
                break;
            }
        }
    }

    private void trade(final BuyLimitOrder buyOrder) {
        final var event = new MatchEvent(sellMarketOrder.getSymbol());
        event.begin();

        logger.trace("match: buy: {} sell: {}", buyOrder, sellMarketOrder);
        switch (sellMarketOrder.get_remaining().compareTo(buyOrder.get_remaining())) {
            case 0 -> handleEquality(buyOrder);
            case 1 -> handleGreaterThan(buyOrder);
            case -1 -> handleLessThan(buyOrder);
        }

        event.end();
        event.commit();
    }

    private void handleEquality(final BuyLimitOrder buyOrder) {
        // Buy order must be polled.
        final var now = now();
        final var trade = new Trade(
                buyOrder.getId(),
                sellMarketOrder.getId(),
                sellMarketOrder.getSymbol(),
                sellMarketOrder.get_remaining().toPlainString(),
                buyOrder.getPrice(),
                ZERO.toPlainString(),
                format("bor:%s;sor:%s", ZERO, ZERO),
                now.toEpochMilli());

        append(trade);
        buyOrder.set_remaining(ZERO);
        sellMarketOrder.set_remaining(ZERO);
        buyOrders.poll();

        logger.trace("poll: buy: {}", buyOrder);
    }

    private void handleGreaterThan(final BuyLimitOrder buyOrder) {
        // Buy order must be polled.
        final var now = now();
        final var remaining = sellMarketOrder.get_remaining().subtract(buyOrder.get_remaining());
        final var trade = new Trade(
                buyOrder.getId(),
                sellMarketOrder.getId(),
                sellMarketOrder.getSymbol(),
                buyOrder.get_remaining().toPlainString(),
                buyOrder.getPrice(),
                ZERO.toPlainString(),
                format("bor:%s;sor:%s", ZERO, remaining),
                now.toEpochMilli());

        append(trade);
        buyOrder.set_remaining(ZERO);
        sellMarketOrder.set_remaining(remaining);
        buyOrders.poll();

        logger.trace("poll: buy: {}", buyOrder);
    }

    private void handleLessThan(final BuyLimitOrder buyOrder) {
        final var now = now();
        final var remaining = buyOrder.get_remaining().subtract(sellMarketOrder.get_remaining());
        final var trade = new Trade(
                buyOrder.getId(),
                sellMarketOrder.getId(),
                sellMarketOrder.getSymbol(),
                sellMarketOrder.get_remaining().toPlainString(),
                buyOrder.getPrice(),
                ZERO.toPlainString(),
                format("bor:%s;sor:%s", remaining, ZERO),
                now.toEpochMilli());

        append(trade);
        buyOrder.set_remaining(remaining);
        sellMarketOrder.set_remaining(ZERO);
    }

    private void append(final Trade trade) {
        try (final var arena = ofConfined()) {
            final var binary = new TradeBinaryRepresentation(arena, trade);
            binary.encodeV1();

            tradesFile.append(binary.segment());
        }
    }
}
