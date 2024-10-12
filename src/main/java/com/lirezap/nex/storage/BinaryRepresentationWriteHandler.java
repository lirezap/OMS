package com.lirezap.nex.storage;

import com.lirezap.nex.binary.BinaryRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.CompletionHandler;

/**
 * A handler that is called after the content of a {@link BinaryRepresentation} has been written into a file.
 *
 * @author Alireza Pourtaghi
 */
public final class BinaryRepresentationWriteHandler implements CompletionHandler<Integer, BinaryRepresentation<?>> {
    private static final Logger logger = LoggerFactory.getLogger(BinaryRepresentationWriteHandler.class);

    @Override
    public void completed(final Integer bytesWritten, final BinaryRepresentation<?> representation) {
        // TODO: Does we need to check written bytes count?
        representation.close();
    }

    @Override
    public void failed(final Throwable ex, final BinaryRepresentation<?> representation) {
        representation.close();
        logger.error("error while writing bytes: {}!", ex.getMessage(), ex);
    }
}
