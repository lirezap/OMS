package com.lirezap.nex.storage;

import com.lirezap.nex.binary.BinaryRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;

/**
 * A handler that is called after the content of a {@link BinaryRepresentation} has been written into a file.
 *
 * @author Alireza Pourtaghi
 */
public final class BinaryRepresentationWriteHandler implements CompletionHandler<Integer, BinaryRepresentation<?>> {
    private static final Logger logger = LoggerFactory.getLogger(BinaryRepresentationWriteHandler.class);

    final AsynchronousFileChannel file;
    final ByteBuffer buffer;
    long localPosition;

    public BinaryRepresentationWriteHandler(final AsynchronousFileChannel file, final ByteBuffer buffer,
                                            final long localPosition) {

        this.file = file;
        this.buffer = buffer;
        this.localPosition = localPosition;
    }

    @Override
    public void completed(final Integer bytesWritten, final BinaryRepresentation<?> representation) {
        if (buffer.remaining() > 0) {
            localPosition += bytesWritten;
            file.write(buffer, localPosition, representation, this);
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
