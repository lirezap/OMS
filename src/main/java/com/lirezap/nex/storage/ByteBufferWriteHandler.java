package com.lirezap.nex.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;

/**
 * A handler that is called after the content of a {@link ByteBuffer} has been written into a file.
 *
 * @author Alireza Pourtaghi
 */
public final class ByteBufferWriteHandler implements CompletionHandler<Integer, ByteBuffer> {
    private static final Logger logger = LoggerFactory.getLogger(ByteBufferWriteHandler.class);

    final AsynchronousFileChannel file;
    final ByteBuffer buffer;
    long localPosition;

    public ByteBufferWriteHandler(final AsynchronousFileChannel file, final ByteBuffer buffer, final long localPosition) {
        this.file = file;
        this.buffer = buffer;
        this.localPosition = localPosition;
    }

    @Override
    public void completed(final Integer bytesWritten, final ByteBuffer buffer) {
        if (buffer.remaining() > 0) {
            localPosition += bytesWritten;
            file.write(buffer, localPosition, buffer, this);
        }
    }

    @Override
    public void failed(final Throwable ex, final ByteBuffer buffer) {
        logger.error("error while writing bytes: {}!", ex.getMessage(), ex);
    }
}
