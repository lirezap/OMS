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
public final class BuyLimitOrderMatcher implements Runnable {
    private static final Logger logger = getLogger(BuyLimitOrderMatcher.class);

    private final PriorityQueue<LimitOrder> sellOrders;
    private final ThreadSafeAtomicFile tradesFile;
    private final BuyLimitOrder buyLimitOrder;

    public BuyLimitOrderMatcher(final PriorityQueue<LimitOrder> sellOrders, final ThreadSafeAtomicFile tradesFile,
                                final BuyLimitOrder buyLimitOrder) {

        this.sellOrders = sellOrders;
        this.tradesFile = tradesFile;
        this.buyLimitOrder = buyLimitOrder;
    }

    @Override
    public void run() {
        if (buyLimitOrder instanceof FOKBuyLimitOrder) {
            handleFOK();
        } else if (buyLimitOrder instanceof IOCBuyLimitOrder) {
            handleIOC();
        } else {
            // Do nothing special, Just warn!
            logger.warn("must not reach block reached for order: {}", buyLimitOrder);
        }
    }

    private void handleFOK() {
        final var sellOrdersHead = sellOrders.peek();
        if (sellOrdersHead != null &&
                buyLimitOrder.get_remaining().compareTo(sellOrdersHead.get_remaining()) <= 0 &&
                buyLimitOrder.get_price().compareTo(sellOrdersHead.get_price()) >= 0) {

            trade((SellLimitOrder) sellOrdersHead);
        } else {
            final var cancelOrder = new CancelOrder(
                    buyLimitOrder.getId(),
                    buyLimitOrder.getTs(),
                    buyLimitOrder.getSymbol(),
                    // Set ZERO to cancel all remaining.
                    ZERO.toPlainString());

            append(cancelOrder, tradesFile);
        }
    }

    private void handleIOC() {
        for (; ; ) {
            final var sellOrdersHead = sellOrders.peek();
            if (buyLimitOrder.get_remaining().compareTo(ZERO) > 0 &&
                    sellOrdersHead != null &&
                    buyLimitOrder.get_price().compareTo(sellOrdersHead.get_price()) >= 0) {

                trade((SellLimitOrder) sellOrdersHead);
            } else {
                break;
            }
        }

        // Because of IOC feature we should cancel remaining quantity in order message.
        if (buyLimitOrder.get_remaining().compareTo(ZERO) > 0) {
            final var cancelOrder = new CancelOrder(
                    buyLimitOrder.getId(),
                    buyLimitOrder.getTs(),
                    buyLimitOrder.getSymbol(),
                    // Set ZERO to cancel all remaining.
                    ZERO.toPlainString());

            append(cancelOrder, tradesFile);
        }
    }

    private void trade(final SellLimitOrder sellOrder) {
        final var event = new MatchEvent(buyLimitOrder.getSymbol());
        event.begin();

        logger.trace("match: buy: {} sell: {}", buyLimitOrder, sellOrder);
        switch (buyLimitOrder.get_remaining().compareTo(sellOrder.get_remaining())) {
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
                buyLimitOrder.getId(),
                sellOrder.getId(),
                buyLimitOrder.getSymbol(),
                buyLimitOrder.get_remaining().toPlainString(),
                buyLimitOrder.getPrice(),
                sellOrder.getPrice(),
                format("bor:%s;sor:%s", ZERO, ZERO),
                now.toEpochMilli());

        append(trade, tradesFile);
        buyLimitOrder.set_remaining(ZERO);
        sellOrder.set_remaining(ZERO);
        sellOrders.poll();

        logger.trace("poll: sell: {}", sellOrder);
    }

    private void handleGreaterThan(final SellLimitOrder sellOrder) {
        // Sell order must be polled.
        final var now = now();
        final var remaining = buyLimitOrder.get_remaining().subtract(sellOrder.get_remaining());
        final var trade = new Trade(
                buyLimitOrder.getId(),
                sellOrder.getId(),
                buyLimitOrder.getSymbol(),
                sellOrder.get_remaining().toPlainString(),
                buyLimitOrder.getPrice(),
                sellOrder.getPrice(),
                format("bor:%s;sor:%s", remaining.stripTrailingZeros(), ZERO),
                now.toEpochMilli());

        append(trade, tradesFile);
        buyLimitOrder.set_remaining(remaining);
        sellOrder.set_remaining(ZERO);
        sellOrders.poll();

        logger.trace("poll: sell: {}", sellOrder);
    }

    private void handleLessThan(final SellLimitOrder sellOrder) {
        final var now = now();
        final var remaining = sellOrder.get_remaining().subtract(buyLimitOrder.get_remaining());
        final var trade = new Trade(
                buyLimitOrder.getId(),
                sellOrder.getId(),
                buyLimitOrder.getSymbol(),
                buyLimitOrder.get_remaining().toPlainString(),
                buyLimitOrder.getPrice(),
                sellOrder.getPrice(),
                format("bor:%s;sor:%s", ZERO, remaining.stripTrailingZeros()),
                now.toEpochMilli());

        append(trade, tradesFile);
        buyLimitOrder.set_remaining(ZERO);
        sellOrder.set_remaining(remaining);
    }
}
