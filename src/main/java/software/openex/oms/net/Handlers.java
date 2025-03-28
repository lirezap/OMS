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
package software.openex.oms.net;

import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import software.openex.oms.binary.BinaryRepresentation;
import software.openex.oms.binary.order.*;
import software.openex.oms.binary.order.book.FetchOrderBookBinaryRepresentation;
import software.openex.oms.binary.order.book.OrderBook;
import software.openex.oms.binary.order.book.OrderBookBinaryRepresentation;

import java.util.ArrayList;

import static java.lang.foreign.Arena.ofShared;
import static org.slf4j.LoggerFactory.getLogger;
import static software.openex.oms.context.AppContext.context;
import static software.openex.oms.models.enums.OrderMessageSide.BUY;
import static software.openex.oms.models.enums.OrderMessageSide.SELL;
import static software.openex.oms.net.ErrorMessages.*;

/**
 * Methods that handle incoming messages, dispatched by dispatcher.
 *
 * @author Alireza Pourtaghi
 */
public final class Handlers implements Responder {
    private static final Logger logger = getLogger(Handlers.class);

    public void handleBuyLimitOrder(final Connection connection) {
        try {
            // TODO: Validate incoming message.
            logMessage(connection);
            final var buyLimitOrder = BuyLimitOrder.decode(connection.segment());
            if (context().config().loadBoolean("matching.engine.store_orders") &&
                    !context().dataBase().insertLimitOrder(buyLimitOrder, BUY)) {

                write(connection, INTERNAL_SERVER_ERROR);
                return;
            }

            context().matchingEngines().offer(buyLimitOrder)
                    .thenAcceptAsync(v -> {
                        // Write the same received message.
                        write(connection);
                    }, context().executors().worker())
                    .exceptionallyAsync(ex -> {
                        logger.error("{}", ex.getMessage());
                        write(connection, INTERNAL_SERVER_ERROR);

                        return null;
                    }, context().executors().worker());
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

    public void handleSellLimitOrder(final Connection connection) {
        try {
            // TODO: Validate incoming message.
            logMessage(connection);
            final var sellLimitOrder = SellLimitOrder.decode(connection.segment());
            if (context().config().loadBoolean("matching.engine.store_orders") &&
                    !context().dataBase().insertLimitOrder(sellLimitOrder, SELL)) {

                write(connection, INTERNAL_SERVER_ERROR);
                return;
            }

            context().matchingEngines().offer(sellLimitOrder)
                    .thenAcceptAsync(v -> {
                        // Write the same received message.
                        write(connection);
                    }, context().executors().worker())
                    .exceptionallyAsync(ex -> {
                        logger.error("{}", ex.getMessage());
                        write(connection, INTERNAL_SERVER_ERROR);

                        return null;
                    }, context().executors().worker());
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
                    .thenAcceptAsync(canceled -> {
                        if (canceled) {
                            // Write the same received message.
                            write(connection);
                        } else {
                            write(connection, ORDER_NOT_FOUND);
                        }
                    }, context().executors().worker())
                    .exceptionallyAsync(ex -> {
                        logger.error("{}", ex.getMessage());
                        write(connection, INTERNAL_SERVER_ERROR);

                        return null;
                    }, context().executors().worker());
        } catch (Exception ex) {
            logger.error("{}", ex.getMessage());
            write(connection, INTERNAL_SERVER_ERROR);
        }
    }

    public void handleFetchOrderBook(final Connection connection) {
        try {
            // TODO: Validate incoming message.
            final var fetchOrderBook = FetchOrderBookBinaryRepresentation.decode(connection.segment());
            context().matchingEngines().orderBook(fetchOrderBook)
                    .thenAcceptAsync(orderBook -> {
                        final var arena = ofShared();
                        final var bids = new ArrayList<BinaryRepresentation<LimitOrder>>();
                        final var asks = new ArrayList<BinaryRepresentation<LimitOrder>>();

                        orderBook.getBids().stream()
                                .map(bid -> new LimitOrderBinaryRepresentation(arena, bid))
                                .peek(BinaryRepresentation::encodeV1)
                                .forEach(bids::add);

                        orderBook.getAsks().stream()
                                .map(ask -> new LimitOrderBinaryRepresentation(arena, ask))
                                .peek(BinaryRepresentation::encodeV1)
                                .forEach(asks::add);

                        final var response = new OrderBookBinaryRepresentation(arena, new OrderBook(bids, asks));
                        response.encodeV1();
                        write(connection, response);
                    }, context().executors().worker())
                    .exceptionallyAsync(ex -> {
                        logger.error("{}", ex.getMessage());
                        write(connection, INTERNAL_SERVER_ERROR);

                        return null;
                    }, context().executors().worker());
        } catch (Exception ex) {
            logger.error("{}", ex.getMessage());
            write(connection, INTERNAL_SERVER_ERROR);
        }
    }

    public void handleBuyMarketOrder(final Connection connection) {
        try {
            // TODO: Validate incoming message.
            logMessage(connection);
            final var buyMarketOrder = BuyMarketOrder.decode(connection.segment());
            if (context().config().loadBoolean("matching.engine.store_orders") &&
                    !context().dataBase().insertMarketOrder(buyMarketOrder, BUY)) {

                write(connection, INTERNAL_SERVER_ERROR);
                return;
            }

            context().matchingEngines().offer(buyMarketOrder);
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

    public void handleSellMarketOrder(final Connection connection) {
        try {
            // TODO: Validate incoming message.
            logMessage(connection);
            final var sellMarketOrder = SellMarketOrder.decode(connection.segment());
            if (context().config().loadBoolean("matching.engine.store_orders") &&
                    !context().dataBase().insertMarketOrder(sellMarketOrder, SELL)) {

                write(connection, INTERNAL_SERVER_ERROR);
                return;
            }

            context().matchingEngines().offer(sellMarketOrder);
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

    public void handleIOCBuyLimitOrder(final Connection connection) {
        try {
            // TODO: Validate incoming message.
            logMessage(connection);
            final var buyLimitOrder = BuyLimitOrder.decode(connection.segment());
            if (context().config().loadBoolean("matching.engine.store_orders") &&
                    !context().dataBase().insertIOCLimitOrder(buyLimitOrder, BUY)) {

                write(connection, INTERNAL_SERVER_ERROR);
                return;
            }

            final var iocBuyLimitOrder = new IOCBuyLimitOrder(
                    buyLimitOrder.getId(),
                    buyLimitOrder.getTs(),
                    buyLimitOrder.getSymbol(),
                    buyLimitOrder.getQuantity(),
                    buyLimitOrder.getPrice());

            context().matchingEngines().offer(iocBuyLimitOrder);
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

    public void handleIOCSellLimitOrder(final Connection connection) {
        try {
            // TODO: Validate incoming message.
            logMessage(connection);
            final var sellLimitOrder = SellLimitOrder.decode(connection.segment());
            if (context().config().loadBoolean("matching.engine.store_orders") &&
                    !context().dataBase().insertIOCLimitOrder(sellLimitOrder, SELL)) {

                write(connection, INTERNAL_SERVER_ERROR);
                return;
            }

            final var iocSellLimitOrder = new IOCSellLimitOrder(
                    sellLimitOrder.getId(),
                    sellLimitOrder.getTs(),
                    sellLimitOrder.getSymbol(),
                    sellLimitOrder.getQuantity(),
                    sellLimitOrder.getPrice());

            context().matchingEngines().offer(iocSellLimitOrder);
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

    public void handleFOKBuyLimitOrder(final Connection connection) {
        try {
            // TODO: Validate incoming message.
            logMessage(connection);
            final var buyLimitOrder = BuyLimitOrder.decode(connection.segment());
            if (context().config().loadBoolean("matching.engine.store_orders") &&
                    !context().dataBase().insertFOKLimitOrder(buyLimitOrder, BUY)) {

                write(connection, INTERNAL_SERVER_ERROR);
                return;
            }

            final var fokBuyLimitOrder = new FOKBuyLimitOrder(
                    buyLimitOrder.getId(),
                    buyLimitOrder.getTs(),
                    buyLimitOrder.getSymbol(),
                    buyLimitOrder.getQuantity(),
                    buyLimitOrder.getPrice());

            context().matchingEngines().offer(fokBuyLimitOrder);
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

    public void handleFOKSellLimitOrder(final Connection connection) {
        try {
            // TODO: Validate incoming message.
            logMessage(connection);
            final var sellLimitOrder = SellLimitOrder.decode(connection.segment());
            if (context().config().loadBoolean("matching.engine.store_orders") &&
                    !context().dataBase().insertFOKLimitOrder(sellLimitOrder, SELL)) {

                write(connection, INTERNAL_SERVER_ERROR);
                return;
            }

            final var fokSellLimitOrder = new FOKSellLimitOrder(
                    sellLimitOrder.getId(),
                    sellLimitOrder.getTs(),
                    sellLimitOrder.getSymbol(),
                    sellLimitOrder.getQuantity(),
                    sellLimitOrder.getPrice());

            context().matchingEngines().offer(fokSellLimitOrder);
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
                file.append(connection.copyMessageForLog().asByteBuffer()), doNothing);
    }
}
