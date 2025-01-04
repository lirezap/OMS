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
package software.openex.oms.binary.order.book;

import software.openex.oms.binary.BinaryRepresentable;
import software.openex.oms.binary.BinaryRepresentation;
import software.openex.oms.binary.order.BuyOrder;
import software.openex.oms.binary.order.SellOrder;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alireza Pourtaghi
 */
public final class OrderBookBinaryRepresentation extends BinaryRepresentation<OrderBook> {
    private final OrderBook orderBook;

    public OrderBookBinaryRepresentation(final OrderBook orderBook) {
        super(orderBook.size());
        this.orderBook = orderBook;
    }

    public OrderBookBinaryRepresentation(final Arena arena, final OrderBook orderBook) {
        super(arena, orderBook.size());
        this.orderBook = orderBook;
    }

    @Override
    protected int id() {
        return 106;
    }

    @Override
    protected void encodeRecord() {
        try {
            putBinaryRepresentations(orderBook.getBids());
            putBinaryRepresentations(orderBook.getAsks());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static List<BuyOrder> bids(final MemorySegment segment) {
        long position = RHS;

        final var bidsSize = segment.get(INT, position);
        position += INT.byteSize();

        final var bids = new ArrayList<BuyOrder>(bidsSize);
        for (int i = 1; i <= bidsSize; i++) {
            final var size = RHS + BinaryRepresentable.size(segment.asSlice(position));
            bids.add(BuyOrder.decode(segment.asSlice(position, size)));
            position += size;
        }

        return bids;
    }

    public static List<SellOrder> asks(final MemorySegment segment) {
        long position = RHS;

        final var bidsSize = segment.get(INT, position);
        position += INT.byteSize();
        for (int i = 1; i <= bidsSize; i++) {
            final var size = RHS + BinaryRepresentable.size(segment.asSlice(position));
            position += size;
        }

        final var asksSize = segment.get(INT, position);
        position += INT.byteSize();

        final var asks = new ArrayList<SellOrder>(asksSize);
        for (int i = 1; i <= asksSize; i++) {
            final var size = RHS + BinaryRepresentable.size(segment.asSlice(position));
            asks.add(SellOrder.decode(segment.asSlice(position, size)));
            position += size;
        }

        return asks;
    }
}
