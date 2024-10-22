package com.lirezap.nex.net;

import com.lirezap.nex.binary.base.ErrorMessageBinaryRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.lirezap.nex.ErrorMessages.*;
import static com.lirezap.nex.binary.BinaryRepresentable.RHS;
import static com.lirezap.nex.binary.BinaryRepresentation.*;
import static com.lirezap.nex.context.AppContext.context;

/**
 * Dispatcher implementation that dispatches incoming messages to appropriate handlers.
 *
 * @author Alireza Pourtaghi
 */
public final class Dispatcher {
    private static final Logger logger = LoggerFactory.getLogger(Dispatcher.class);
    private static final ReadHandler readHandler = new ReadHandler();
    private static final Handlers handlers = new Handlers();

    public void dispatch(final Connection connection) {
        if (isValid(connection)) {
            context().executors().worker().submit(() -> {
                switch (id(connection.segment())) {
                    case 101 -> handlers.handleBuyOrder(connection);
                    case 102 -> handlers.handleSellOrder(connection);

                    default -> write(connection, MESSAGE_NOT_SUPPORTED);
                }
            });
        }
    }

    private boolean isValid(final Connection connection) {
        if (connection.buffer().limit() <= RHS) {
            write(connection, MESSAGE_FORMAT_NOT_VALID);
            return false;
        }

        if (version(connection.segment()) != 1) {
            write(connection, MESSAGE_VERSION_NOT_SUPPORTED);
            return false;
        }

        if (size(connection.segment()) != (connection.buffer().limit() - RHS)) {
            write(connection, MESSAGE_SIZE_NOT_VALID);
            return false;
        }

        return true;
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
