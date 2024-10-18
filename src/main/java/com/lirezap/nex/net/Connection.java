package com.lirezap.nex.net;

import java.io.Closeable;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

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

    public AsynchronousSocketChannel socket() {
        return socket;
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
