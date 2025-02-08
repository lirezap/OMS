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
package software.openex.oms.storage;

import org.slf4j.Logger;
import software.openex.oms.binary.order.LimitOrder;
import software.openex.oms.binary.order.LimitOrderBinaryRepresentation;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.foreign.Arena.ofConfined;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * High performance, paralleled, append-only and thread-safe file writer implementation.
 *
 * @author Alireza Pourtaghi
 */
public final class AsynchronousAppendOnlyFile implements Closeable {
    private static final Logger logger = getLogger(AsynchronousAppendOnlyFile.class);

    private final Semaphore guard;
    private final AtomicLong position;
    private final long parallelism;
    private final AtomicLong index;
    private final FileWriter[] writers;

    public AsynchronousAppendOnlyFile(final Path path, final int parallelism, final OpenOption... options)
            throws IOException {

        requireNonNull(path);
        this.guard = new Semaphore(1);
        this.position = new AtomicLong(0);
        this.parallelism = parallelism;
        this.index = new AtomicLong(0);
        this.writers = new FileWriter[parallelism];

        for (int i = 0; i < parallelism; i++) {
            logger.info("Setting up a new writer at index {} for {} ...", i, path);
            writers[i] = new FileWriter(path, options);
        }

        setPosition();
    }

    public void append(final ByteBuffer buffer) {
        final var writer = writers[(int) (index.getAndIncrement() % parallelism)];

        final var file = writer.getFile();
        final var localPosition = position.getAndAdd(buffer.limit());
        file.write(buffer, localPosition, buffer, new ByteBufferWriteHandler(file, localPosition));
    }

    public void append(final LimitOrder limitOrder) {
        final var writer = writers[(int) (index.getAndIncrement() % parallelism)];

        writer.getExecutor().submit(() -> {
            final var representation = new LimitOrderBinaryRepresentation(ofConfined(), limitOrder);
            representation.encodeV1();

            final var file = writer.getFile();
            final var buffer = representation.buffer();
            final var localPosition = position.getAndAdd(representation.segment().byteSize());
            file.write(buffer, localPosition, representation, new BinaryRepresentationWriteHandler(file, buffer, localPosition));
        });
    }

    private void setPosition() {
        if (guard.tryAcquire()) {
            // Only one thread can reach this block at a time for a specific file!
            final var file = writers[0].getFile();
            file.lock(file, new FileSizePositionSetterLockHandler(guard, position));
        }
    }

    @Override
    public void close() throws IOException {
        logger.info("Closing file writers ...");

        for (final var writer : writers) {
            writer.getFile().close();
            writer.getExecutor().close();
        }
    }
}
