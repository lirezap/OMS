package com.lirezap.nex.storage;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.file.Path;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Thread safe event/message store implementation.
 *
 * @author Alireza Pourtaghi
 */
public final class ThreadSafeNexStore extends NexStore {
    private final int timeoutMillis;

    public ThreadSafeNexStore(final Path source, final int timeoutMillis) throws IOException {
        super(source);
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public void write(final ByteBuffer buffer, final long position) {
        try {
            if (guard().tryAcquire(timeoutMillis, MILLISECONDS)) {
                try {
                    super.write(buffer, position);
                } finally {
                    guard().release();
                }
            } else {
                throw new RuntimeException("write operation timed out!");
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void write(final MemorySegment segment, final long position) {
        try {
            if (guard().tryAcquire(timeoutMillis, MILLISECONDS)) {
                try {
                    super.write(segment.asByteBuffer(), position);
                } finally {
                    guard().release();
                }
            } else {
                throw new RuntimeException("write operation timed out!");
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void append(final ByteBuffer buffer) {
        try {
            if (guard().tryAcquire(timeoutMillis, MILLISECONDS)) {
                try {
                    super.append(buffer);
                } finally {
                    guard().release();
                }
            } else {
                throw new RuntimeException("append operation timed out!");
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void append(final MemorySegment segment) {
        try {
            if (guard().tryAcquire(timeoutMillis, MILLISECONDS)) {
                try {
                    super.append(segment.asByteBuffer());
                } finally {
                    guard().release();
                }
            } else {
                throw new RuntimeException("append operation timed out!");
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
