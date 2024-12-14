/*
 * ISC License
 *
 * Copyright (c) 2024, Alireza Pourtaghi <lirezap@protonmail.com>
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
package software.openex.oms.binary.file;

import software.openex.oms.binary.BinaryRepresentation;

import java.lang.foreign.Arena;

/**
 * @author Alireza Pourtaghi
 */
public final class FileHeaderBinaryRepresentation extends BinaryRepresentation<FileHeader> {
    private final FileHeader fileHeader;

    public FileHeaderBinaryRepresentation(final FileHeader fileHeader) {
        super(fileHeader.size());
        this.fileHeader = fileHeader;
    }

    public FileHeaderBinaryRepresentation(final Arena arena, final FileHeader fileHeader) {
        super(arena, fileHeader.size());
        this.fileHeader = fileHeader;
    }

    @Override
    protected int id() {
        return 1;
    }

    @Override
    protected void encodeRecord() {
        try {
            putLong(fileHeader.getDurabilitySize());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public long durabilitySize() {
        return segment().get(LONG, RHS);
    }

    public void incrementDurabilitySize(final long inc) {
        LONG.varHandle().set(segment(), RHS, durabilitySize() + inc);
    }
}
