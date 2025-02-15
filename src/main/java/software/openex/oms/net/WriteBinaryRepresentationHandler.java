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
package software.openex.oms.net;

import org.slf4j.Logger;
import software.openex.oms.binary.BinaryRepresentation;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Completion handler that writes binary representation bytes into a channel.
 *
 * @author Alireza Pourtaghi
 */
public final class WriteBinaryRepresentationHandler implements CompletionHandler<Integer, ByteBuffer> {
    private static final Logger logger = getLogger(WriteBinaryRepresentationHandler.class);
    private static final ReadHandler readHandler = new ReadHandler();

    private final Connection connection;
    private final BinaryRepresentation<?> binaryRepresentation;

    public WriteBinaryRepresentationHandler(final Connection connection,
                                            final BinaryRepresentation<?> binaryRepresentation) {

        this.connection = connection;
        this.binaryRepresentation = binaryRepresentation;
    }

    @Override
    public void completed(final Integer bytes, final ByteBuffer buffer) {
        if (buffer.remaining() > 0) {
            write(buffer);
        } else {
            binaryRepresentation.close();
            connection.buffer().clear();
            read(connection);
        }
    }

    @Override
    public void failed(final Throwable th, final ByteBuffer buffer) {
        logger.error("write error operation failed: {}", th.getMessage());

        binaryRepresentation.close();
        connection.buffer().clear();
        read(connection);
    }

    private void write(final ByteBuffer buffer) {
        try {
            connection.socket().write(buffer, buffer, this);
        } catch (Exception ex) {
            logger.error("write call failed: {}", ex.getMessage());

            binaryRepresentation.close();
            connection.buffer().clear();
            read(connection);
        }
    }

    private void read(final Connection connection) {
        try {
            connection.socket().read(connection.buffer(), connection, readHandler);
        } catch (Exception ex) {
            logger.error("read call failed: {}", ex.getMessage());
            close(connection);
        }
    }

    private void close(final Connection connection) {
        try {
            connection.close();
        } catch (Exception ex) {
            logger.error("error while closing connection: {}", ex.getMessage());
        }
    }
}
