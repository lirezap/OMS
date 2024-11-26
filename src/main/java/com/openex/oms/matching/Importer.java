package com.openex.oms.matching;

import com.openex.oms.storage.AtomicFile;
import com.openex.oms.storage.ThreadSafeAtomicFile;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A trade importer implementation, that tries to import trades to a database table.
 *
 * @author Alireza Pourtaghi
 */
public final class Importer implements Runnable {
    private static final Logger logger = getLogger(Importer.class);

    private final ExecutorService executor;
    private final ThreadSafeAtomicFile tradesFile;
    private final AtomicFile tradesMetadataFile;
    private final int readSizeInBytes;
    private final long position;

    public Importer(final ExecutorService executor, final ThreadSafeAtomicFile tradesFile) {
        this.executor = executor;
        this.tradesFile = tradesFile;
        this.tradesMetadataFile = tradesMetadataFile();
        this.readSizeInBytes = 1024;
        this.position = 0;
    }

    @Override
    public void run() {
        // TODO: Complete implementation.
    }

    private AtomicFile tradesMetadataFile() {
        try {
            return new AtomicFile(tradesFile.source().resolveSibling(tradesFile.source().getFileName() + ".metadata"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
