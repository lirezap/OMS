package com.lirezap.nex.context;

import com.lirezap.nex.lib.lz4.LZ4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Compression algorithms component.
 *
 * @author Alireza Pourtaghi
 */
public final class Compression {
    private static final Logger logger = LoggerFactory.getLogger(Compression.class);

    private final LZ4 lz4;

    Compression(final Configuration configuration) {
        this.lz4 = new LZ4(Path.of(configuration.loadString("libraries.native.lz4.path")));
    }

    public LZ4 lz4() {
        return lz4;
    }

    /**
     * Adds a shutdown hook for current component.
     */
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Deallocating LZ4 library ...");

            try {
                lz4.close();
            } catch (Exception ex) {
                logger.error("{}", ex.getMessage());
            }
        }));
    }
}
