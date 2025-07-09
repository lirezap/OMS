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
import software.openex.oms.binary.base.ErrorMessageBinaryRepresentation;

import java.nio.channels.CompletionHandler;

import static org.slf4j.LoggerFactory.getLogger;
import static software.openex.oms.binary.BinaryRepresentable.RHS;
import static software.openex.oms.binary.BinaryRepresentable.size;
import static software.openex.oms.context.AppContext.context;
import static software.openex.oms.net.Connection.extendSegment;
import static software.openex.oms.net.ErrorMessages.MESSAGE_FORMAT_NOT_VALID;
import static software.openex.oms.net.ErrorMessages.MESSAGE_LENGTH_TOO_BIG;

/**
 * Stateless completion handler that reads bytes from a channel.
 *
 * @author Alireza Pourtaghi
 */
public final class ReadHandler implements CompletionHandler<Integer, Connection> {
    private static final Logger logger = getLogger(ReadHandler.class);
    private static final int EOS = -1;

    @Override
    public void completed(final Integer bytes, final Connection connection) {
        switch (bytes) {
            case EOS -> handleEOS(connection);
            case 0 -> handleZeroBytesReceived(connection);

            default -> handleMessage(connection);
        }
    }

    @Override
    public void failed(final Throwable th, final Connection connection) {
        logger.error("read operation failed: {}", th.getMessage());
        close(connection);
    }

    private void handleEOS(final Connection connection) {
        logger.trace("end of stream detected; closing connection ...");
        close(connection);
    }

    private void handleZeroBytesReceived(final Connection connection) {
        if (connection.buffer().position() < connection.buffer().limit()) {
            read(connection);
        } else {
            logger.warn("full buffer loop detected; closing connection ...");
            close(connection);
        }
    }

    private void handleMessage(final Connection connection) {
        connection.buffer().flip();
        logger.trace("Buffer: {}", connection.buffer());

        final var limit = connection.buffer().limit();
        if (limit <= RHS) {
            write(connection, MESSAGE_FORMAT_NOT_VALID);
            return;
        }

        if (limit > context().config().loadInt("server.max_message_size")) {
            write(connection, MESSAGE_LENGTH_TOO_BIG);
            return;
        }

        final var capacity = connection.buffer().capacity();
        final var requiredReadSize = RHS + size(connection.segment());
        if (limit < capacity) {
            if (limit == requiredReadSize) {
                context().dispatcher().dispatch(connection);
                return;
            }

            if (limit < requiredReadSize) {
                connection.buffer().position(limit);
                connection.buffer().limit(capacity);
                read(connection);
                return;
            }

            write(connection, MESSAGE_FORMAT_NOT_VALID);
            return;
        }

        if (limit == capacity) {
            if (limit == requiredReadSize) {
                context().dispatcher().dispatch(connection);
                return;
            }

            // Extended buffer size remains for connection.
            final var readBufferSize = context().config().loadInt("server.read_buffer_size");
            final var extendedSegmentConnection = extendSegment(connection, readBufferSize);
            read(extendedSegmentConnection);
        }
    }

    private void write(final Connection connection, final ErrorMessageBinaryRepresentation message) {
        try {
            final var buffer = message.buffer();
            connection.socket().write(buffer, buffer, new WriteErrorHandler(connection));
        } catch (Exception ex) {
            logger.error("write call failed: {}", ex.getMessage());

            connection.buffer().clear();
            read(connection);
        }
    }

    private void read(final Connection connection) {
        try {
            connection.socket().read(connection.buffer(), connection, this);
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
