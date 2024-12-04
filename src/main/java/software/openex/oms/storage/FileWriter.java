package software.openex.oms.storage;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

import static java.nio.channels.AsynchronousFileChannel.open;
import static java.util.Set.of;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * Encapsulated file and its assigned thread.
 *
 * @author Alireza Pourtaghi
 */
public final class FileWriter {
    private final ExecutorService executor;
    private final AsynchronousFileChannel file;

    public FileWriter(final Path path, final OpenOption... options) throws IOException {
        this.executor = newSingleThreadExecutor();
        this.file = open(path, of(options), executor);
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public AsynchronousFileChannel getFile() {
        return file;
    }
}
