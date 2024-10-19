package com.lirezap.nex;

import com.lirezap.nex.binary.base.ErrorMessage;
import com.lirezap.nex.binary.base.ErrorMessageBinaryRepresentation;

import java.lang.foreign.Arena;

/**
 * A list of global error messages.
 *
 * @author Alireza Pourtaghi
 */
public final class ErrorMessages {

    public static final ErrorMessageBinaryRepresentation MESSAGE_VERSION_NOT_SUPPORTED =
            new ErrorMessageBinaryRepresentation(
                    Arena.global(),
                    new ErrorMessage("message_version.not_supported", "message version not supported"));

    public static final ErrorMessageBinaryRepresentation MESSAGE_FORMAT_NOT_VALID =
            new ErrorMessageBinaryRepresentation(
                    Arena.global(),
                    new ErrorMessage("message_format.not_valid", "message format is not valid"));

    public static final ErrorMessageBinaryRepresentation MESSAGE_LENGTH_TOO_BIG =
            new ErrorMessageBinaryRepresentation(
                    Arena.global(),
                    new ErrorMessage("message_length.too_big", "message length is too big to handle"));

    static {
        MESSAGE_VERSION_NOT_SUPPORTED.encodeV1();
        MESSAGE_FORMAT_NOT_VALID.encodeV1();
        MESSAGE_LENGTH_TOO_BIG.encodeV1();
    }
}
