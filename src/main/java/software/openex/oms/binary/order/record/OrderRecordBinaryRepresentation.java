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
package software.openex.oms.binary.order.record;

import software.openex.oms.binary.BinaryRepresentation;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * @author Alireza Pourtaghi
 */
public final class OrderRecordBinaryRepresentation extends BinaryRepresentation<OrderRecord> {
    private final OrderRecord orderRecord;

    public OrderRecordBinaryRepresentation(final OrderRecord orderRecord) {
        super(orderRecord.size());
        this.orderRecord = orderRecord;
    }

    public OrderRecordBinaryRepresentation(final Arena arena, final OrderRecord orderRecord) {
        super(arena, orderRecord.size());
        this.orderRecord = orderRecord;
    }

    @Override
    protected int id() {
        return 120;
    }

    @Override
    protected void encodeRecord() {
        try {
            putLong(orderRecord.getId());
            putString(orderRecord.getSymbol());
            putString(orderRecord.getSide());
            putString(orderRecord.getType());
            putString(orderRecord.getQuantity());
            putString(orderRecord.getPrice());
            putString(orderRecord.getRemaining());
            putString(orderRecord.getState());
            putString(orderRecord.getMetadata());
            putLong(orderRecord.getTs());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static OrderRecord decode(final MemorySegment segment) {
        long position = RHS;

        final var id = segment.get(LONG, position);
        position += LONG.byteSize();

        final var symbolSize = segment.get(INT, position);
        position += INT.byteSize();

        final var symbol = segment.getString(position);
        position += symbolSize;

        final var sideSize = segment.get(INT, position);
        position += INT.byteSize();

        final var side = segment.getString(position);
        position += sideSize;

        final var typeSize = segment.get(INT, position);
        position += INT.byteSize();

        final var type = segment.getString(position);
        position += typeSize;

        final var quantitySize = segment.get(INT, position);
        position += INT.byteSize();

        final var quantity = segment.getString(position);
        position += quantitySize;

        final var priceSize = segment.get(INT, position);
        position += INT.byteSize();

        final var price = segment.getString(position);
        position += priceSize;

        final var remainingSize = segment.get(INT, position);
        position += INT.byteSize();

        final var remaining = segment.getString(position);
        position += remainingSize;

        final var stateSize = segment.get(INT, position);
        position += INT.byteSize();

        final var state = segment.getString(position);
        position += stateSize;

        final var metadataSize = segment.get(INT, position);
        position += INT.byteSize();

        final var metadata = segment.getString(position);
        position += metadataSize;

        final var ts = segment.get(LONG, position);

        return new OrderRecord(id, symbol, side, type, quantity, price, remaining, state, metadata, ts);
    }
}
