package com.lirezap.nex.net;

import java.io.Closeable;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

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
        this.arena = Arena.ofShared();
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
        socket.close();
        arena.close();
    }
}
