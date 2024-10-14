package com.lirezap.nex.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardOpenOption.*;

/**
 * Safe event/message store implementation.
 *
 * @author Alireza Pourtaghi
 */
public final class NexStore implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(NexStore.class);

    private final Path source;
    private final Path target;
    private final FileChannel file;
    private final Semaphore guard;
    private final AtomicLong position;

    public NexStore(final Path source) {
        try {
            this.source = source;
            this.target = source.resolveSibling(source.getFileName() + ".mv");
            // TODO: Check it is a file not a directory!
            recover(target, source);
            this.file = FileChannel.open(source, CREATE, WRITE, SYNC);
            this.guard = new Semaphore(1);
            this.position = new AtomicLong(0);

            updateCurrentPosition();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void recover(final Path target, final Path source) {
        if (Files.exists(target)) {
            // System crash or non-graceful shutdown?!
            try {
                Files.move(target, source, ATOMIC_MOVE);
            } catch (Exception ex) {
                logger.error("could not recover file: {}!", ex.getMessage(), ex);
                System.exit(-1);
            }
        }
    }

    private void updateCurrentPosition() {
        if (guard.tryAcquire()) {
            // Only one thread can reach this block at a time for a specific file!
            try {
                final var lock = file.lock();
                position.addAndGet(file.size());
                lock.release();
                guard.release();
            } catch (Exception ex) {
                logger.error("could not release file lock: {}!", ex.getMessage(), ex);
                guard.release();

                System.exit(-1);
            }
        }
    }

    public void write(final ByteBuffer buffer, final long position) {
        try {
            Files.move(source, target, ATOMIC_MOVE);
            try (final var moved = FileChannel.open(target, CREATE, WRITE, SYNC)) {
                moved.write(buffer, position);
            } catch (Exception ex) {
                logger.error("move operation failed: {}!", ex.getMessage(), ex);
                throw new RuntimeException(ex);
            } finally {
                Files.move(target, source, ATOMIC_MOVE);
            }
        } catch (UnsupportedOperationException | IOException ex) {
            logger.error("file system operation not supported: {}!", ex.getMessage(), ex);
            System.exit(-1);
        }
    }

    public void write(final MemorySegment segment, final long position) {
        write(segment.asByteBuffer(), position);
    }

    public void append(final ByteBuffer buffer) {
        write(buffer, position.getAndAdd(buffer.limit()));
    }

    public void append(final MemorySegment segment) {
        write(segment, position.getAndAdd(segment.byteSize()));
    }

    @Override
    public void close() throws IOException {
        file.close();
    }
}
