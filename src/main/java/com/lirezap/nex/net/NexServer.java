package com.lirezap.nex.net;

import com.lirezap.nex.context.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;

import static com.lirezap.nex.context.AppContext.context;
import static java.net.StandardSocketOptions.SO_RCVBUF;
import static java.net.StandardSocketOptions.SO_REUSEADDR;

/**
 * An asynchronous server socket implementation.
 *
 * @author Alireza Pourtaghi
 */
public final class NexServer {
    private static final Logger logger = LoggerFactory.getLogger(NexServer.class);

    private final AsynchronousServerSocketChannel server;
    private final AcceptConnectionHandler acceptConnectionHandler;

    public NexServer(final Configuration configuration) throws IOException {
        logger.info("Binding server socket {}:{}", configuration.loadString("server.host"), configuration.loadInt("server.port"));
        this.server = bind(setOptions(open(), configuration), configuration);
        this.acceptConnectionHandler = new AcceptConnectionHandler();

        addShutdownHook();
    }

    public void listen() {
        server.accept(context(), acceptConnectionHandler);
        logger.info("Successfully started server socket and listening ...");
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

    private static AsynchronousServerSocketChannel bind(final AsynchronousServerSocketChannel server,
                                                        final Configuration configuration) throws IOException {

        server.bind(address(configuration));
        return server;
    }

    private static InetSocketAddress address(final Configuration configuration) {
        return new InetSocketAddress(configuration.loadString("server.host"), configuration.loadInt("server.port"));
    }

    /**
     * Adds a shutdown hook for current component.
     */
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down the socket server ...");

            try {
                server.close();
            } catch (Exception ex) {
                logger.error("{}", ex.getMessage());
            }
        }));
    }
}
