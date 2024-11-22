package com.lirezap.nex.binary.trade;

import com.lirezap.nex.binary.BinaryRepresentation;

import java.lang.foreign.Arena;

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
        return 201;
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
}
