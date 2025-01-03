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
package software.openex.oms.binary.order;

import java.lang.foreign.MemorySegment;

import static java.lang.Long.compare;
import static software.openex.oms.binary.BinaryRepresentable.*;

/**
 * @author Alireza Pourtaghi
 */
public final class CancelOrder extends Order {

    public CancelOrder(final long id, final long ts, final String symbol, final String quantity, final String price) {
        super(id, ts, symbol, quantity, price);
    }

    @Override
    public int representationId() {
        return 104;
    }

    @Override
    public int compareTo(final Order o) {
        return compare(getTs(), o.getTs());
    }

    public static CancelOrder decode(final MemorySegment segment) {
        long position = RHS;

        var id = segment.get(LONG, position);
        position += LONG.byteSize();

        var ts = segment.get(LONG, position);
        position += LONG.byteSize();

        var symbolSize = segment.get(INT, position);
        position += INT.byteSize();

        var symbol = segment.getString(position);
        position += symbolSize;

        var quantitySize = segment.get(INT, position);
        position += INT.byteSize();

        var quantity = segment.getString(position);
        position += quantitySize;

        var priceSize = segment.get(INT, position);
        position += INT.byteSize();

        var price = segment.getString(position);

        return new CancelOrder(id, ts, symbol, quantity, price);
    }
}
