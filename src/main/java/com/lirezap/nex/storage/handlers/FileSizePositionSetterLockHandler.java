package com.lirezap.nex.storage.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileLock;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A handler that sets the position of the next byte to be written into file as file's current size.
 *
 * @author Alireza Pourtaghi
 */
public final class FileSizePositionSetterLockHandler implements CompletionHandler<FileLock, AsynchronousFileChannel> {
    private static final Logger logger = LoggerFactory.getLogger(FileSizePositionSetterLockHandler.class);

    private final Semaphore guard;
    private final AtomicLong position;

    public FileSizePositionSetterLockHandler(final Semaphore guard, final AtomicLong position) {
        this.guard = guard;
        this.position = position;
    }

    @Override
    public void completed(final FileLock lock, final AsynchronousFileChannel file) {
        try {
            position.addAndGet(file.size());
            lock.release();
            guard.release();
        } catch (Exception ex) {
            logger.error("could not release file lock: {}!", ex.getMessage(), ex);
            guard.release();

            System.exit(-1);
        }
    }

    @Override
    public void failed(final Throwable ex, final AsynchronousFileChannel file) {
        logger.error("could not set position on file: {}!", ex.getMessage(), ex);
        guard.release();

        System.exit(-1);
    }
}
