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
package software.openex.oms.net;

import java.io.Closeable;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

import static java.lang.foreign.Arena.ofShared;
import static java.lang.foreign.MemorySegment.copy;

/**
 * An open connection abstraction and related fields.
 *
 * @author Alireza Pourtaghi
 */
public final class Connection implements Closeable {
    private final AsynchronousSocketChannel socket;
    private final Arena arena;
    private final MemorySegment segment;
    private final ByteBuffer buffer;

    public Connection(final AsynchronousSocketChannel socket, final int size) {
        this.socket = socket;
        this.arena = ofShared();
        this.segment = this.arena.allocate(size);
        this.buffer = this.segment.asByteBuffer();
    }

    public static Connection extendSegment(final Connection connection, final int extendSize) {
        final var size = connection.segment().byteSize();
        final var newSize = size + extendSize;
        final var newConnection = new Connection(connection.socket(), (int) newSize);

        copy(connection.segment(), 0, newConnection.segment(), 0, size);
        newConnection.buffer().position(connection.buffer().capacity());
        connection.arena().close();

        return newConnection;
    }

    public MemorySegment copyMessage() {
        final var copySegment = arena.allocate(buffer().limit());
        copy(segment(), 0, copySegment, 0, copySegment.byteSize());

        return copySegment;
    }

    public AsynchronousSocketChannel socket() {
        return socket;
    }

    public Arena arena() {
        return arena;
    }

    public MemorySegment segment() {
        return segment;
    }

    public ByteBuffer buffer() {
        return buffer;
    }

    @Override
    public void close() throws IOException {
        if (socket.isOpen()) socket.close();
        arena.close();
    }
}
