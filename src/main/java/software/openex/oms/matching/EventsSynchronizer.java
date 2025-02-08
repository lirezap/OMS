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
import software.openex.oms.binary.file.FileHeader;
import software.openex.oms.binary.file.FileHeaderBinaryRepresentation;
import software.openex.oms.binary.order.CancelOrder;
import software.openex.oms.binary.order.Order;
import software.openex.oms.binary.trade.Trade;
import software.openex.oms.binary.trade.TradeBinaryRepresentation;
import software.openex.oms.matching.event.SyncCanceledOrderEvent;
import software.openex.oms.matching.event.SyncTradeEvent;
import software.openex.oms.storage.AtomicFile;
import software.openex.oms.storage.ThreadSafeAtomicFile;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.util.concurrent.ExecutorService;

import static java.lang.Boolean.TRUE;
import static java.lang.foreign.Arena.ofConfined;
import static java.time.Instant.ofEpochMilli;
import static org.slf4j.LoggerFactory.getLogger;
import static software.openex.oms.binary.BinaryRepresentable.*;
import static software.openex.oms.context.AppContext.context;
import static software.openex.oms.models.Tables.ORDER_MESSAGE;
import static software.openex.oms.models.Tables.TRADE;

/**
 * Events synchronizer implementation that must keep trades, remaining quantity of orders and canceled orders in sync
 * with database tables.
 *
 * @author Alireza Pourtaghi
 */
public final class EventsSynchronizer implements Runnable {
    private static final Logger logger = getLogger(EventsSynchronizer.class);
    private static final FileHeaderBinaryRepresentation fileHeader = new FileHeaderBinaryRepresentation(new FileHeader(0));

    private final ExecutorService executor;
    private final ThreadSafeAtomicFile eventsFile;
    private final AtomicFile eventsMetadataFile;

    public EventsSynchronizer(final ExecutorService executor, final ThreadSafeAtomicFile eventsFile) {
        this.executor = executor;
        this.eventsFile = eventsFile;
        this.eventsMetadataFile = eventsMetadataFile();
    }

    @Override
    public void run() {
        try {
            try (final var arena = ofConfined()) {
                final var fileSegment = eventsMetadataFile.read(arena, fileHeader.representationSize(), LONG.byteSize());
                final var nextPositionToSync = fileSegment.get(LONG, 0);
                if (nextPositionToSync > 0) {
                    syncAtPosition(arena, nextPositionToSync);
                } else {
                    // First time sync.
                    syncFirst(arena);
                }
            }
        } catch (Exception ex) {
            logger.error("{}", ex.getMessage());
        } finally {
            executor.submit(this);
        }
    }

    private void syncAtPosition(final Arena arena, final long position) throws IOException {
        final var fileSegment = eventsFile.read(arena, position, RHS);
        final var recordId = id(fileSegment);
        final var recordSize = size(fileSegment);

        if (recordId == 103 && recordSize > 0) {
            final var tradeSegment = eventsFile.read(arena, position, RHS + recordSize);
            final var trade = TradeBinaryRepresentation.decode(tradeSegment);
            insertTrade(trade, arena, position + RHS + recordSize);

            return;
        }

        if (recordId == 104 && recordSize > 0) {
            final var cancelOrderSegment = eventsFile.read(arena, position, RHS + recordSize);
            final var cancelOrder = CancelOrder.decode(cancelOrderSegment);
            cancelOrder(cancelOrder, arena, position + RHS + recordSize);
        }
    }

    private void syncFirst(final Arena arena) throws IOException {
        final var fileSegment = eventsFile.read(arena, fileHeader.representationSize(), RHS);
        final var recordId = id(fileSegment);
        final var recordSize = size(fileSegment);

        if (recordId == 103 && recordSize > 0) {
            final var tradeSegment = eventsFile.read(arena, fileHeader.representationSize(), RHS + recordSize);
            final var trade = TradeBinaryRepresentation.decode(tradeSegment);
            insertTrade(trade, arena, fileHeader.representationSize() + RHS + recordSize);

            return;
        }

        if (recordId == 104 && recordSize > 0) {
            final var cancelOrderSegment = eventsFile.read(arena, fileHeader.representationSize(), RHS + recordSize);
            final var cancelOrder = CancelOrder.decode(cancelOrderSegment);
            cancelOrder(cancelOrder, arena, fileHeader.representationSize() + RHS + recordSize);
        }
    }

    private void insertTrade(final Trade trade, final Arena arena, final long nextPositionToImport) {
        final var event = new SyncTradeEvent();
        event.begin();

        context().dataBase().postgresql().transaction(configuration -> {
            final var count = configuration.dsl().insertInto(TRADE)
                    .columns(TRADE.BUY_ORDER_ID, TRADE.SELL_ORDER_ID, TRADE.SYMBOL, TRADE.QUANTITY, TRADE.BUY_PRICE, TRADE.SELL_PRICE, TRADE.METADATA, TRADE.TS)
                    .values(trade.getBuyOrderId(), trade.getSellOrderId(), trade.getSymbol(), trade.getQuantity(), trade.getBuyPrice(), trade.getSellPrice(), trade.getMetadata(), ofEpochMilli(trade.getTs()))
                    .execute();

            if (count == 1) {
                // matching.engine.store_orders option may be false.
                configuration.dsl().update(ORDER_MESSAGE)
                        .set(ORDER_MESSAGE.REMAINING, trade.getMetadata().split(";")[0].replace("bor:", ""))
                        .where(ORDER_MESSAGE.ID.eq(trade.getBuyOrderId()))
                        .and(ORDER_MESSAGE.SYMBOL.eq(trade.getSymbol()))
                        .execute();

                configuration.dsl().update(ORDER_MESSAGE)
                        .set(ORDER_MESSAGE.REMAINING, trade.getMetadata().split(";")[1].replace("sor:", ""))
                        .where(ORDER_MESSAGE.ID.eq(trade.getSellOrderId()))
                        .and(ORDER_MESSAGE.SYMBOL.eq(trade.getSymbol()))
                        .execute();

                final var newValue = arena.allocate(LONG.byteSize());
                newValue.set(LONG, 0, nextPositionToImport);
                eventsMetadataFile.write(newValue, fileHeader.representationSize());

                event.end();
                event.commit();
            }
        });
    }

    private void cancelOrder(final Order order, final Arena arena, final long nextPositionToImport) {
        final var event = new SyncCanceledOrderEvent();
        event.begin();

        context().dataBase().postgresql().transaction(configuration -> {
            final var count = configuration.dsl().update(ORDER_MESSAGE)
                    .set(ORDER_MESSAGE.CANCELED, TRUE)
                    .where(ORDER_MESSAGE.ID.eq(order.getId()))
                    .and(ORDER_MESSAGE.SYMBOL.eq(order.getSymbol()))
                    .execute();

            // matching.engine.store_orders option may be false.
            if (count == 0 || count == 1) {
                final var newValue = arena.allocate(LONG.byteSize());
                newValue.set(LONG, 0, nextPositionToImport);
                eventsMetadataFile.write(newValue, fileHeader.representationSize());

                event.end();
                event.commit();
            }
        });
    }

    private AtomicFile eventsMetadataFile() {
        try {
            return new AtomicFile(eventsFile.source().resolveSibling(eventsFile.source().getFileName() + ".metadata"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
