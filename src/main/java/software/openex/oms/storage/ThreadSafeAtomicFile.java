/*
 * ISC License
 *
 * Copyright (c) 2025, Alireza Pourtaghi <lirezap@protonmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package software.openex.oms.storage;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.file.Path;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Thread safe implementation of {@link AtomicFile}.
 *
 * @author Alireza Pourtaghi
 */
public final class ThreadSafeAtomicFile extends AtomicFile {
    private final int timeoutMillis;

    public ThreadSafeAtomicFile(final Path source, final int timeoutMillis) throws IOException {
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

    @Override
    public MemorySegment read(final Arena arena, final long position, final long size) throws IOException {
        try {
            if (guard().tryAcquire(timeoutMillis, MILLISECONDS)) {
                try {
                    return super.read(arena, position, size);
                } finally {
                    guard().release();
                }
            } else {
                throw new RuntimeException("read operation timed out!");
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
