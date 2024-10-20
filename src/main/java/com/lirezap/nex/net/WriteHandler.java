package com.lirezap.nex.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.CompletionHandler;

/**
 * Stateless completion handler that write bytes into a channel.
 *
 * @author Alireza Pourtaghi
 */
public final class WriteHandler implements CompletionHandler<Integer, Connection> {
    private static final Logger logger = LoggerFactory.getLogger(WriteHandler.class);
    private static final ReadHandler readHandler = new ReadHandler();

    @Override
    public void completed(final Integer bytes, final Connection connection) {
        if (connection.buffer().remaining() > 0) {
            write(connection);
        } else {
            connection.buffer().clear();
            read(connection);
        }
    }

    @Override
    public void failed(final Throwable th, final Connection connection) {
        logger.error("write operation failed: {}", th.getMessage());

        connection.buffer().clear();
        read(connection);
    }

    private void write(final Connection connection) {
        try {
            connection.socket().write(connection.buffer(), connection, this);
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
