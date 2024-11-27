package com.openex.oms.net;

import com.openex.oms.binary.order.BuyOrder;
import com.openex.oms.binary.order.Order;
import com.openex.oms.binary.order.SellOrder;
import com.openex.oms.models.enums.OrderRequestType;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;

import static com.openex.oms.context.AppContext.context;
import static com.openex.oms.models.Tables.ORDER_REQUEST;
import static com.openex.oms.models.enums.OrderRequestType.BUY;
import static com.openex.oms.models.enums.OrderRequestType.SELL;
import static com.openex.oms.net.ErrorMessages.INTERNAL_SERVER_ERROR;
import static com.openex.oms.net.ErrorMessages.ORDER_ALREADY_EXISTS;
import static java.time.Instant.ofEpochMilli;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Methods that handle incoming messages, dispatched by dispatcher.
 *
 * @author Alireza Pourtaghi
 */
public final class Handlers implements Responder {
    private static final Logger logger = getLogger(Handlers.class);

    public void handleBuyOrder(final Connection connection) {
        try {
            // TODO: Validate incoming message.
            logMessage(connection);
            final var buyOrder = BuyOrder.decode(connection.segment());
            if (context().config().loadBoolean("matching.engine.store_orders") && !insertOrder(buyOrder, BUY)) {
                write(connection, INTERNAL_SERVER_ERROR);
                return;
            }

            context().matchingEngines().offer(buyOrder)
                    .thenAccept(v -> {
                        // Write the same received message.
                        write(connection);
                    }).exceptionally(ex -> {
                        logger.error("{}", ex.getMessage());
                        write(connection, INTERNAL_SERVER_ERROR);

                        return null;
                    });
        } catch (DataAccessException ex) {
            if (ex.getMessage().contains("(id, symbol)") && ex.getMessage().contains("already exists")) {
                write(connection, ORDER_ALREADY_EXISTS);
                return;
            }

            logger.error("{}", ex.getMessage());
            write(connection, INTERNAL_SERVER_ERROR);
        } catch (Exception ex) {
            logger.error("{}", ex.getMessage());
            write(connection, INTERNAL_SERVER_ERROR);
        }
    }

    public void handleSellOrder(final Connection connection) {
        try {
            // TODO: Validate incoming message.
            logMessage(connection);
            final var sellOrder = SellOrder.decode(connection.segment());
            if (context().config().loadBoolean("matching.engine.store_orders") && !insertOrder(sellOrder, SELL)) {
                write(connection, INTERNAL_SERVER_ERROR);
                return;
            }

            context().matchingEngines().offer(sellOrder)
                    .thenAccept(v -> {
                        // Write the same received message.
                        write(connection);
                    }).exceptionally(ex -> {
                        logger.error("{}", ex.getMessage());
                        write(connection, INTERNAL_SERVER_ERROR);

                        return null;
                    });
        } catch (DataAccessException ex) {
            if (ex.getMessage().contains("(id, symbol)") && ex.getMessage().contains("already exists")) {
                write(connection, ORDER_ALREADY_EXISTS);
                return;
            }

            logger.error("{}", ex.getMessage());
            write(connection, INTERNAL_SERVER_ERROR);
        } catch (Exception ex) {
            logger.error("{}", ex.getMessage());
            write(connection, INTERNAL_SERVER_ERROR);
        }
    }

    private void logMessage(final Connection connection) {
        context().messagesLogFile().ifPresentOrElse(file ->
                file.append(connection.copyMessage().asByteBuffer()), doNothing);
    }

    private boolean insertOrder(final Order order, final OrderRequestType type) {
        final var count = context().dataBase().postgresql().insertInto(ORDER_REQUEST)
                .columns(ORDER_REQUEST.ID, ORDER_REQUEST.SYMBOL, ORDER_REQUEST.TYPE, ORDER_REQUEST.QUANTITY, ORDER_REQUEST.PRICE, ORDER_REQUEST.REMAINING, ORDER_REQUEST.TS)
                .values(order.getId(), order.getSymbol(), type, order.getQuantity(), order.getPrice(), order.getQuantity(), ofEpochMilli(order.getTs()))
                .execute();

        return count == 1;
    }
}
