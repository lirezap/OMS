package com.lirezap.nex.storage;

import com.lirezap.nex.binary.order.Order;
import com.lirezap.nex.binary.order.OrderBinaryRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.nio.ByteBuffer;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNull;

/**
 * High performance, paralleled, append-only and thread-safe file writer implementation.
 *
 * @author Alireza Pourtaghi
 */
public final class AsynchronousAppendOnlyFile implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(AsynchronousAppendOnlyFile.class);

    private final Semaphore guard = new Semaphore(1);
    private final AtomicLong position = new AtomicLong(0);
    private final FileSizePositionSetterLockHandler fileSizePositionSetterLockHandler;
    private final long parallelism;
    private final AtomicLong index;
    private final FileWriter[] writers;

    public AsynchronousAppendOnlyFile(final Path path, final int parallelism, final OpenOption... options)
            throws IOException {

        requireNonNull(path);
        this.fileSizePositionSetterLockHandler = new FileSizePositionSetterLockHandler(guard, position);
        this.parallelism = parallelism;
        this.index = new AtomicLong(0);
        this.writers = new FileWriter[parallelism];

        for (int i = 0; i < parallelism; i++) {
            logger.info("Setting up a new writer at index {} for {} ...", i, path);
            writers[i] = new FileWriter(path, options);
        }

        setPosition();
    }

    public void append(final ByteBuffer buffer) {
        final var writer = writers[(int) (index.getAndIncrement() % parallelism)];

        final var file = writer.getFile();
        final var localPosition = position.getAndAdd(buffer.limit());
        file.write(buffer, localPosition, buffer, new ByteBufferWriteHandler(file, buffer, localPosition));
    }

    public void append(final Order order) {
        final var writer = writers[(int) (index.getAndIncrement() % parallelism)];

        writer.getExecutor().submit(() -> {
            final var representation = new OrderBinaryRepresentation(Arena.ofConfined(), order);
            representation.encodeV1();

            final var file = writer.getFile();
            final var buffer = representation.buffer();
            final var localPosition = position.getAndAdd(representation.segment().byteSize());
            file.write(buffer, localPosition, representation, new BinaryRepresentationWriteHandler(file, buffer, localPosition));
        });
    }

    private void setPosition() {
        if (guard.tryAcquire()) {
            // Only one thread can reach this block at a time for a specific file!
            final var file = writers[0].getFile();
            file.lock(file, fileSizePositionSetterLockHandler);
        }
    }

    @Override
    public void close() throws IOException {
        logger.info("Closing file writers ...");

        for (final var writer : writers) {
            writer.getFile().close();
            writer.getExecutor().close();
        }
    }
}
