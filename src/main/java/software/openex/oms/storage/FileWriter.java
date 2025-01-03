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

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

import static java.nio.channels.AsynchronousFileChannel.open;
import static java.util.Set.of;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * Encapsulated file and its assigned thread.
 *
 * @author Alireza Pourtaghi
 */
public final class FileWriter {
    private final ExecutorService executor;
    private final AsynchronousFileChannel file;

    public FileWriter(final Path path, final OpenOption... options) throws IOException {
        this.executor = newSingleThreadExecutor();
        this.file = open(path, of(options), executor);
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public AsynchronousFileChannel getFile() {
        return file;
    }
}
