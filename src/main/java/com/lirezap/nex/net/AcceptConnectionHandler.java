package com.lirezap.nex.net;

import com.lirezap.nex.context.AppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ShutdownChannelGroupException;

/**
 * Stateless completion handler that accepts incoming socket connections.
 *
 * @author Alireza Pourtaghi
 */
public final class AcceptConnectionHandler implements CompletionHandler<AsynchronousSocketChannel, AppContext> {
    private static final Logger logger = LoggerFactory.getLogger(AcceptConnectionHandler.class);
    private static final ReadHandler readHandler = new ReadHandler();

    @Override
    public void completed(final AsynchronousSocketChannel socket, final AppContext context) {
        listen(context);
        read(new Connection(socket, context.config().loadInt("server.read_buffer_size")));
    }

    @Override
    public void failed(final Throwable th, final AppContext context) {
        if (th.getCause() instanceof ShutdownChannelGroupException) {
            logger.error("closed accepted connection because of channel group shutdown");
        } else if (th.getCause() instanceof SecurityException) {
            logger.error("closed accepted connection because of security restricted accept call");
        } else {
            logger.error("accept connection failed: {}", th.getMessage());
        }
    }

    private void listen(final AppContext context) {
        try {
            context.nexServer().listen();
        } catch (Exception ex) {
            logger.error("listen call failed for next connection: {}", ex.getMessage());
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
