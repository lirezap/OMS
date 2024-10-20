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

    public static final ErrorMessageBinaryRepresentation MESSAGE_FORMAT_NOT_VALID =
            new ErrorMessageBinaryRepresentation(
                    Arena.global(),
                    new ErrorMessage("message_format.not_valid", "message's format is not valid"));

    public static final ErrorMessageBinaryRepresentation MESSAGE_VERSION_NOT_SUPPORTED =
            new ErrorMessageBinaryRepresentation(
                    Arena.global(),
                    new ErrorMessage("message_version.not_supported", "message's version not supported"));

    public static final ErrorMessageBinaryRepresentation MESSAGE_SIZE_NOT_VALID =
            new ErrorMessageBinaryRepresentation(
                    Arena.global(),
                    new ErrorMessage("message_size.not_valid", "message's size is not valid"));

    public static final ErrorMessageBinaryRepresentation MESSAGE_NOT_SUPPORTED =
            new ErrorMessageBinaryRepresentation(
                    Arena.global(),
                    new ErrorMessage("message.not_supported", "message is not supported"));

    public static final ErrorMessageBinaryRepresentation MESSAGE_LENGTH_TOO_BIG =
            new ErrorMessageBinaryRepresentation(
                    Arena.global(),
                    new ErrorMessage("message_length.too_big", "message's length is too big to handle"));

    public static final ErrorMessageBinaryRepresentation HANDLER_NOT_IMPLEMENTED =
            new ErrorMessageBinaryRepresentation(
                    Arena.global(),
                    new ErrorMessage("handler.not_implemented", "handler is not implemented"));

    static {
        MESSAGE_FORMAT_NOT_VALID.encodeV1();
        MESSAGE_VERSION_NOT_SUPPORTED.encodeV1();
        MESSAGE_SIZE_NOT_VALID.encodeV1();
        MESSAGE_NOT_SUPPORTED.encodeV1();
        MESSAGE_LENGTH_TOO_BIG.encodeV1();
        HANDLER_NOT_IMPLEMENTED.encodeV1();
    }
}
