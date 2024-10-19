package com.lirezap.nex.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

/**
 * Completion handler that write error bytes into a channel.
 *
 * @author Alireza Pourtaghi
 */
public final class WriteErrorHandler implements CompletionHandler<Integer, ByteBuffer> {
    private static final Logger logger = LoggerFactory.getLogger(WriteErrorHandler.class);

    private final ReadHandler readHandler;
    private final Connection connection;

    public WriteErrorHandler(final Connection connection) {
        this.readHandler = new ReadHandler();
        this.connection = connection;
    }

    @Override
    public void completed(final Integer bytes, final ByteBuffer buffer) {
        if (buffer.remaining() > 0) {
            write(buffer);
        } else {
            connection.buffer().clear();
            read(connection);
        }
    }

    @Override
    public void failed(final Throwable th, final ByteBuffer buffer) {
        logger.error("write error operation failed: {}", th.getMessage());

        connection.buffer().clear();
        read(connection);
    }

    private void write(final ByteBuffer buffer) {
        try {
            connection.socket().write(buffer, buffer, this);
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
