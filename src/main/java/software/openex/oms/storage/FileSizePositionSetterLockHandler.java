/*
 * ISC License
 *
 * Copyright (c) 2025, Alireza Pourtaghi <lirezap@protonmail.com>
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
package software.openex.oms.storage;

import org.slf4j.Logger;

import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileLock;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.System.exit;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A handler that sets the position of the next byte to be written into file as file's current size.
 *
 * @author Alireza Pourtaghi
 */
public final class FileSizePositionSetterLockHandler implements CompletionHandler<FileLock, AsynchronousFileChannel> {
    private static final Logger logger = getLogger(FileSizePositionSetterLockHandler.class);

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

            exit(-1);
        }
    }

    @Override
    public void failed(final Throwable ex, final AsynchronousFileChannel file) {
        logger.error("could not set position on file: {}!", ex.getMessage(), ex);
        guard.release();

        exit(-1);
    }
}
