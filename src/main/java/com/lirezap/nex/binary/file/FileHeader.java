package com.lirezap.nex.binary.file;

/**
 * @author Alireza Pourtaghi
 */
public final class FileHeader {
    private final long durabilitySize;

    public FileHeader(final long durabilitySize) {
        this.durabilitySize = durabilitySize;
    }

    public long getDurabilitySize() {
        return durabilitySize;
    }

    public int size() {
        return 8;
    }
}
