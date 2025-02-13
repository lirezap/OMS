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
import software.openex.oms.models.enums.OrderMessageSide;

import java.util.ArrayList;

import static java.lang.foreign.Arena.ofShared;
import static java.time.Instant.ofEpochMilli;
import static org.slf4j.LoggerFactory.getLogger;
import static software.openex.oms.context.AppContext.context;
import static software.openex.oms.models.enums.OrderMessageSide.BUY;
import static software.openex.oms.models.enums.OrderMessageSide.SELL;
import static software.openex.oms.models.enums.OrderMessageType.LIMIT;
import static software.openex.oms.models.enums.OrderMessageType.MARKET;
import static software.openex.oms.models.tables.OrderMessage.ORDER_MESSAGE;
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
            if (context().config().loadBoolean("matching.engine.store_orders") && !insert(buyLimitOrder, BUY)) {
                write(connection, INTERNAL_SERVER_ERROR);
                return;
            }

            context().matchingEngines().offer(buyLimitOrder)
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

    public void handleSellLimitOrder(final Connection connection) {
        try {
            // TODO: Validate incoming message.
            logMessage(connection);
            final var sellLimitOrder = SellLimitOrder.decode(connection.segment());
            if (context().config().loadBoolean("matching.engine.store_orders") && !insert(sellLimitOrder, SELL)) {
                write(connection, INTERNAL_SERVER_ERROR);
                return;
            }

            context().matchingEngines().offer(sellLimitOrder)
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
                            // Write the same received message.
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
            // TODO: Validate incoming message.
            final var fetchOrderBook = FetchOrderBookBinaryRepresentation.decode(connection.segment());
            context().matchingEngines().orderBook(fetchOrderBook)
                    .thenAccept(orderBook -> {
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
                    })
                    .exceptionally(ex -> {
                        logger.error("{}", ex.getMessage());
                        write(connection, INTERNAL_SERVER_ERROR);

                        return null;
                    });
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
            if (context().config().loadBoolean("matching.engine.store_orders") && !insert(buyMarketOrder, BUY)) {
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
            if (context().config().loadBoolean("matching.engine.store_orders") && !insert(sellMarketOrder, SELL)) {
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

    private void logMessage(final Connection connection) {
        context().messagesLogFile().ifPresentOrElse(file ->
                file.append(connection.copyMessageForLog().asByteBuffer()), doNothing);
    }

    private boolean insert(final LimitOrder order, final OrderMessageSide side) {
        final var count = context().dataBase().postgresql().insertInto(ORDER_MESSAGE)
                .columns(ORDER_MESSAGE.ID, ORDER_MESSAGE.SYMBOL, ORDER_MESSAGE.SIDE, ORDER_MESSAGE.TYPE, ORDER_MESSAGE.QUANTITY, ORDER_MESSAGE.PRICE, ORDER_MESSAGE.REMAINING, ORDER_MESSAGE.TS)
                .values(order.getId(), order.getSymbol(), side, LIMIT, order.getQuantity(), order.getPrice(), order.getQuantity(), ofEpochMilli(order.getTs()))
                .execute();

        return count == 1;
    }

    private boolean insert(final MarketOrder order, final OrderMessageSide side) {
        final var count = context().dataBase().postgresql().insertInto(ORDER_MESSAGE)
                .columns(ORDER_MESSAGE.ID, ORDER_MESSAGE.SYMBOL, ORDER_MESSAGE.SIDE, ORDER_MESSAGE.TYPE, ORDER_MESSAGE.QUANTITY, ORDER_MESSAGE.REMAINING, ORDER_MESSAGE.TS)
                .values(order.getId(), order.getSymbol(), side, MARKET, order.getQuantity(), order.getQuantity(), ofEpochMilli(order.getTs()))
                .execute();

        return count == 1;
    }
}
