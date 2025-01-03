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

import java.nio.channels.CompletionHandler;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Stateless completion handler that writes bytes into a channel.
 *
 * @author Alireza Pourtaghi
 */
public final class WriteHandler implements CompletionHandler<Integer, Connection> {
    private static final Logger logger = getLogger(WriteHandler.class);
    private static final ReadHandler readHandler = new ReadHandler();

    @Override
    public void completed(final Integer bytes, final Connection connection) {
        if (connection.buffer().remaining() > 0) {
            write(connection);
        } else {
            connection.buffer().clear();
            read(connection);
        }
    }

    @Override
    public void failed(final Throwable th, final Connection connection) {
        logger.error("write operation failed: {}", th.getMessage());

        connection.buffer().clear();
        read(connection);
    }

    private void write(final Connection connection) {
        try {
            connection.socket().write(connection.buffer(), connection, this);
        } catch (Exception ex) {
            logger.error("write call failed: {}", ex.getMessage());

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
