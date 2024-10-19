package com.lirezap.nex.binary.base;

import com.lirezap.nex.binary.BinaryRepresentation;

import java.lang.foreign.Arena;

/**
 * @author Alireza Pourtaghi
 */
public final class ErrorMessageBinaryRepresentation extends BinaryRepresentation<ErrorMessage> {
    private final ErrorMessage errorMessage;

    public ErrorMessageBinaryRepresentation(final ErrorMessage errorMessage) {
        super(errorMessage.size());
        this.errorMessage = errorMessage;
    }

    public ErrorMessageBinaryRepresentation(final Arena arena, final ErrorMessage errorMessage) {
        super(arena, errorMessage.size());
        this.errorMessage = errorMessage;
    }

    @Override
    protected int id() {
        return -1;
    }

    @Override
    protected void encodeRecord() {
        try {
            putString(errorMessage.getCode());
            putString(errorMessage.getMessage());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
