package com.lirezap.nex.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.CompletionHandler;

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
        if (bytes == EOS) {
            logger.trace("end of stream detected; closing connection ...");
            close(connection);
            return;
        }

        if (bytes == 0 && connection.buffer().position() == connection.buffer().limit()) {
            logger.warn("full buffer loop detected; closing connection ...");
            close(connection);
            return;
        }

        if (bytes > 0) {
            connection.buffer().flip();
            // TODO: Handle buffer code here.
            connection.socket().write(connection.buffer());

            connection.buffer().clear();
            connection.socket().read(connection.buffer(), connection, this);
        }
    }

    @Override
    public void failed(final Throwable th, final Connection connection) {
        logger.error("read operation failed: {}", th.getMessage());
        close(connection);
    }

    private void close(final Connection connection) {
        try {
            connection.close();
        } catch (Exception ex) {
            logger.error("error while closing connection: {}", ex.getMessage());
        }
    }
}
