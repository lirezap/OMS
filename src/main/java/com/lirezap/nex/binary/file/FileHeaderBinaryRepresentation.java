package com.lirezap.nex.binary.file;

import com.lirezap.nex.binary.BinaryRepresentation;

import java.lang.foreign.Arena;

/**
 * @author Alireza Pourtaghi
 */
public final class FileHeaderBinaryRepresentation extends BinaryRepresentation<FileHeader> {
    private final FileHeader fileHeader;

    public FileHeaderBinaryRepresentation(final FileHeader fileHeader) {
        super(fileHeader.size());
        this.fileHeader = fileHeader;
    }

    public FileHeaderBinaryRepresentation(final Arena arena, final FileHeader fileHeader) {
        super(arena, fileHeader.size());
        this.fileHeader = fileHeader;
    }

    @Override
    protected int id() {
        return 1;
    }

    @Override
    protected void encodeRecord() {
        try {
            putLong(fileHeader.getDurabilitySize());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public long durabilitySize() {
        // TODO: Safe?!
        return segment().get(LONG, RHS);
    }

    public void incrementDurabilitySize(final long inc) {
        // TODO: Safe?!
        LONG.varHandle().set(segment(), RHS, durabilitySize() + inc);
    }
}
