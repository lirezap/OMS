package com.lirezap.nex.net;

import com.lirezap.nex.binary.base.ErrorMessageBinaryRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.lirezap.nex.ErrorMessages.MESSAGE_VERSION_NOT_SUPPORTED;
import static com.lirezap.nex.binary.BinaryRepresentable.VR1;

/**
 * Dispatcher implementation that dispatches incoming messages to appropriate handlers.
 *
 * @author Alireza Pourtaghi
 */
public final class Dispatcher {
    private static final Logger logger = LoggerFactory.getLogger(Dispatcher.class);

    private final ReadHandler readHandler;
    private final WriteHandler writeHandler;

    public Dispatcher() {
        this.readHandler = new ReadHandler();
        this.writeHandler = new WriteHandler();
    }

    public void dispatch(final Connection connection) {
        if (connection.buffer().get() != VR1) {
            write(connection, MESSAGE_VERSION_NOT_SUPPORTED);
        }

        // TODO: Complete implementation.
    }

    private void write(final Connection connection, final ErrorMessageBinaryRepresentation message) {
        try {
            final var buffer = message.buffer();
            connection.socket().write(buffer, buffer, new WriteErrorHandler(connection));
        } catch (Exception ex) {
            logger.error("write call failed: {}", ex.getMessage());

            connection.buffer().clear();
            read(connection);
        }
    }

    private void read(final Connection connection) {
        try {
            connection.socket().read(connection.buffer(), connection, readHandler);
        } catch (Exception ex) {
            logger.error("read call failed: {}", ex.getMessage());
            close(connection);
        }
    }

    private void close(final Connection connection) {
        try {
            connection.close();
        } catch (Exception ex) {
            logger.error("error while closing connection: {}", ex.getMessage());
        }
    }
}
