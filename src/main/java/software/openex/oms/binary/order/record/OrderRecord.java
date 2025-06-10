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

import java.lang.foreign.MemorySegment;

import static software.openex.oms.binary.BinaryRepresentable.*;

/**
 * @author Alireza Pourtaghi
 */
public final class OrderRecord {
    private final long id;
    private final String symbol;
    private final String side;
    private final String type;
    private final String quantity;
    private final String price;
    private final String remaining;
    private final String state;
    private final String metadata;
    private final long ts;

    public OrderRecord(final long id, final String symbol, final String side, final String type, final String quantity,
                       final String price, final String remaining, final String state, final String metadata,
                       final long ts) {

        this.id = id;
        this.symbol = symbol == null ? "" : symbol;
        this.side = side == null ? "" : side;
        this.type = type == null ? "" : type;
        this.quantity = quantity == null ? "" : quantity;
        this.price = price == null ? "" : price;
        this.remaining = remaining == null ? "" : remaining;
        this.state = state == null ? "" : state;
        this.metadata = metadata == null ? "" : metadata;
        this.ts = ts;
    }

    public int size() {
        return 8 + representationSize(symbol) + representationSize(side) + representationSize(type) +
                representationSize(quantity) + representationSize(price) + representationSize(remaining) +
                representationSize(state) + representationSize(metadata) + 8;
    }

    public long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSide() {
        return side;
    }

    public String getType() {
        return type;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getPrice() {
        return price;
    }

    public String getRemaining() {
        return remaining;
    }

    public String getState() {
        return state;
    }

    public String getMetadata() {
        return metadata;
    }

    public long getTs() {
        return ts;
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
