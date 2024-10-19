package com.lirezap.nex.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.CompletionHandler;

import static com.lirezap.nex.context.AppContext.context;

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

        if (connection.buffer().limit() < connection.buffer().capacity()) {
            context().dispatcher().dispatch(connection);
        }

        if (connection.buffer().limit() == connection.buffer().capacity()) {
            // TODO: Complete implementation.
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
