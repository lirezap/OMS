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
package software.openex.oms.context;

import org.jooq.DSLContext;
import org.jooq.Record8;
import org.jooq.Record9;
import org.jooq.Result;
import org.jooq.conf.Settings;
import org.slf4j.Logger;
import software.openex.oms.binary.order.LimitOrder;
import software.openex.oms.binary.order.MarketOrder;
import software.openex.oms.binary.order.Order;
import software.openex.oms.binary.trade.Trade;
import software.openex.oms.models.enums.OrderMessageSide;
import software.openex.oms.models.enums.OrderMessageState;
import software.openex.oms.models.enums.OrderMessageType;

import java.time.Instant;

import static java.time.Instant.ofEpochMilli;
import static org.jooq.SQLDialect.POSTGRES;
import static org.jooq.impl.DSL.using;
import static org.slf4j.LoggerFactory.getLogger;
import static software.openex.oms.models.Tables.TRADE;
import static software.openex.oms.models.enums.OrderMessageState.*;
import static software.openex.oms.models.enums.OrderMessageType.LIMIT;
import static software.openex.oms.models.enums.OrderMessageType.MARKET;
import static software.openex.oms.models.tables.OrderMessage.ORDER_MESSAGE;

/**
 * Database access class.
 *
 * @author Alireza Pourtaghi
 */
public final class DataBase {
    private static final Logger logger = getLogger(DataBase.class);

    private final DSLContext dslContext;

    DataBase(final Configuration configuration, final DataSource dataSource) {
        final var settings = new Settings();
        settings.setQueryTimeout((int) configuration.loadDuration("db.postgresql.query_timeout").toSeconds());

        this.dslContext = using(dataSource.postgresql(), POSTGRES, settings);
    }

    public DSLContext postgresql() {
        return dslContext;
    }

    public boolean insertLimitOrder(final LimitOrder order, final OrderMessageSide side) {
        final var count = postgresql()
                .insertInto(ORDER_MESSAGE)
                .columns(ORDER_MESSAGE.ID,
                        ORDER_MESSAGE.SYMBOL,
                        ORDER_MESSAGE.SIDE,
                        ORDER_MESSAGE.TYPE,
                        ORDER_MESSAGE.QUANTITY,
                        ORDER_MESSAGE.PRICE,
                        ORDER_MESSAGE.REMAINING,
                        ORDER_MESSAGE.TS)
                .values(order.getId(),
                        order.getSymbol(),
                        side,
                        LIMIT,
                        order.getQuantity(),
                        order.getPrice(),
                        order.getQuantity(),
                        ofEpochMilli(order.getTs()))
                .execute();

        return count == 1;
    }

    public boolean insertMarketOrder(final MarketOrder order, final OrderMessageSide side) {
        final var count = postgresql()
                .insertInto(ORDER_MESSAGE)
                .columns(ORDER_MESSAGE.ID,
                        ORDER_MESSAGE.SYMBOL,
                        ORDER_MESSAGE.SIDE,
                        ORDER_MESSAGE.TYPE,
                        ORDER_MESSAGE.QUANTITY,
                        ORDER_MESSAGE.REMAINING,
                        ORDER_MESSAGE.TS)
                .values(order.getId(),
                        order.getSymbol(),
                        side,
                        MARKET,
                        order.getQuantity(),
                        order.getQuantity(),
                        ofEpochMilli(order.getTs()))
                .execute();

        return count == 1;
    }

    public Record9<Long, String, OrderMessageSide, OrderMessageType, String, String, String, OrderMessageState, Instant>
    fetchOrderMessage(final long id, final String symbol) {

        return postgresql()
                .select(ORDER_MESSAGE.ID,
                        ORDER_MESSAGE.SYMBOL,
                        ORDER_MESSAGE.SIDE,
                        ORDER_MESSAGE.TYPE,
                        ORDER_MESSAGE.QUANTITY,
                        ORDER_MESSAGE.PRICE,
                        ORDER_MESSAGE.REMAINING,
                        ORDER_MESSAGE.STATE,
                        ORDER_MESSAGE.TS)
                .from(ORDER_MESSAGE)
                .where(ORDER_MESSAGE.ID.eq(id))
                .and(ORDER_MESSAGE.SYMBOL.eq(symbol))
                .fetchOne();
    }

    public int insertTrade(final org.jooq.Configuration configuration, final Trade trade) {
        return configuration.dsl()
                .insertInto(TRADE)
                .columns(TRADE.BUY_ORDER_ID,
                        TRADE.SELL_ORDER_ID,
                        TRADE.SYMBOL,
                        TRADE.QUANTITY,
                        TRADE.BUY_PRICE,
                        TRADE.SELL_PRICE,
                        TRADE.METADATA,
                        TRADE.TS)
                .values(trade.getBuyOrderId(),
                        trade.getSellOrderId(),
                        trade.getSymbol(),
                        trade.getQuantity(),
                        trade.getBuyPrice(),
                        trade.getSellPrice(),
                        trade.getMetadata(),
                        ofEpochMilli(trade.getTs()))
                .execute();
    }

    public int cancelOrder(final org.jooq.Configuration configuration, final Order order) {
        return configuration.dsl()
                .update(ORDER_MESSAGE)
                .set(ORDER_MESSAGE.STATE, CANCELED)
                .where(ORDER_MESSAGE.ID.eq(order.getId()))
                .and(ORDER_MESSAGE.SYMBOL.eq(order.getSymbol()))
                .execute();
    }

    public int executeOrder(final org.jooq.Configuration configuration, final long orderId, final String symbol,
                            final String remaining) {

        return configuration.dsl()
                .update(ORDER_MESSAGE)
                .set(ORDER_MESSAGE.STATE, EXECUTED)
                .set(ORDER_MESSAGE.REMAINING, remaining)
                .where(ORDER_MESSAGE.ID.eq(orderId))
                .and(ORDER_MESSAGE.SYMBOL.eq(symbol))
                .execute();
    }

    public int updateRemaining(final org.jooq.Configuration configuration, final long orderId, final String symbol,
                               final String remaining) {

        return configuration.dsl()
                .update(ORDER_MESSAGE)
                .set(ORDER_MESSAGE.REMAINING, remaining)
                .where(ORDER_MESSAGE.ID.eq(orderId))
                .and(ORDER_MESSAGE.SYMBOL.eq(symbol))
                .execute();
    }

    public Result<Record8<Long, String, OrderMessageSide, OrderMessageType, String, String, String, Instant>>
    fetchActiveOrderMessages(final Instant from, final int limit) {

        return postgresql()
                .select(ORDER_MESSAGE.ID,
                        ORDER_MESSAGE.SYMBOL,
                        ORDER_MESSAGE.SIDE,
                        ORDER_MESSAGE.TYPE,
                        ORDER_MESSAGE.QUANTITY,
                        ORDER_MESSAGE.PRICE,
                        ORDER_MESSAGE.REMAINING,
                        ORDER_MESSAGE.TS)
                .from(ORDER_MESSAGE)
                .where(ORDER_MESSAGE.STATE.eq(ACTIVE))
                .and(ORDER_MESSAGE.TS.greaterOrEqual(from))
                .orderBy(ORDER_MESSAGE.TS)
                .limit(limit)
                .fetch();
    }
}
