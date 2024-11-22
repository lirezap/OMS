package com.lirezap.nex.matching;

import com.lirezap.nex.binary.order.BuyOrder;
import com.lirezap.nex.binary.order.SellOrder;
import com.lirezap.nex.binary.trade.Trade;
import com.lirezap.nex.binary.trade.TradeBinaryRepresentation;
import com.lirezap.nex.storage.AtomicFile;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
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
    private final PriorityQueue<BuyOrder> buyOrders;
    private final PriorityQueue<SellOrder> sellOrders;
    private final AtomicFile tradesFile;

    public Matcher(final String symbol, final ExecutorService executor, final PriorityQueue<BuyOrder> buyOrders,
                   final PriorityQueue<SellOrder> sellOrders, final Path dataDirectoryPath) {

        this.executor = executor;
        this.buyOrders = buyOrders;
        this.sellOrders = sellOrders;
        this.tradesFile = tradesFile(symbol, dataDirectoryPath);
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

    private void trade(final BuyOrder buyOrder, final SellOrder sellOrder) {
        logger.trace("match: buy: {} sell: {}", buyOrder, sellOrder);

        switch (buyOrder.get_remaining().compareTo(sellOrder.get_remaining())) {
            case 0 -> handleEquality(buyOrder, sellOrder);
            case 1 -> handleGreaterThan(buyOrder, sellOrder);
            case -1 -> handleLessThan(buyOrder, sellOrder);
        }
    }

    private void handleEquality(final BuyOrder buyOrder, final SellOrder sellOrder) {
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

    private void handleGreaterThan(final BuyOrder buyOrder, final SellOrder sellOrder) {
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
                now.getEpochSecond());

        append(trade);
        buyOrder.set_remaining(remaining);
        sellOrder.set_remaining(ZERO);
        sellOrders.poll();

        logger.trace("poll: sell: {}", sellOrder);
    }

    private void handleLessThan(final BuyOrder buyOrder, final SellOrder sellOrder) {
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
                now.getEpochSecond());

        append(trade);
        buyOrder.set_remaining(ZERO);
        sellOrder.set_remaining(remaining);
        buyOrders.poll();

        logger.trace("poll: buy: {}", buyOrder);
    }

    private AtomicFile tradesFile(final String symbol, final Path dataDirectoryPath) {
        try {
            return new AtomicFile(dataDirectoryPath.resolve(symbol + ".trades"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void append(final Trade trade) {
        try (final var arena = ofConfined()) {
            final var binary = new TradeBinaryRepresentation(arena, trade);
            binary.encodeV1();

            tradesFile.append(binary.segment());
        }
    }
}
