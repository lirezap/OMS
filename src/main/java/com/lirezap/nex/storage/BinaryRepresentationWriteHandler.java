package com.lirezap.nex.storage;

import com.lirezap.nex.binary.BinaryRepresentation;
import org.slf4j.Logger;

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
