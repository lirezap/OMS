package com.openex.oms.net;

import com.openex.oms.binary.base.ErrorMessage;
import com.openex.oms.binary.base.ErrorMessageBinaryRepresentation;

import static java.lang.foreign.Arena.global;

/**
 * A list of global error messages.
 *
 * @author Alireza Pourtaghi
 */
public final class ErrorMessages {

    public static final ErrorMessageBinaryRepresentation MESSAGE_FORMAT_NOT_VALID =
            new ErrorMessageBinaryRepresentation(
                    global(),
                    new ErrorMessage("message_format.not_valid", "message's format is not valid"));

    public static final ErrorMessageBinaryRepresentation MESSAGE_LENGTH_TOO_BIG =
            new ErrorMessageBinaryRepresentation(
                    global(),
                    new ErrorMessage("message_length.too_big", "message's length is too big to handle"));

    public static final ErrorMessageBinaryRepresentation MESSAGE_VERSION_NOT_SUPPORTED =
            new ErrorMessageBinaryRepresentation(
                    global(),
                    new ErrorMessage("message_version.not_supported", "message's version not supported"));

    public static final ErrorMessageBinaryRepresentation MESSAGE_SIZE_NOT_VALID =
            new ErrorMessageBinaryRepresentation(
                    global(),
                    new ErrorMessage("message_size.not_valid", "message's size is not valid"));

    public static final ErrorMessageBinaryRepresentation MESSAGE_NOT_SUPPORTED =
            new ErrorMessageBinaryRepresentation(
                    global(),
                    new ErrorMessage("message.not_supported", "message is not supported"));

    public static final ErrorMessageBinaryRepresentation ORDER_ALREADY_EXISTS =
            new ErrorMessageBinaryRepresentation(
                    global(),
                    new ErrorMessage("order.already_exists", "order already exists"));

    public static final ErrorMessageBinaryRepresentation INTERNAL_SERVER_ERROR =
            new ErrorMessageBinaryRepresentation(
                    global(),
                    new ErrorMessage("server.error", "internal server error"));

    static {
        MESSAGE_FORMAT_NOT_VALID.encodeV1();
        MESSAGE_LENGTH_TOO_BIG.encodeV1();
        MESSAGE_VERSION_NOT_SUPPORTED.encodeV1();
        MESSAGE_SIZE_NOT_VALID.encodeV1();
        MESSAGE_NOT_SUPPORTED.encodeV1();
        ORDER_ALREADY_EXISTS.encodeV1();
        INTERNAL_SERVER_ERROR.encodeV1();
    }
}
