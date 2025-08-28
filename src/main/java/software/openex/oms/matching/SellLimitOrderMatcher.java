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
import software.openex.oms.binary.order.*;
import software.openex.oms.binary.trade.Trade;
import software.openex.oms.event.matching.MatchEvent;
import software.openex.oms.storage.ThreadSafeAtomicFile;

import java.util.PriorityQueue;

import static java.lang.String.format;
import static java.math.BigDecimal.ZERO;
import static java.time.Instant.now;
import static org.slf4j.LoggerFactory.getLogger;
import static software.openex.oms.matching.Util.append;

/**
 * An IOC limit order matcher as a runnable to be submitted into engine's single threaded executor.
 *
 * @author Alireza Pourtaghi
 */
public final class SellLimitOrderMatcher implements Runnable {
    private static final Logger logger = getLogger(SellLimitOrderMatcher.class);

    private final PriorityQueue<LimitOrder> buyOrders;
    private final ThreadSafeAtomicFile tradesFile;
    private final SellLimitOrder sellLimitOrder;

    public SellLimitOrderMatcher(final PriorityQueue<LimitOrder> buyOrders, final ThreadSafeAtomicFile tradesFile,
                                 final SellLimitOrder sellLimitOrder) {

        this.buyOrders = buyOrders;
        this.tradesFile = tradesFile;
        this.sellLimitOrder = sellLimitOrder;
    }

    @Override
    public void run() {
        if (sellLimitOrder instanceof FOKSellLimitOrder) {
            handleFOK();
        } else if (sellLimitOrder instanceof IOCSellLimitOrder) {
            handleIOC();
        } else {
            // Do nothing special, Just warn!
            logger.warn("must not reach block reached for order: {}", sellLimitOrder);
        }
    }

    private void handleFOK() {
        final var buyOrdersHead = buyOrders.peek();
        if (buyOrdersHead != null &&
                sellLimitOrder.get_remaining().compareTo(buyOrdersHead.get_remaining()) <= 0 &&
                sellLimitOrder.get_price().compareTo(buyOrdersHead.get_price()) <= 0) {

            trade((BuyLimitOrder) buyOrdersHead);
        } else {
            final var cancelOrder = new CancelOrder(
                    sellLimitOrder.getId(),
                    sellLimitOrder.getTs(),
                    sellLimitOrder.getSymbol(),
                    // Set ZERO to cancel all remaining.
                    ZERO.toPlainString());

            append(cancelOrder, tradesFile);
        }
    }

    private void handleIOC() {
        for (; ; ) {
            final var buyOrdersHead = buyOrders.peek();
            if (sellLimitOrder.get_remaining().compareTo(ZERO) > 0 &&
                    buyOrdersHead != null &&
                    sellLimitOrder.get_price().compareTo(buyOrdersHead.get_price()) <= 0) {

                trade((BuyLimitOrder) buyOrdersHead);
            } else {
                break;
            }
        }

        // Because of IOC feature we should cancel remaining quantity in order message.
        if (sellLimitOrder.get_remaining().compareTo(ZERO) > 0) {
            final var cancelOrder = new CancelOrder(
                    sellLimitOrder.getId(),
                    sellLimitOrder.getTs(),
                    sellLimitOrder.getSymbol(),
                    // Set ZERO to cancel all remaining.
                    ZERO.toPlainString());

            append(cancelOrder, tradesFile);
        }
    }

    private void trade(final BuyLimitOrder buyOrder) {
        final var event = new MatchEvent(sellLimitOrder.getSymbol());
        event.begin();

        logger.trace("match: buy: {} sell: {}", buyOrder, sellLimitOrder);
        switch (sellLimitOrder.get_remaining().compareTo(buyOrder.get_remaining())) {
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
                sellLimitOrder.getId(),
                sellLimitOrder.getSymbol(),
                sellLimitOrder.get_remaining().toPlainString(),
                buyOrder.getPrice(),
                sellLimitOrder.getPrice(),
                format("bor:%s;sor:%s", ZERO, ZERO),
                now.toEpochMilli());

        append(trade, tradesFile);
        buyOrder.set_remaining(ZERO);
        sellLimitOrder.set_remaining(ZERO);
        buyOrders.poll();

        logger.trace("poll: buy: {}", buyOrder);
    }

    private void handleGreaterThan(final BuyLimitOrder buyOrder) {
        // Buy order must be polled.
        final var now = now();
        final var remaining = sellLimitOrder.get_remaining().subtract(buyOrder.get_remaining());
        final var trade = new Trade(
                buyOrder.getId(),
                sellLimitOrder.getId(),
                sellLimitOrder.getSymbol(),
                buyOrder.get_remaining().toPlainString(),
                buyOrder.getPrice(),
                sellLimitOrder.getPrice(),
                format("bor:%s;sor:%s", ZERO, remaining.stripTrailingZeros()),
                now.toEpochMilli());

        append(trade, tradesFile);
        buyOrder.set_remaining(ZERO);
        sellLimitOrder.set_remaining(remaining);
        buyOrders.poll();

        logger.trace("poll: buy: {}", buyOrder);
    }

    private void handleLessThan(final BuyLimitOrder buyOrder) {
        final var now = now();
        final var remaining = buyOrder.get_remaining().subtract(sellLimitOrder.get_remaining());
        final var trade = new Trade(
                buyOrder.getId(),
                sellLimitOrder.getId(),
                sellLimitOrder.getSymbol(),
                sellLimitOrder.get_remaining().toPlainString(),
                buyOrder.getPrice(),
                sellLimitOrder.getPrice(),
                format("bor:%s;sor:%s", remaining.stripTrailingZeros(), ZERO),
                now.toEpochMilli());

        append(trade, tradesFile);
        buyOrder.set_remaining(remaining);
        sellLimitOrder.set_remaining(ZERO);
    }
}
