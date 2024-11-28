package com.openex.oms.matching;

import com.openex.oms.binary.file.FileHeader;
import com.openex.oms.binary.file.FileHeaderBinaryRepresentation;
import com.openex.oms.binary.order.CancelOrder;
import com.openex.oms.binary.order.Order;
import com.openex.oms.binary.trade.Trade;
import com.openex.oms.storage.AtomicFile;
import com.openex.oms.storage.ThreadSafeAtomicFile;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.util.concurrent.ExecutorService;

import static com.openex.oms.binary.BinaryRepresentable.*;
import static com.openex.oms.context.AppContext.context;
import static com.openex.oms.models.Tables.ORDER_REQUEST;
import static com.openex.oms.models.Tables.TRADE;
import static java.lang.Boolean.TRUE;
import static java.lang.foreign.Arena.ofConfined;
import static java.time.Instant.ofEpochMilli;
import static org.slf4j.LoggerFactory.getLogger;

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
            final var trade = Trade.decode(tradeSegment);
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
            final var trade = Trade.decode(tradeSegment);
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
        context().dataBase().postgresql().transaction(configuration -> {
            final var count = configuration.dsl().insertInto(TRADE)
                    .columns(TRADE.BUY_ORDER_ID, TRADE.SELL_ORDER_ID, TRADE.SYMBOL, TRADE.QUANTITY, TRADE.BUY_PRICE, TRADE.SELL_PRICE, TRADE.TS)
                    .values(trade.getBuyOrderId(), trade.getSellOrderId(), trade.getSymbol(), trade.getQuantity(), trade.getBuyPrice(), trade.getSellPrice(), ofEpochMilli(trade.getTs()))
                    .execute();

            if (count == 1) {
                final var newValue = arena.allocate(LONG.byteSize());
                newValue.set(LONG, 0, nextPositionToImport);
                eventsMetadataFile.write(newValue, fileHeader.representationSize());
            }
        });
    }

    private void cancelOrder(final Order order, final Arena arena, final long nextPositionToImport) {
        context().dataBase().postgresql().transaction(configuration -> {
            final var count = configuration.dsl().update(ORDER_REQUEST)
                    .set(ORDER_REQUEST.CANCELED, TRUE)
                    .where(ORDER_REQUEST.ID.eq(order.getId()))
                    .and(ORDER_REQUEST.SYMBOL.eq(order.getSymbol()))
                    .execute();

            // matching.engine.store_orders option may be false.
            if (count == 0 || count == 1) {
                final var newValue = arena.allocate(LONG.byteSize());
                newValue.set(LONG, 0, nextPositionToImport);
                eventsMetadataFile.write(newValue, fileHeader.representationSize());
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
