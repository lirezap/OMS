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
package software.openex.oms.binary;

import software.openex.oms.binary.event.CompressEvent;
import software.openex.oms.binary.event.EncodeEvent;
import software.openex.oms.context.Compression;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.foreign.Arena.ofShared;
import static java.lang.foreign.MemorySegment.copy;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Bytes representation of any model.
 *
 * @author Alireza Pourtaghi
 */
public abstract class BinaryRepresentation<T> implements BinaryRepresentable, AutoCloseable {
    private final Arena arena;
    private final MemorySegment segment;
    private final int size;
    private final AtomicLong position;

    protected BinaryRepresentation(final int size) {
        this(ofShared(), size);
    }

    protected BinaryRepresentation(final Arena arena, final int size) {
        this.arena = arena;
        this.segment = arena.allocate(RHS + size);
        this.size = size;
        this.position = new AtomicLong(0);
    }

    public final void encodeV1() {
        final var event = new EncodeEvent(id());
        event.begin();

        // Version
        segment.set(BYTE, position.getAndAdd(BYTE.byteSize()), VR1);
        // Flags
        segment.set(BYTE, position.getAndAdd(BYTE.byteSize()), FGS);
        // Record's id
        segment.set(INT, position.getAndAdd(INT.byteSize()), id());
        // Record's size
        segment.set(INT, position.getAndAdd(INT.byteSize()), size);
        // Record
        encodeRecord();

        event.end();
        event.commit();
    }

    public final MemorySegment compressLZ4(final Compression compression) {
        try {
            final var event = new CompressEvent(id());
            event.begin();

            final var neededMemorySize = compression.lz4().compressBound(size);
            if (neededMemorySize <= 0) {
                throw new RuntimeException("could not compute compress bound!");
            }

            final var memory = arena.allocate(RHS + neededMemorySize);
            final var compressionSize = compression.lz4().compressDefault(segment.asSlice(RHS), memory.asSlice(RHS), size, neededMemorySize);
            if (compressionSize <= 0) {
                throw new RuntimeException("could not compress content!");
            }

            copy(segment, 0, memory, 0, 6);
            memory.set(INT, 6, compressionSize);
            setCompressed(memory);
            final var reinterpretMemory = memory.reinterpret(RHS + compressionSize);

            event.end();
            event.commit();
            return reinterpretMemory;
        } catch (Throwable th) {
            throw new RuntimeException(th);
        }
    }

    public final void putByte(final byte value) {
        segment.set(BYTE, position.getAndAdd(BYTE.byteSize()), value);
    }

    public final void putShort(final short value) {
        segment.set(SHORT, position.getAndAdd(SHORT.byteSize()), value);
    }

    public final void putInt(final int value) {
        segment.set(INT, position.getAndAdd(INT.byteSize()), value);
    }

    public final void putLong(final long value) {
        segment.set(LONG, position.getAndAdd(LONG.byteSize()), value);
    }

    public final void putString(final String value) {
        var length = value.getBytes(UTF_8).length;
        if (length == Integer.MAX_VALUE) throw new IllegalArgumentException("size of string value is too big!");

        // Null terminated
        length++;
        putInt(length);
        segment.setString(position.getAndAdd(length), value);
    }

    public final void putBytes(final byte[] bytes) {
        final var length = bytes.length;

        putInt(length);
        copy(bytes, 0, segment, BYTE, position.getAndAdd(length), length);
    }

    public final <C> void putBinaryRepresentations(final List<BinaryRepresentation<C>> binaryRepresentations) {
        putInt(binaryRepresentations.size());

        for (final var br : binaryRepresentations) {
            final var brSegmentSize = br.segment().byteSize();
            copy(br.segment(), 0, segment, position.getAndAdd(brSegmentSize), brSegmentSize);
        }
    }

    protected abstract int id();

    protected abstract void encodeRecord();

    public final MemorySegment segment() {
        return segment;
    }

    public final ByteBuffer buffer() {
        return segment.asByteBuffer();
    }

    public final int size() {
        return size;
    }

    public final long representationSize() {
        return RHS + size;
    }

    @Override
    public final void close() {
        arena.close();
    }
}
