package com.openex.oms.net;

import com.openex.oms.context.Configuration;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;

import static com.openex.oms.context.AppContext.context;
import static java.net.StandardSocketOptions.SO_RCVBUF;
import static java.net.StandardSocketOptions.SO_REUSEADDR;
import static org.slf4j.LoggerFactory.getLogger;

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
        logger.info("Binding server socket {}:{} ...", configuration.loadString("server.host"), configuration.loadInt("server.port"));
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
        return new InetSocketAddress(configuration.loadString("server.host"), configuration.loadInt("server.port"));
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
