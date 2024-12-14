/*
 * ISC License
 *
 * Copyright (c) 2024, Alireza Pourtaghi <lirezap@protonmail.com>
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
import software.openex.oms.lib.lz4.LZ4;

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
