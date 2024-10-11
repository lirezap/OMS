package com.lirezap.nex.context;

import com.lirezap.nex.http.handlers.JsonBodyResponderHandler;
import com.lirezap.nex.http.handlers.LiveNessHandler;
import com.lirezap.nex.http.handlers.NoContentResponderHandler;
import com.lirezap.nex.http.handlers.RequestLoggingFormatter;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static com.lirezap.nex.context.AppContext.context;
import static io.netty.handler.codec.http.HttpResponseStatus.GATEWAY_TIMEOUT;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

/**
 * HTTP server component.
 *
 * @author Alireza Pourtaghi
 */
public final class HTTPServer {
    private static final Logger logger = LoggerFactory.getLogger(HTTPServer.class);

    private final Vertx vertx;
    private final HttpServer httpServer;
    private final Router router;

    HTTPServer(final Configuration configuration, final Vertx vertx) {
        this.vertx = vertx;

        final var options = new HttpServerOptions()
                .setHost(configuration.loadString("http.server.host"))
                .setPort(configuration.loadInt("http.server.port"))
                .setIdleTimeout((int) configuration.loadDuration("http.server.idle_timeout").toSeconds()).setIdleTimeoutUnit(TimeUnit.SECONDS)
                .setCompressionSupported(configuration.loadBoolean("http.server.use_compression"))
                .setCompressionLevel(configuration.loadInt("http.server.compression_level"))
                .setReusePort(TRUE)
                .setTcpFastOpen(TRUE)
                .setTcpQuickAck(TRUE)
                .setTcpCork(TRUE);

        this.httpServer = vertx.createHttpServer(options);
        this.router = Router.router(vertx);

        addShutdownHook();
    }

    public void start() {
        setupRoutes();

        final var cause = httpServer.requestHandler(router).listen().cause();
        if (cause != null) {
            throw new RuntimeException(cause.getMessage());
        }

        logger.info("Successfully started HTTP server and listening on {}:{} with native transport {}",
                context().config().loadString("http.server.host"),
                context().config().loadString("http.server.port"),
                vertx.isNativeTransportEnabled() ? "enabled" : "not enabled");
    }

    private void setupRoutes() {
        setupBaseHandlers();

        final var jsonBodyResponderHandler = new JsonBodyResponderHandler();
        final var noContentResponderHandler = new NoContentResponderHandler();
        final var bodyHandler = BodyHandler.create(FALSE);

        router.get("/ready").handler(new LiveNessHandler());
        router.get("/health*").handler(healthChecksHandler());
    }

    private void setupBaseHandlers() {
        router.route().handler(ResponseTimeHandler.create());
        router.route().handler(TimeoutHandler.create(context().config().loadDuration("http.server.request_timeout").toMillis(), GATEWAY_TIMEOUT.code()));

        if (context().config().loadBoolean("http.server.request_logging_enabled")) {
            router.route().handler(LoggerHandler.create(LoggerFormat.CUSTOM).customFormatter(new RequestLoggingFormatter()));
        }

        if (context().config().loadBoolean("http.server.hsts_enabled")) {
            router.route().handler(HSTSHandler.create(TRUE));
        }

        if (context().config().loadBoolean("http.server.csp_enabled")) {
            router.route().handler(CSPHandler.create());
        }

        if (context().config().loadBoolean("http.server.cors_enabled")) {
            router.route().handler(CorsHandler.create().addOrigin(context().config().loadString("http.server.cors_origin")));
        }

        if (context().config().loadBoolean("http.server.x_frame_enabled")) {
            router.route().handler(XFrameHandler.create(context().config().loadString("http.server.x_frame_action")));
        }
    }

    private HealthCheckHandler healthChecksHandler() {
        return HealthCheckHandler.create(vertx);
    }

    /**
     * Adds a shutdown hook for current component.
     */
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Closing HTTP server ...");

            final var cause = vertx.close().cause();
            if (cause != null) {
                logger.error("{}", cause.getMessage());
            }
        }));
    }
}
