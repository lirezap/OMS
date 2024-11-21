package com.lirezap.nex.matching;

import com.lirezap.nex.binary.order.BuyOrder;
import com.lirezap.nex.binary.order.SellOrder;
import org.slf4j.Logger;

import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;

import static com.lirezap.nex.context.AppContext.context;
import static ir.jibit.nex.models.Tables.ORDER_REQUEST;
import static ir.jibit.nex.models.Tables.TRADE;
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

    public Matcher(final ExecutorService executor, final PriorityQueue<BuyOrder> buyOrders,
                   final PriorityQueue<SellOrder> sellOrders) {

        this.executor = executor;
        this.buyOrders = buyOrders;
        this.sellOrders = sellOrders;
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

        context().dataBase().postgresql().transaction(block -> {
            block.dsl().update(ORDER_REQUEST)
                    .set(ORDER_REQUEST.REMAINING, ZERO.toPlainString())
                    .where(ORDER_REQUEST.ID.eq(buyOrder.getId()))
                    .and(ORDER_REQUEST.SYMBOL.eq(buyOrder.getSymbol()))
                    .execute();

            block.dsl().update(ORDER_REQUEST)
                    .set(ORDER_REQUEST.REMAINING, ZERO.toPlainString())
                    .where(ORDER_REQUEST.ID.eq(sellOrder.getId()))
                    .and(ORDER_REQUEST.SYMBOL.eq(sellOrder.getSymbol()))
                    .execute();

            block.dsl().insertInto(TRADE)
                    .columns(TRADE.BUY_ORDER_ID, TRADE.SELL_ORDER_ID, TRADE.SYMBOL, TRADE.QUANTITY, TRADE.BUY_PRICE, TRADE.SELL_PRICE, TRADE.TS)
                    .values(buyOrder.getId(), sellOrder.getId(), buyOrder.getSymbol(), buyOrder.getQuantity(), buyOrder.getPrice(), sellOrder.getPrice(), now)
                    .execute();
        });

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

        context().dataBase().postgresql().transaction(block -> {
            block.dsl().update(ORDER_REQUEST)
                    .set(ORDER_REQUEST.REMAINING, remaining.toPlainString())
                    .where(ORDER_REQUEST.ID.eq(buyOrder.getId()))
                    .and(ORDER_REQUEST.SYMBOL.eq(buyOrder.getSymbol()))
                    .execute();

            block.dsl().update(ORDER_REQUEST)
                    .set(ORDER_REQUEST.REMAINING, ZERO.toPlainString())
                    .where(ORDER_REQUEST.ID.eq(sellOrder.getId()))
                    .and(ORDER_REQUEST.SYMBOL.eq(sellOrder.getSymbol()))
                    .execute();

            block.dsl().insertInto(TRADE)
                    .columns(TRADE.BUY_ORDER_ID, TRADE.SELL_ORDER_ID, TRADE.SYMBOL, TRADE.QUANTITY, TRADE.BUY_PRICE, TRADE.SELL_PRICE, TRADE.TS)
                    .values(buyOrder.getId(), sellOrder.getId(), buyOrder.getSymbol(), sellOrder.getQuantity(), buyOrder.getPrice(), sellOrder.getPrice(), now)
                    .execute();
        });

        buyOrder.set_remaining(remaining);
        sellOrder.set_remaining(ZERO);
        sellOrders.poll();

        logger.trace("poll: sell: {}", sellOrder);
    }

    private void handleLessThan(final BuyOrder buyOrder, final SellOrder sellOrder) {
        // Buy order must be polled.
        final var now = now();
        final var remaining = sellOrder.get_remaining().subtract(buyOrder.get_remaining());

        context().dataBase().postgresql().transaction(block -> {
            block.dsl().update(ORDER_REQUEST)
                    .set(ORDER_REQUEST.REMAINING, ZERO.toPlainString())
                    .where(ORDER_REQUEST.ID.eq(buyOrder.getId()))
                    .and(ORDER_REQUEST.SYMBOL.eq(buyOrder.getSymbol()))
                    .execute();

            block.dsl().update(ORDER_REQUEST)
                    .set(ORDER_REQUEST.REMAINING, remaining.toPlainString())
                    .where(ORDER_REQUEST.ID.eq(sellOrder.getId()))
                    .and(ORDER_REQUEST.SYMBOL.eq(sellOrder.getSymbol()))
                    .execute();

            block.dsl().insertInto(TRADE)
                    .columns(TRADE.BUY_ORDER_ID, TRADE.SELL_ORDER_ID, TRADE.SYMBOL, TRADE.QUANTITY, TRADE.BUY_PRICE, TRADE.SELL_PRICE, TRADE.TS)
                    .values(buyOrder.getId(), sellOrder.getId(), buyOrder.getSymbol(), buyOrder.getQuantity(), buyOrder.getPrice(), sellOrder.getPrice(), now)
                    .execute();
        });

        buyOrder.set_remaining(ZERO);
        sellOrder.set_remaining(remaining);
        buyOrders.poll();

        logger.trace("poll: buy: {}", buyOrder);
    }
}
