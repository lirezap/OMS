package com.openex.oms.storage;

import com.openex.oms.binary.file.FileHeader;
import com.openex.oms.binary.file.FileHeaderBinaryRepresentation;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.System.exit;
import static java.lang.foreign.Arena.global;
import static java.nio.channels.FileChannel.open;
import static java.nio.file.Files.*;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardOpenOption.*;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Atomic guaranteed event/message store implementation. This implementation is not thread safe; see
 * {@link ThreadSafeAtomicFile} implementation for use by multiple threads.
 *
 * @author Alireza Pourtaghi
 */
public sealed class AtomicFile implements Closeable permits ThreadSafeAtomicFile {
    private static final Logger logger = getLogger(AtomicFile.class);

    private final Semaphore guard;
    private final AtomicLong position;
    private final Path source;
    private final Path target;
    private final FileHeaderBinaryRepresentation header;
    private final FileChannel file;

    public AtomicFile(final Path source) throws IOException {
        requireNonNull(source);
        checkSource(source);

        this.guard = new Semaphore(1);
        this.position = new AtomicLong(0);
        this.source = source;
        this.target = source.resolveSibling(this.source.getFileName() + ".mv");
        this.header = new FileHeaderBinaryRepresentation(global(), new FileHeader(0));
        header.encodeV1();

        examineSource(this.source, this.target);
        this.file = open(this.source, CREATE, READ, WRITE, SYNC);
        examineFile(this.file);
        updateCurrentPosition(this.file);
    }

    public void write(final ByteBuffer buffer, final long position) {
        try {
            move(source, target, ATOMIC_MOVE);
        } catch (Exception ex) {
            logger.error("file system operation not supported: {}!", ex.getMessage(), ex);
            exit(-1);
        }

        try (final var moved = open(target, READ, WRITE, SYNC)) {
            var bytesWritten = 0;
            while (buffer.remaining() > 0) {
                bytesWritten += moved.write(buffer, position + bytesWritten);
            }

            header.incrementDurabilitySize(bytesWritten);
            final var headerAsBuffer = header.buffer();

            bytesWritten = 0;
            while (headerAsBuffer.remaining() > 0) {
                bytesWritten += moved.write(headerAsBuffer, bytesWritten);
            }
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
            exit(-1);
        }

        try (final var moved = open(target, READ, WRITE, SYNC)) {
            var bytesWritten = 0;
            var localPosition = position.getAndAdd(buffer.limit());
            while (buffer.remaining() > 0) {
                bytesWritten += moved.write(buffer, localPosition + bytesWritten);
            }

            header.incrementDurabilitySize(bytesWritten);
            final var headerAsBuffer = header.buffer();

            bytesWritten = 0;
            while (headerAsBuffer.remaining() > 0) {
                bytesWritten += moved.write(headerAsBuffer, bytesWritten);
            }
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

    public MemorySegment read(final Arena arena, final long position, final long size) throws IOException {
        final var segment = arena.allocate(size);
        file.read(segment.asByteBuffer().clear(), position);

        return segment;
    }

    private void checkSource(final Path source) {
        if (isDirectory(source)) {
            throw new RuntimeException("provided path does not represent a file!");
        }
    }

    private void examineSource(final Path source, final Path target) {
        if (guard.tryAcquire()) {
            // Only one thread can reach this block at a time for a specific file!

            if (notExists(source)) {
                // Source not exists!
                if (exists(target)) {
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
                    header.incrementDurabilitySize(header.representationSize());
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
    public final void close() throws IOException {
        file.close();
    }

    protected final Semaphore guard() {
        return guard;
    }

    public final Path source() {
        return source;
    }
}
