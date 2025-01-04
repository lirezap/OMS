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
package software.openex.oms.binary.trade;

import software.openex.oms.binary.BinaryRepresentation;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * @author Alireza Pourtaghi
 */
public final class TradeBinaryRepresentation extends BinaryRepresentation<Trade> {
    private final Trade trade;

    public TradeBinaryRepresentation(final Trade trade) {
        super(trade.size());
        this.trade = trade;
    }

    public TradeBinaryRepresentation(final Arena arena, final Trade trade) {
        super(arena, trade.size());
        this.trade = trade;
    }

    @Override
    protected int id() {
        return 103;
    }

    @Override
    protected void encodeRecord() {
        try {
            putLong(trade.getBuyOrderId());
            putLong(trade.getSellOrderId());
            putString(trade.getSymbol());
            putString(trade.getQuantity());
            putString(trade.getBuyPrice());
            putString(trade.getSellPrice());
            putString(trade.getMetadata());
            putLong(trade.getTs());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Trade decode(final MemorySegment segment) {
        long position = RHS;

        final var buyOrderId = segment.get(LONG, position);
        position += LONG.byteSize();

        final var sellOrderId = segment.get(LONG, position);
        position += LONG.byteSize();

        final var symbolSize = segment.get(INT, position);
        position += INT.byteSize();

        final var symbol = segment.getString(position);
        position += symbolSize;

        final var quantitySize = segment.get(INT, position);
        position += INT.byteSize();

        final var quantity = segment.getString(position);
        position += quantitySize;

        final var buyPriceSize = segment.get(INT, position);
        position += INT.byteSize();

        final var buyPrice = segment.getString(position);
        position += buyPriceSize;

        final var sellPriceSize = segment.get(INT, position);
        position += INT.byteSize();

        final var sellPrice = segment.getString(position);
        position += sellPriceSize;

        final var metadataSize = segment.get(INT, position);
        position += INT.byteSize();

        final var metadata = segment.getString(position);
        position += metadataSize;

        final var ts = segment.get(LONG, position);

        return new Trade(buyOrderId, sellOrderId, symbol, quantity, buyPrice, sellPrice, metadata, ts);
    }
}
