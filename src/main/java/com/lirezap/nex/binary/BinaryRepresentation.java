package com.lirezap.nex.binary;

import com.lirezap.nex.context.Compression;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

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
        this(Arena.ofShared(), size);
    }

    protected BinaryRepresentation(final Arena arena, final int size) {
        this.arena = arena;
        this.segment = arena.allocate(RHS + size);
        this.size = size;
        this.position = new AtomicLong(0);
    }

    public final void encodeV1() {
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
    }

    public final MemorySegment compressLZ4(final Compression compression) {
        try {
            final var neededMemorySize = compression.lz4().compressBound(size);
            if (neededMemorySize <= 0) {
                throw new RuntimeException("could not compute compress bound!");
            }

            final var memory = arena.allocate(RHS + neededMemorySize);
            final var compressionSize = compression.lz4().compressDefault(segment.asSlice(RHS), memory.asSlice(RHS), size, neededMemorySize);
            if (compressionSize <= 0) {
                throw new RuntimeException("could not compress content!");
            }

            MemorySegment.copy(segment, 0, memory, 0, 6);
            memory.set(INT, 6, compressionSize);

            return memory.reinterpret(RHS + compressionSize);
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

        final var slice = segment.asSlice(position.getAndAdd(length), length);
        for (int i = 0; i < length; i++) {
            slice.set(BYTE, i, bytes[i]);
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

    public final long size() {
        return RHS + size;
    }

    public final int recordSize() {
        return size;
    }

    @Override
    public final void close() {
        arena.close();
    }
}
