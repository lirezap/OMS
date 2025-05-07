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

import software.openex.oms.binary.BinaryRepresentation;

import java.lang.foreign.Arena;

/**
 * @author Alireza Pourtaghi
 */
public final class StopLimitOrderBinaryRepresentation extends BinaryRepresentation<StopLimitOrder> {
    private final StopLimitOrder stopLimitOrder;

    public StopLimitOrderBinaryRepresentation(final StopLimitOrder stopLimitOrder) {
        super(stopLimitOrder.size());
        this.stopLimitOrder = stopLimitOrder;
    }

    public StopLimitOrderBinaryRepresentation(final Arena arena, final StopLimitOrder stopLimitOrder) {
        super(arena, stopLimitOrder.size());
        this.stopLimitOrder = stopLimitOrder;
    }

    @Override
    protected int id() {
        return stopLimitOrder.representationId();
    }

    @Override
    protected void encodeRecord() {
        try {
            putLong(stopLimitOrder.getId());
            putLong(stopLimitOrder.getTs());
            putString(stopLimitOrder.getSymbol());
            putString(stopLimitOrder.getQuantity());
            putString(stopLimitOrder.getPrice());
            putString(stopLimitOrder.getStopPrice());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
