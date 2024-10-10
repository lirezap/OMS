package com.lirezap.nex.binary;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility methods for binary representations.
 *
 * @author Alireza Pourtaghi
 */
public interface BinaryRepresentations {

    static int representationSize(final String value) {
        return value.getBytes(UTF_8).length + 5;
    }

    static int representationSize(final byte[] value) {
        return value.length + 8;
    }
}
