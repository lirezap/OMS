package com.lirezap.nex.storage;

import com.lirezap.nex.binary.BinaryRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

/**
 * A handler that is called after the content of a {@link BinaryRepresentation} has been written into a file.
 *
 * @author Alireza Pourtaghi
 */
public final class BinaryRepresentationWriteHandler implements CompletionHandler<Integer, BinaryRepresentation<?>> {
    private static final Logger logger = LoggerFactory.getLogger(BinaryRepresentationWriteHandler.class);

    final FileWriter writer;
    final ByteBuffer buffer;
    long localPosition;

    public BinaryRepresentationWriteHandler(final FileWriter writer, final ByteBuffer buffer, final long localPosition) {
        this.writer = writer;
        this.buffer = buffer;
        this.localPosition = localPosition;
    }

    @Override
    public void completed(final Integer bytesWritten, final BinaryRepresentation<?> representation) {
        if (buffer.remaining() > 0) {
            writer.getExecutor().submit(() -> {
                writer.getFile().write(
                        buffer,
                        localPosition + bytesWritten,
                        representation,
                        new BinaryRepresentationWriteHandler(writer, buffer, localPosition + bytesWritten));
            });
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
