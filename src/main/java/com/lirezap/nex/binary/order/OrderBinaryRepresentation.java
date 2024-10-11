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
            encodeId();
            encodeTs();
            encodeSymbol();
            encodeQuantity();
            encodePrice();
            encodeCurrency();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void encodeId() {
        putLong(order.getId());
    }

    private void encodeTs() {
        putLong(order.getTs());
    }

    private void encodeSymbol() {
        putString(order.getSymbol());
    }

    private void encodeQuantity() {
        putString(order.getQuantity());
    }

    private void encodePrice() {
        putString(order.getPrice());
    }

    private void encodeCurrency() {
        putString(order.getCurrency());
    }
}
