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
package software.openex.oms.binary.order.book;

import software.openex.oms.binary.BinaryRepresentation;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * @author Alireza Pourtaghi
 */
public final class FetchOrderBookBinaryRepresentation extends BinaryRepresentation<FetchOrderBook> {
    private final FetchOrderBook fetchOrderBook;

    public FetchOrderBookBinaryRepresentation(final FetchOrderBook fetchOrderBook) {
        super(fetchOrderBook.size());
        this.fetchOrderBook = fetchOrderBook;
    }

    public FetchOrderBookBinaryRepresentation(final Arena arena, final FetchOrderBook fetchOrderBook) {
        super(arena, fetchOrderBook.size());
        this.fetchOrderBook = fetchOrderBook;
    }

    @Override
    protected int id() {
        return 105;
    }

    @Override
    protected void encodeRecord() {
        try {
            putString(fetchOrderBook.getSymbol());
            putInt(fetchOrderBook.getFetchSize());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static FetchOrderBook decode(final MemorySegment segment) {
        long position = RHS;

        var symbolSize = segment.get(INT, position);
        position += INT.byteSize();

        var symbol = segment.getString(position);
        position += symbolSize;

        var fetchSize = segment.get(INT, position);

        return new FetchOrderBook(symbol, fetchSize);
    }
}
