package com.lirezap.nex.binary.order;

import com.lirezap.nex.binary.BinaryRepresentation;

import java.lang.foreign.Arena;

/**
 * @author Alireza Pourtaghi
 */
public final class OrderBinaryRepresentation extends BinaryRepresentation<Order> {
    private final Order order;

    public OrderBinaryRepresentation(final Order order) {
        super(order.size());
        this.order = order;
    }

    public OrderBinaryRepresentation(final Arena arena, final Order order) {
        super(arena, order.size());
        this.order = order;
    }

    @Override
    protected int id() {
        return order.representationId();
    }

    @Override
    protected void encodeRecord() {
        try {
            putLong(order.getId());
            putLong(order.getTs());
            putString(order.getSymbol());
            putString(order.getQuantity());
            putString(order.getRemaining());
            putString(order.getPrice());
            putString(order.getCurrency());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
