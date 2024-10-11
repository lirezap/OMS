package com.lirezap.nex.storage;

import com.lirezap.nex.binary.http.HTTPRequest;
import com.lirezap.nex.binary.http.HTTPRequestBinaryRepresentation;
import com.lirezap.nex.storage.handlers.BinaryRepresentationWriteHandler;
import com.lirezap.nex.storage.handlers.FileSizePositionSetterLockHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * High performance, parallel, append-only and thread-safe file writer implementation.
 *
 * @author Alireza Pourtaghi
 */
public final class ParallelAppendOnlyFileWriter implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ParallelAppendOnlyFileWriter.class);

    private final Semaphore guard;
    private final AtomicLong position;
    private final FileSizePositionSetterLockHandler fileSizePositionSetterLockHandler;
    private final BinaryRepresentationWriteHandler binaryRepresentationWriteHandler;
    private final long parallelism;
    private final AtomicLong index;
    private final FileWriter[] writers;

    public ParallelAppendOnlyFileWriter(final Path path, final int parallelism) throws IOException {
        this.guard = new Semaphore(1);
        this.position = new AtomicLong(0);
        this.fileSizePositionSetterLockHandler = new FileSizePositionSetterLockHandler(guard, position);
        this.binaryRepresentationWriteHandler = new BinaryRepresentationWriteHandler();
        this.parallelism = parallelism;
        this.index = new AtomicLong(0);
        this.writers = new FileWriter[parallelism];

        for (int i = 0; i < parallelism; i++) {
            logger.info("Setting up a new writer at index {} for {} ...", i, path);
            writers[i] = new FileWriter(path);
        }

        updateCurrentPosition();
    }

    private void updateCurrentPosition() {
        if (guard.tryAcquire()) {
            // Only one thread can reach this block at a time for a specific file!
            final var file = writers[0].getFile();
            file.lock(file, fileSizePositionSetterLockHandler);
        }
    }

    public void append(final HTTPRequest httpRequest) {
        final var writer = writers[(int) (index.getAndIncrement() % parallelism)];
        writer.getExecutor().submit(() -> {
            final var representation = new HTTPRequestBinaryRepresentation(Arena.ofConfined(), httpRequest);
            representation.encode();

            writer.getFile().write(
                    representation.buffer(),
                    position.getAndAdd(representation.segment().byteSize()),
                    representation,
                    binaryRepresentationWriteHandler);
        });
    }

    @Override
    public void close() throws Exception {
        for (final var writer : writers) {
            writer.getFile().close();
            writer.getExecutor().close();
        }
    }

    /**
     * @author Alireza Pourtaghi
     */
    private static final class FileWriter {
        private final ExecutorService executor;
        private final AsynchronousFileChannel file;

        public FileWriter(final Path path) throws IOException {
            this.executor = Executors.newSingleThreadExecutor();
            this.file = AsynchronousFileChannel.open(path, Set.of(CREATE, WRITE), executor);
        }

        public ExecutorService getExecutor() {
            return executor;
        }

        public AsynchronousFileChannel getFile() {
            return file;
        }
    }
}
