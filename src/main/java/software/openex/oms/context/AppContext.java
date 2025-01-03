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
package software.openex.oms.context;

import org.slf4j.Logger;
import software.openex.oms.net.Dispatcher;
import software.openex.oms.net.SocketServer;
import software.openex.oms.storage.AsynchronousAppendOnlyFile;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Runtime.getRuntime;
import static java.nio.file.Path.of;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Singleton application context that contains main components.
 *
 * @author Alireza Pourtaghi
 */
public final class AppContext {
    private static final Logger logger = getLogger(AppContext.class);

    private static final AtomicBoolean initialized = new AtomicBoolean(FALSE);
    private static AppContext context;

    private final Configuration configuration;
    private final DatabaseMigrator databaseMigrator;
    private final DataSource dataSource;
    private final DataBase dataBase;
    private final Compression compression;
    private final AsynchronousAppendOnlyFile messagesLogFile;
    private final MatchingEngines matchingEngines;
    private final Executors executors;
    private final Dispatcher dispatcher;
    private final SocketServer socketServer;

    private AppContext() {
        addShutdownHook();

        this.configuration = new Configuration();
        this.databaseMigrator = new DatabaseMigrator(this.configuration);
        this.dataSource = new DataSource(this.configuration);
        this.dataBase = new DataBase(this.configuration, this.dataSource);
        this.compression = new Compression(this.configuration);
        this.messagesLogFile = messagesLogFile(this.configuration);
        this.matchingEngines = new MatchingEngines(this.configuration);
        this.executors = new Executors(this.configuration);
        this.dispatcher = new Dispatcher();
        this.socketServer = socketServer(this.configuration);
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

    public DatabaseMigrator databaseMigrator() {
        return databaseMigrator;
    }

    public DataSource dataSource() {
        return dataSource;
    }

    public DataBase dataBase() {
        return dataBase;
    }

    public Compression compression() {
        return compression;
    }

    public Optional<AsynchronousAppendOnlyFile> messagesLogFile() {
        return ofNullable(messagesLogFile);
    }

    public MatchingEngines matchingEngines() {
        return matchingEngines;
    }

    public Executors executors() {
        return executors;
    }

    public Dispatcher dispatcher() {
        return dispatcher;
    }

    public SocketServer socketServer() {
        return socketServer;
    }

    private static AsynchronousAppendOnlyFile messagesLogFile(final Configuration configuration) {
        if (!configuration.loadBoolean("logging.messages.enabled")) return null;

        try {
            return new AsynchronousAppendOnlyFile(
                    of(configuration.loadString("logging.messages.file_path")),
                    configuration.loadInt("logging.messages.parallelism"),
                    CREATE, WRITE);

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static SocketServer socketServer(final Configuration configuration) {
        try {
            return new SocketServer(configuration);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Adds a shutdown hook for context.
     */
    private void addShutdownHook() {
        getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (socketServer != null) socketServer.close();
                if (executors != null) executors.close();
                if (matchingEngines != null) matchingEngines.close();
                if (messagesLogFile != null) messagesLogFile.close();
                if (compression != null) compression.close();
                if (dataSource != null) dataSource.close();
            } catch (Exception ex) {
                logger.error("{}", ex.getMessage());
            }
        }));
    }
}
