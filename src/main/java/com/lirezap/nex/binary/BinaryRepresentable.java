package com.lirezap.nex.binary;

import static java.lang.foreign.ValueLayout.*;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A binary representable base interface including utility methods.
 *
 * @author Alireza Pourtaghi
 */
public interface BinaryRepresentable {
    OfByte BYTE = JAVA_BYTE.withOrder(BIG_ENDIAN);
    OfShort SHORT = JAVA_SHORT_UNALIGNED.withOrder(BIG_ENDIAN);
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

    static int representationSize(final String value) {
        return 4 + value.getBytes(UTF_8).length + 1;
    }

    static int representationSize(final byte[] value) {
        return 4 + value.length;
    }
}
