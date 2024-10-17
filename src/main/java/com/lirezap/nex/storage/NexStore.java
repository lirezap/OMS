package com.lirezap.nex.storage;

import com.lirezap.nex.binary.file.FileHeader;
import com.lirezap.nex.binary.file.FileHeaderBinaryRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import static java.nio.channels.FileChannel.open;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.move;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardOpenOption.*;
import static java.util.Objects.requireNonNull;

/**
 * Event/message store implementation. This implementation is not thread safe; see {@link ThreadSafeNexStore}.
 *
 * @author Alireza Pourtaghi
 */
public sealed class NexStore implements Closeable permits ThreadSafeNexStore {
    private static final Logger logger = LoggerFactory.getLogger(NexStore.class);

    private final Semaphore guard = new Semaphore(1);
    private final AtomicLong position = new AtomicLong(0);
    private final Path source;
    private final Path target;
    private final FileChannel file;
    private final FileHeaderBinaryRepresentation header =
            new FileHeaderBinaryRepresentation(Arena.global(), new FileHeader(0));

    public NexStore(final Path source) throws IOException {
        requireNonNull(source);
        checkSource(source);
        this.source = source;
        this.target = source.resolveSibling(source.getFileName() + ".mv");

        header.encodeV1();
        examineSource(source, target);

        this.file = open(source, CREATE, READ, WRITE, SYNC);
        examineFile(file);
        updateCurrentPosition(file);
    }

    public void write(final ByteBuffer buffer, final long position) {
        try {
            move(source, target, ATOMIC_MOVE);
        } catch (Exception ex) {
            logger.error("file system operation not supported: {}!", ex.getMessage(), ex);
            System.exit(-1);
        }

        var bytesWritten = 0;
        try (final var moved = open(target, READ, WRITE, SYNC)) {
            bytesWritten = moved.write(buffer, position);
            header.incrementDurabilitySize(bytesWritten);
            var _ = moved.write(header.buffer(), 0);
        } catch (Exception ex) {
            logger.error("write failed: {}!", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        } finally {
            try {
                move(target, source, ATOMIC_MOVE);
            } catch (IOException _) {
            }
        }
    }

    public void write(final MemorySegment segment, final long position) {
        write(segment.asByteBuffer(), position);
    }

    public void append(final ByteBuffer buffer) {
        try {
            move(source, target, ATOMIC_MOVE);
        } catch (Exception ex) {
            logger.error("file system operation not supported: {}!", ex.getMessage(), ex);
            System.exit(-1);
        }

        var bytesWritten = 0;
        try (final var moved = open(target, READ, WRITE, SYNC)) {
            bytesWritten = moved.write(buffer, position.getAndAdd(buffer.limit()));
            header.incrementDurabilitySize(bytesWritten);
            var _ = moved.write(header.buffer(), 0);
        } catch (Exception ex) {
            logger.error("write failed: {}!", ex.getMessage(), ex);
            position.addAndGet(-buffer.limit());

            throw new RuntimeException(ex);
        } finally {
            try {
                move(target, source, ATOMIC_MOVE);
            } catch (IOException _) {
            }
        }
    }

    public void append(final MemorySegment segment) {
        append(segment.asByteBuffer());
    }

    private void checkSource(final Path source) {
        if (isDirectory(source)) {
            throw new RuntimeException("provided path does not represent a file!");
        }
    }

    private void examineSource(final Path source, final Path target) {
        if (guard.tryAcquire()) {
            // Only one thread can reach this block at a time for a specific file!

            if (Files.notExists(source)) {
                // Source not exists!
                if (Files.exists(target)) {
                    // System crash or non-graceful shutdown?!
                    recover(target, source);
                } else {
                    // Then it is not created so far.
                    guard.release();
                }
            } else {
                guard.release();
            }
        } else {
            throw new RuntimeException("source file in process!");
        }
    }

    private void recover(final Path target, final Path source) {
        try (final var moved = open(target, READ, WRITE, SYNC);
             final var lock = moved.lock()) {

            moved.read(header.buffer(), 0);
            moved.truncate(header.durabilitySize());
            move(target, source, ATOMIC_MOVE);
        } catch (Exception ex) {
            logger.error("could not recover file: {}!", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        } finally {
            guard.release();
        }
    }

    private void examineFile(final FileChannel file) {
        if (guard.tryAcquire()) {
            // Only one thread can reach this block at a time for a specific file!

            try (final var lock = file.lock()) {
                if (file.size() == 0) {
                    header.incrementDurabilitySize(header.size());
                    var _ = file.write(header.buffer(), 0);
                } else {
                    file.read(header.buffer(), 0);
                }
            } catch (Exception ex) {
                logger.error("could not examine file: {}!", ex.getMessage(), ex);
                throw new RuntimeException(ex);
            } finally {
                guard.release();
            }
        } else {
            throw new RuntimeException("file in process!");
        }
    }

    private void updateCurrentPosition(final FileChannel file) {
        if (guard.tryAcquire()) {
            // Only one thread can reach this block at a time for a specific file!

            try (final var lock = file.lock()) {
                position.addAndGet(file.size());
            } catch (Exception ex) {
                logger.error("could not set position on file: {}!", ex.getMessage(), ex);
                throw new RuntimeException(ex);
            } finally {
                guard.release();
            }
        } else {
            throw new RuntimeException("file in process!");
        }
    }

    @Override
    public void close() throws IOException {
        file.close();
    }

    protected final Semaphore guard() {
        return guard;
    }
}
