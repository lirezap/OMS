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

import java.lang.foreign.MemorySegment;
import java.util.List;

import static java.lang.Math.addExact;
import static java.lang.foreign.ValueLayout.*;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * {@link BinaryRepresentation} base interface including utility fields and methods.
 *
 * @author Alireza Pourtaghi
 */
public interface BinaryRepresentable {
    OfByte BYTE = JAVA_BYTE.withOrder(BIG_ENDIAN);
    OfInt INT = JAVA_INT_UNALIGNED.withOrder(BIG_ENDIAN);
    OfLong LONG = JAVA_LONG_UNALIGNED.withOrder(BIG_ENDIAN);

    // Representation's header size
    int RHS = 1 + 1 + 4 + 4;

    // Representation's version
    byte VR1 = 0b00000001;

    // Flags; 8 flags can be used in a single byte
    byte FGS = 0b00000000;

    // Line feed
    byte LFD = 0x0A;

    // Carriage Return
    byte CRT = 0x0D;

    default void setCompressed(final MemorySegment segment) {
        segment.set(BYTE, 1, (byte) (flags(segment) | 0b00000001));
    }

    default boolean isCompressed(final MemorySegment segment) {
        return (flags(segment) & 0b00000001) == 0b00000001;
    }

    static byte version(final MemorySegment segment) {
        return segment.get(BYTE, 0);
    }

    static byte flags(final MemorySegment segment) {
        return segment.get(BYTE, 1);
    }

    static int id(final MemorySegment segment) {
        return segment.get(INT, 2);
    }

    static int size(final MemorySegment segment) {
        return segment.get(INT, 6);
    }

    static int originalSize(final MemorySegment segment) {
        return segment.get(INT, RHS);
    }

    static int representationSize(final String value) {
        return addExact(addExact(4, value.getBytes(UTF_8).length), 1);
    }

    static int representationSize(final byte[] value) {
        return addExact(4, value.length);
    }

    static <T> int representationSize(final List<BinaryRepresentation<T>> brs) {
        var size = 0;
        for (final var br : brs) {
            size = addExact(size, (int) br.representationSize());
        }

        return addExact(4, size);
    }
}
