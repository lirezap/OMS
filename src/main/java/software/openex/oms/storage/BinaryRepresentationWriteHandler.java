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
package software.openex.oms.storage;

import org.slf4j.Logger;
import software.openex.oms.binary.BinaryRepresentation;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A handler that is called after the content of a {@link BinaryRepresentation} has been written into a file.
 *
 * @author Alireza Pourtaghi
 */
public final class BinaryRepresentationWriteHandler implements CompletionHandler<Integer, BinaryRepresentation<?>> {
    private static final Logger logger = getLogger(BinaryRepresentationWriteHandler.class);

    private final AsynchronousFileChannel file;
    private final ByteBuffer buffer;
    private final long localPosition;

    public BinaryRepresentationWriteHandler(final AsynchronousFileChannel file, final ByteBuffer buffer,
                                            final long localPosition) {

        this.file = file;
        this.buffer = buffer;
        this.localPosition = localPosition;
    }

    @Override
    public void completed(final Integer bytesWritten, final BinaryRepresentation<?> representation) {
        if (buffer.remaining() > 0) {
            file.write(buffer, localPosition + bytesWritten, representation, this);
        } else {
            representation.close();
        }
    }

    @Override
    public void failed(final Throwable ex, final BinaryRepresentation<?> representation) {
        representation.close();
        logger.error("error while writing bytes: {}!", ex.getMessage(), ex);
    }
}
