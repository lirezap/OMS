package com.lirezap.nex.net;

import com.lirezap.nex.binary.base.ErrorMessageBinaryRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base responder interface that includes read and write methods for TCP connections.
 * This interface is used by {@link Dispatcher} and {@link Handlers}.
 *
 * @author Alireza Pourtaghi
 */
public interface Responder {
    Logger logger = LoggerFactory.getLogger(Responder.class);

    ReadHandler readHandler = new ReadHandler();
    WriteHandler writeHandler = new WriteHandler();
    Runnable doNothing = () -> {};

    default void write(final Connection connection, final ErrorMessageBinaryRepresentation message) {
        try {
            final var buffer = message.buffer();
            connection.socket().write(buffer, buffer, new WriteErrorHandler(connection));
        } catch (Exception ex) {
            logger.error("write call failed: {}", ex.getMessage());

            connection.buffer().clear();
            read(connection);
        }
    }

    default void write(final Connection connection) {
        try {
            connection.socket().write(connection.buffer(), connection, writeHandler);
        } catch (Exception ex) {
            logger.error("write call failed: {}", ex.getMessage());

            connection.buffer().clear();
            read(connection);
        }
    }

    default void read(final Connection connection) {
        try {
            connection.socket().read(connection.buffer(), connection, readHandler);
        } catch (Exception ex) {
            logger.error("read call failed: {}", ex.getMessage());
            close(connection);
        }
    }

    default void close(final Connection connection) {
        try {
            connection.close();
        } catch (Exception ex) {
            logger.error("error while closing connection: {}", ex.getMessage());
        }
    }
}
