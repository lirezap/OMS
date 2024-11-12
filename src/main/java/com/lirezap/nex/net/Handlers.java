package com.lirezap.nex.net;

import com.lirezap.nex.binary.order.BuyOrder;
import com.lirezap.nex.binary.order.Order;
import com.lirezap.nex.binary.order.SellOrder;
import ir.jibit.nex.models.enums.OrderRequestType;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;

import static com.lirezap.nex.context.AppContext.context;
import static com.lirezap.nex.net.ErrorMessages.INTERNAL_SERVER_ERROR;
import static com.lirezap.nex.net.ErrorMessages.ORDER_ALREADY_EXISTS;
import static ir.jibit.nex.models.Tables.ORDER_REQUEST;
import static ir.jibit.nex.models.enums.OrderRequestType.BUY;
import static ir.jibit.nex.models.enums.OrderRequestType.SELL;
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
            if (!insertOrder(buyOrder, BUY)) {
                write(connection, INTERNAL_SERVER_ERROR);
                return;
            }
            // TODO: Add into matching engine.
            // Write the same received message.
            write(connection);
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
            if (!insertOrder(sellOrder, SELL)) {
                write(connection, INTERNAL_SERVER_ERROR);
                return;
            }
            // TODO: Add into matching engine.
            // Write the same received message.
            write(connection);
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
        var count = context().dataBase().postgresql().insertInto(ORDER_REQUEST)
                .columns(
                        ORDER_REQUEST.ID,
                        ORDER_REQUEST.SYMBOL,
                        ORDER_REQUEST.TYPE,
                        ORDER_REQUEST.QUANTITY,
                        ORDER_REQUEST.PRICE,
                        ORDER_REQUEST.REMAINING,
                        ORDER_REQUEST.TS)
                .values(
                        order.getId(),
                        order.getSymbol(),
                        type,
                        order.getQuantity(),
                        order.getPrice(),
                        order.getQuantity(),
                        ofEpochMilli(order.getTs()))
                .execute();

        return count == 1;
    }
}
