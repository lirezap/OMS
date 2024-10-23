package com.lirezap.nex.storage;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Encapsulated file and its assigned thread.
 *
 * @author Alireza Pourtaghi
 */
public final class FileWriter {
    private final ExecutorService executor;
    private final AsynchronousFileChannel file;

    public FileWriter(final Path path, final OpenOption... options) throws IOException {
        this.executor = Executors.newSingleThreadExecutor();
        this.file = AsynchronousFileChannel.open(path, Set.of(options), executor);
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public AsynchronousFileChannel getFile() {
        return file;
    }
}
