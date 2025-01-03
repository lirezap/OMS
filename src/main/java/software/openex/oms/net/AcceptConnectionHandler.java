/*
 * ISC License
 *
 * Copyright (c) 2025, Alireza Pourtaghi <lirezap@protonmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package software.openex.oms.net;

import org.slf4j.Logger;
import software.openex.oms.context.AppContext;

import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ShutdownChannelGroupException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Stateless completion handler that accepts incoming socket connections.
 *
 * @author Alireza Pourtaghi
 */
public final class AcceptConnectionHandler implements CompletionHandler<AsynchronousSocketChannel, AppContext> {
    private static final Logger logger = getLogger(AcceptConnectionHandler.class);
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
            context.socketServer().listen();
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
