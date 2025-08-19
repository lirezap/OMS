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

import software.openex.oms.context.Compression;
import software.openex.oms.event.binary.CompressEvent;
import software.openex.oms.event.binary.EncodeEvent;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.List;

import static java.lang.foreign.Arena.ofShared;
import static java.lang.foreign.MemorySegment.copy;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Bytes representation of any model. Be curious that the class is not thread safe.
 *
 * @author Alireza Pourtaghi
 */
public abstract class BinaryRepresentation<T> implements BinaryRepresentable, AutoCloseable {
    private final Arena arena;
    private final MemorySegment segment;
    private final int size;
    private long position;

    protected BinaryRepresentation(final int size) {
        this(ofShared(), size);
    }

    protected BinaryRepresentation(final Arena arena, final int size) {
        this.arena = arena;
        this.segment = arena.allocate(RHS + size);
        this.size = size;
        this.position = 0;
    }

    public final void encodeV1() {
        final var event = new EncodeEvent(id());
        event.begin();

        putByte(VR1);
        putByte(FGS);
        putInt(id());
        putInt(size);
        encodeRecord();

        event.end();
        event.commit();
    }

    public final MemorySegment compressLZ4(final Compression compression) {
        // TODO: Check possible arithmetic overflow.
        try {
            final var event = new CompressEvent(id());
            event.begin();

            final var neededMemorySize = compression.lz4().compressBound(size);
            if (neededMemorySize <= 0) throw new RuntimeException("could not compute compress bound!");

            final var memory = arena.allocate(RHS + 4 + neededMemorySize);
            final var compressionSize = compression.lz4().compressDefault(
                    segment.asSlice(RHS), memory.asSlice(RHS + 4), size, neededMemorySize);
            if (compressionSize <= 0) throw new RuntimeException("could not compress content!");

            copy(segment, 0, memory, 0, 6);
            setCompressed(memory);
            memory.set(INT, 6, 4 + compressionSize);
            memory.set(INT, RHS, size);
            final var reinterpretMemory = memory.reinterpret(RHS + 4 + compressionSize);

            event.end();
            event.commit();
            return reinterpretMemory;
        } catch (Throwable th) {
            throw new RuntimeException(th);
        }
    }

    protected final void putByte(final byte value) {
        // TODO: Check possible arithmetic overflow.
        segment.set(BYTE, position, value);
        position += BYTE.byteSize();
    }

    protected final void putInt(final int value) {
        // TODO: Check possible arithmetic overflow.
        segment.set(INT, position, value);
        position += INT.byteSize();
    }

    protected final void putLong(final long value) {
        // TODO: Check possible arithmetic overflow.
        segment.set(LONG, position, value);
        position += LONG.byteSize();
    }

    protected final void putString(final String value) {
        // TODO: Check possible arithmetic overflow.
        var length = value.getBytes(UTF_8).length;
        if (length == Integer.MAX_VALUE) throw new IllegalArgumentException("size of string value is too big!");

        // Null terminated
        length++;
        putInt(length);
        segment.setString(position, value);
        position += length;
    }

    protected final void putBytes(final byte[] bytes) {
        // TODO: Check possible arithmetic overflow.
        final var length = bytes.length;

        putInt(length);
        copy(bytes, 0, segment, BYTE, position, length);
        position += length;
    }

    protected final <C> void putBinaryRepresentations(final List<BinaryRepresentation<C>> brs) {
        // TODO: Check possible arithmetic overflow.
        putInt(brs.size());

        for (final var br : brs) {
            final var brSegmentSize = br.segment().byteSize();
            copy(br.segment(), 0, segment, position, brSegmentSize);
            position += brSegmentSize;
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
