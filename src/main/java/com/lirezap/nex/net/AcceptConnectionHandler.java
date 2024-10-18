package com.lirezap.nex.net;

import com.lirezap.nex.context.AppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ShutdownChannelGroupException;

/**
 * A completion handler that accepts incoming socket connections.
 *
 * @author Alireza Pourtaghi
 */
public final class AcceptConnectionHandler implements CompletionHandler<AsynchronousSocketChannel, AppContext> {
    private static final Logger logger = LoggerFactory.getLogger(AcceptConnectionHandler.class);

    @Override
    public void completed(final AsynchronousSocketChannel socket, final AppContext context) {
        try {
            context.nexServer().listen();
        } catch (Exception ex) {
            logger.error("listen call failed for next connection: {}", ex.getMessage());
        }

        handle(socket, context);
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

    private void handle(final AsynchronousSocketChannel socket, final AppContext context) {
        // TODO: Complete implementation!
    }
}
