package com.lirezap.nex.context;

import com.lirezap.nex.net.Dispatcher;
import com.lirezap.nex.net.NexServer;
import com.lirezap.nex.storage.AsynchronousAppendOnlyFile;
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
import static java.util.Optional.ofNullable;

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
    private final Compression compression;
    private final AsynchronousAppendOnlyFile messagesLogFile;
    private final Executors executors;
    private final Dispatcher dispatcher;
    private final NexServer nexServer;

    private AppContext() {
        this.configuration = new Configuration();
        this.compression = new Compression(this.configuration);
        this.messagesLogFile = messagesLogFile(this.configuration);
        this.executors = new Executors(this.configuration);
        this.dispatcher = new Dispatcher();
        this.nexServer = nexServer(this.configuration);
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

    public Compression compression() {
        return compression;
    }

    public Optional<AsynchronousAppendOnlyFile> messagesLogFile() {
        return ofNullable(messagesLogFile);
    }

    public Executors executors() {
        return executors;
    }

    public Dispatcher dispatcher() {
        return dispatcher;
    }

    public NexServer nexServer() {
        return nexServer;
    }

    private static AsynchronousAppendOnlyFile messagesLogFile(final Configuration configuration) {
        if (!configuration.loadBoolean("logging.messages.enabled"))
            return null;

        try {
            return new AsynchronousAppendOnlyFile(
                    Path.of(configuration.loadString("logging.messages.file_path")),
                    configuration.loadInt("logging.messages.parallelism"),
                    CREATE, WRITE);

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static NexServer nexServer(final Configuration configuration) {
        try {
            return new NexServer(configuration);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
