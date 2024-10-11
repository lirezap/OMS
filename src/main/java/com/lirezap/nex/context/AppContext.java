package com.lirezap.nex.context;

import com.lirezap.nex.storage.AsynchronousAppendOnlyFile;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * Singleton application context that contains main components.
 *
 * @author Alireza Pourtaghi
 */
public final class AppContext {
    private static final Logger logger = LoggerFactory.getLogger(AppContext.class);

    private static final AtomicBoolean initialized = new AtomicBoolean(FALSE);
    private static AppContext context;

    private final Configuration configuration;
    private final Vertx vertx;
    private final HTTPServer httpServer;
    private final AsynchronousAppendOnlyFile httpRequestsLogFile;

    private AppContext() {
        this.configuration = new Configuration();
        this.vertx = vertx(this.configuration);
        this.httpServer = new HTTPServer(this.configuration, this.vertx);
        this.httpRequestsLogFile = httpRequestsLogFile(this.configuration);
    }

    /**
     * Thread safe application context initializer.
     */
    public static void initialize() {
        if (initialized.getAcquire()) {
            // Do nothing; already initialized application context.
        } else {
            // Initializing application context.
            initialized.set(TRUE);
            context = new AppContext();
        }
    }

    /**
     * Returns current instance of application context.
     * This method is not safe if initialized method is not called and may return null.
     *
     * @return current initialized application context
     */
    public static AppContext context() {
        return context;
    }

    /**
     * Returns current instance of application context.
     * This method is safe if initialized method is not called, thereby always has a return value.
     *
     * @return current initialized application context
     */
    public static AppContext contextSafe() {
        initialize();
        return context;
    }

    public Configuration config() {
        return configuration;
    }

    public Vertx vertx() {
        return vertx;
    }

    public HTTPServer httpServer() {
        return httpServer;
    }

    public Optional<AsynchronousAppendOnlyFile> httpRequestsLogFile() {
        return Optional.ofNullable(httpRequestsLogFile);
    }

    private static Vertx vertx(final Configuration configuration) {
        final var metricsServerOptions = new HttpServerOptions()
                .setHost(configuration.loadString("metrics.server.host"))
                .setPort(configuration.loadInt("metrics.server.port"));

        final var prometheusOptions = new VertxPrometheusOptions()
                .setEnabled(configuration.loadBoolean("metrics.server.enabled"))
                .setStartEmbeddedServer(TRUE)
                .setEmbeddedServerOptions(metricsServerOptions)
                .setEmbeddedServerEndpoint("/metrics");

        final var micrometerOptions = new MicrometerMetricsOptions()
                .setEnabled(configuration.loadBoolean("metrics.server.enabled"))
                .setPrometheusOptions(prometheusOptions)
                .setJvmMetricsEnabled(TRUE);

        final var options = new VertxOptions()
                .setPreferNativeTransport(TRUE)
                .setMetricsOptions(micrometerOptions);

        return Vertx.vertx(options);
    }

    private static AsynchronousAppendOnlyFile httpRequestsLogFile(final Configuration configuration) {
        if (!configuration.loadBoolean("http.server.request_logging_enabled"))
            return null;

        try {
            return new AsynchronousAppendOnlyFile(
                    Path.of(configuration.loadString("http.server.request_logging_file_path")),
                    configuration.loadInt("http.server.request_logging_parallelism"),
                    CREATE, WRITE);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
