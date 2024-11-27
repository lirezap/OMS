package com.openex.oms.matching;

import com.openex.oms.binary.file.FileHeader;
import com.openex.oms.binary.file.FileHeaderBinaryRepresentation;
import com.openex.oms.binary.trade.Trade;
import com.openex.oms.storage.AtomicFile;
import com.openex.oms.storage.ThreadSafeAtomicFile;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.util.concurrent.ExecutorService;

import static com.openex.oms.binary.BinaryRepresentable.*;
import static com.openex.oms.binary.trade.Trade.decode;
import static com.openex.oms.context.AppContext.context;
import static com.openex.oms.models.Tables.TRADE;
import static java.lang.foreign.Arena.ofConfined;
import static java.time.Instant.ofEpochMilli;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A trade importer implementation, that tries to import trades to a database table.
 *
 * @author Alireza Pourtaghi
 */
public final class Importer implements Runnable {
    private static final Logger logger = getLogger(Importer.class);
    private static final FileHeaderBinaryRepresentation fileHeaderBinaryRepresentation =
            new FileHeaderBinaryRepresentation(new FileHeader(0));

    private final ExecutorService executor;
    private final ThreadSafeAtomicFile tradesFile;
    private final AtomicFile tradesMetadataFile;

    public Importer(final ExecutorService executor, final ThreadSafeAtomicFile tradesFile) {
        this.executor = executor;
        this.tradesFile = tradesFile;
        this.tradesMetadataFile = tradesMetadataFile();
    }

    @Override
    public void run() {
        try {
            try (final var arena = ofConfined()) {
                final var fileSegment = tradesMetadataFile.read(arena, fileHeaderBinaryRepresentation.representationSize(), LONG.byteSize());
                final var nextPositionToImport = fileSegment.get(LONG, 0);
                if (nextPositionToImport > 0) {
                    importTradeAtPosition(arena, nextPositionToImport);
                } else {
                    // First trade to import.
                    importFirstTrade(arena);
                }
            }
        } catch (Exception ex) {
            logger.error("{}", ex.getMessage());
        } finally {
            executor.submit(this);
        }
    }

    private void importTradeAtPosition(final Arena arena, final long position) throws IOException {
        final var fileSegment = tradesFile.read(arena, position, RHS);
        final var recordId = id(fileSegment);
        final var recordSize = size(fileSegment);

        if (recordId == 103 && recordSize > 0) {
            final var tradeSegment = tradesFile.read(arena, position, RHS + recordSize);
            final var trade = decode(tradeSegment);
            insertTrade(trade, arena, position + RHS + recordSize);
        }
    }

    private void importFirstTrade(final Arena arena) throws IOException {
        final var fileSegment = tradesFile.read(arena, fileHeaderBinaryRepresentation.representationSize(), RHS);
        final var recordId = id(fileSegment);
        final var recordSize = size(fileSegment);

        if (recordId == 103 && recordSize > 0) {
            final var tradeSegment = tradesFile.read(arena, fileHeaderBinaryRepresentation.representationSize(), RHS + recordSize);
            final var trade = decode(tradeSegment);
            insertTrade(trade, arena, fileHeaderBinaryRepresentation.representationSize() + RHS + recordSize);
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
                tradesMetadataFile.write(newValue, fileHeaderBinaryRepresentation.representationSize());
            }
        });
    }

    private AtomicFile tradesMetadataFile() {
        try {
            return new AtomicFile(tradesFile.source().resolveSibling(tradesFile.source().getFileName() + ".metadata"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
