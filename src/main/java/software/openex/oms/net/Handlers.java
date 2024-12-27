/*
 * ISC License
 *
 * Copyright (c) 2024, Alireza Pourtaghi <lirezap@protonmail.com>
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
package software.openex.oms.net;

import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import software.openex.oms.binary.order.BuyOrder;
import software.openex.oms.binary.order.CancelOrder;
import software.openex.oms.binary.order.Order;
import software.openex.oms.binary.order.SellOrder;
import software.openex.oms.models.enums.OrderRequestType;

import static java.time.Instant.ofEpochMilli;
import static org.slf4j.LoggerFactory.getLogger;
import static software.openex.oms.context.AppContext.context;
import static software.openex.oms.models.Tables.ORDER_REQUEST;
import static software.openex.oms.models.enums.OrderRequestType.BUY;
import static software.openex.oms.models.enums.OrderRequestType.SELL;
import static software.openex.oms.net.ErrorMessages.*;

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

    public void handleCancelOrder(final Connection connection) {
        try {
            // TODO: Validate incoming message.
            logMessage(connection);
            final var cancelOrder = CancelOrder.decode(connection.segment());
            context().matchingEngines().cancel(cancelOrder)
                    .thenAccept(canceled -> {
                        if (canceled) {
                            write(connection);
                        } else {
                            write(connection, ORDER_NOT_FOUND);
                        }
                    }).exceptionally(ex -> {
                        logger.error("{}", ex.getMessage());
                        write(connection, INTERNAL_SERVER_ERROR);

                        return null;
                    });
        } catch (Exception ex) {
            logger.error("{}", ex.getMessage());
            write(connection, INTERNAL_SERVER_ERROR);
        }
    }

    public void handleFetchOrderBook(final Connection connection) {
        try {
            // TODO: Complete implementation.
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
