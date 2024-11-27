package com.openex.oms.matching;

import com.openex.oms.binary.order.Order;
import com.openex.oms.binary.trade.Trade;
import com.openex.oms.binary.trade.TradeBinaryRepresentation;
import com.openex.oms.storage.ThreadSafeAtomicFile;
import org.slf4j.Logger;

import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;

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
    private final PriorityQueue<Order> buyOrders;
    private final PriorityQueue<Order> sellOrders;
    private final ThreadSafeAtomicFile tradesFile;

    public Matcher(final ExecutorService executor, final PriorityQueue<Order> buyOrders,
                   final PriorityQueue<Order> sellOrders, final ThreadSafeAtomicFile tradesFile) {

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
            executor.submit(this);
        }
    }

    private void trade(final Order buyOrder, final Order sellOrder) {
        logger.trace("match: buy: {} sell: {}", buyOrder, sellOrder);

        switch (buyOrder.get_remaining().compareTo(sellOrder.get_remaining())) {
            case 0 -> handleEquality(buyOrder, sellOrder);
            case 1 -> handleGreaterThan(buyOrder, sellOrder);
            case -1 -> handleLessThan(buyOrder, sellOrder);
        }
    }

    private void handleEquality(final Order buyOrder, final Order sellOrder) {
        // Both orders must be polled.
        final var now = now();
        final var trade = new Trade(
                buyOrder.getId(),
                sellOrder.getId(),
                buyOrder.getSymbol(),
                buyOrder.get_remaining().toPlainString(),
                buyOrder.getPrice(),
                sellOrder.getPrice(),
                "",
                now.toEpochMilli());

        append(trade);
        buyOrder.set_remaining(ZERO);
        sellOrder.set_remaining(ZERO);
        buyOrders.poll();
        sellOrders.poll();

        logger.trace("poll: buy: {}", buyOrder);
        logger.trace("poll: sell: {}", sellOrder);
    }

    private void handleGreaterThan(final Order buyOrder, final Order sellOrder) {
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
                "",
                now.toEpochMilli());

        append(trade);
        buyOrder.set_remaining(remaining);
        sellOrder.set_remaining(ZERO);
        sellOrders.poll();

        logger.trace("poll: sell: {}", sellOrder);
    }

    private void handleLessThan(final Order buyOrder, final Order sellOrder) {
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
                "",
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
