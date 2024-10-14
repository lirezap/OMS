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

    private final Path path;
    private final Path move;
    private final FileChannel file;
    private final Semaphore guard;
    private final AtomicLong position;

    public NexStore(final Path path) {
        try {
            this.path = path;
            this.move = path.resolveSibling(path.getFileName() + ".mv");
            // TODO: Check it is a file not a directory!
            recover(move, path);
            this.file = FileChannel.open(path, CREATE, WRITE, SYNC);
            this.guard = new Semaphore(1);
            this.position = new AtomicLong(0);

            updateCurrentPosition();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void recover(final Path move, final Path path) {
        if (Files.exists(move)) {
            // System crash or non-graceful shutdown?!
            try {
                Files.move(move, path, ATOMIC_MOVE);
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
            Files.move(path, move, ATOMIC_MOVE);
            try (final var moved = FileChannel.open(move, CREATE, WRITE, SYNC)) {
                moved.write(buffer, position);
            } catch (Exception ex) {
                logger.error("move operation failed: {}!", ex.getMessage(), ex);
                throw new RuntimeException(ex);
            } finally {
                Files.move(move, path, ATOMIC_MOVE);
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
