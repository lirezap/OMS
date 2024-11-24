package com.openex.oms.storage;

import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A handler that is called after the content of a {@link ByteBuffer} has been written into a file.
 *
 * @author Alireza Pourtaghi
 */
public final class ByteBufferWriteHandler implements CompletionHandler<Integer, ByteBuffer> {
    private static final Logger logger = getLogger(ByteBufferWriteHandler.class);

    private final AsynchronousFileChannel file;
    private final long localPosition;

    public ByteBufferWriteHandler(final AsynchronousFileChannel file, final long localPosition) {
        this.file = file;
        this.localPosition = localPosition;
    }

    @Override
    public void completed(final Integer bytesWritten, final ByteBuffer buffer) {
        if (buffer.remaining() > 0) {
            file.write(buffer, localPosition + bytesWritten, buffer, this);
        }
    }

    @Override
    public void failed(final Throwable ex, final ByteBuffer buffer) {
        logger.error("error while writing bytes: {}!", ex.getMessage(), ex);
    }
}
