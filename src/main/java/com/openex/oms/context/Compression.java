package com.openex.oms.context;

import com.openex.oms.lib.lz4.LZ4;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;

import static java.nio.file.Path.of;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Compression algorithms component.
 *
 * @author Alireza Pourtaghi
 */
public final class Compression implements Closeable {
    private static final Logger logger = getLogger(Compression.class);

    private final LZ4 lz4;

    Compression(final Configuration configuration) {
        this.lz4 = new LZ4(of(configuration.loadString("libraries.native.lz4.path")));
    }

    public LZ4 lz4() {
        return lz4;
    }

    @Override
    public void close() throws IOException {
        logger.info("Deallocating LZ4 library ...");

        try {
            lz4.close();
        } catch (Exception ex) {
            logger.error("{}", ex.getMessage());
        }
    }
}
