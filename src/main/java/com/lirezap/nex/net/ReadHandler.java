package com.lirezap.nex.net;

import com.lirezap.nex.binary.base.ErrorMessageBinaryRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.CompletionHandler;

import static com.lirezap.nex.ErrorMessages.MESSAGE_FORMAT_NOT_VALID;
import static com.lirezap.nex.ErrorMessages.MESSAGE_LENGTH_TOO_BIG;
import static com.lirezap.nex.binary.BinaryRepresentable.RHS;
import static com.lirezap.nex.binary.BinaryRepresentation.size;
import static com.lirezap.nex.context.AppContext.context;
import static com.lirezap.nex.net.Connection.extendSegment;

/**
 * Stateless completion handler that reads bytes from a channel.
 *
 * @author Alireza Pourtaghi
 */
public final class ReadHandler implements CompletionHandler<Integer, Connection> {
    private static final Logger logger = LoggerFactory.getLogger(ReadHandler.class);
    private static final int EOS = -1;

    @Override
    public void completed(final Integer bytes, final Connection connection) {
        switch (bytes) {
            case EOS -> handleEOS(connection);
            case 0 -> handleZeroBytesReceived(connection);
            default -> handleMessage(connection);
        }
    }

    @Override
    public void failed(final Throwable th, final Connection connection) {
        logger.error("read operation failed: {}", th.getMessage());
        close(connection);
    }

    private void handleEOS(final Connection connection) {
        logger.trace("end of stream detected; closing connection ...");
        close(connection);
    }

    private void handleZeroBytesReceived(final Connection connection) {
        if (connection.buffer().position() < connection.buffer().limit()) {
            read(connection);
        } else {
            logger.warn("full buffer loop detected; closing connection ...");
            close(connection);
        }
    }

    private void handleMessage(final Connection connection) {
        connection.buffer().flip();
        logger.trace("Buffer: {}", connection.buffer());

        if (connection.buffer().limit() <= RHS) {
            write(connection, MESSAGE_FORMAT_NOT_VALID);
            return;
        }

        if (connection.buffer().limit() > context().config().loadInt("server.max_message_size")) {
            write(connection, MESSAGE_LENGTH_TOO_BIG);
            return;
        }

        if (connection.buffer().limit() < connection.buffer().capacity()) {
            context().dispatcher().dispatch(connection);
            return;
        }

        if (connection.buffer().limit() == connection.buffer().capacity()) {
            if (connection.buffer().limit() == (RHS + size(connection.buffer()))) {
                context().dispatcher().dispatch(connection);
            } else {
                // Extended buffer size remains for connection.
                final var extendedSegmentConnection = extendSegment(connection, context().config().loadInt("server.read_buffer_size"));
                read(extendedSegmentConnection);
            }
        }
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
            connection.socket().read(connection.buffer(), connection, this);
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
