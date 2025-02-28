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
import software.openex.oms.context.Configuration;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;

import static java.net.StandardSocketOptions.SO_RCVBUF;
import static java.net.StandardSocketOptions.SO_REUSEADDR;
import static org.slf4j.LoggerFactory.getLogger;
import static software.openex.oms.context.AppContext.context;

/**
 * An asynchronous socket server implementation.
 *
 * @author Alireza Pourtaghi
 */
public final class SocketServer implements Closeable {
    private static final Logger logger = getLogger(SocketServer.class);

    private final AsynchronousServerSocketChannel server;
    private final AcceptConnectionHandler acceptConnectionHandler;

    public SocketServer(final Configuration configuration) throws IOException {
        final var host = configuration.loadString("server.host");
        final var port = configuration.loadInt("server.port");

        logger.info("Binding server socket {}:{} ...", host, port);
        this.server = setOptions(open(), configuration).bind(address(configuration));
        this.acceptConnectionHandler = new AcceptConnectionHandler();
    }

    public void listen() {
        server.accept(context(), acceptConnectionHandler);
    }

    private static AsynchronousServerSocketChannel open() throws IOException {
        return AsynchronousServerSocketChannel.open();
    }

    private static AsynchronousServerSocketChannel setOptions(final AsynchronousServerSocketChannel server,
                                                              final Configuration configuration) throws IOException {

        server.setOption(SO_RCVBUF, configuration.loadInt("server.receive_buffer_size"));
        server.setOption(SO_REUSEADDR, configuration.loadBoolean("server.reuse_address"));
        return server;
    }

    private static InetSocketAddress address(final Configuration configuration) {
        final var host = configuration.loadString("server.host");
        final var port = configuration.loadInt("server.port");

        return new InetSocketAddress(host, port);
    }

    @Override
    public void close() throws IOException {
        logger.info("Shutting down the socket server ...");

        try {
            server.close();
        } catch (Exception ex) {
            logger.error("{}", ex.getMessage());
        }
    }
}
