package com.lirezap.nex.binary.order;

/**
 * @author Alireza Pourtaghi
 */
public final class SellOrder extends Order {

    public SellOrder(final long id, final long ts, final String symbol, final String quantity, final String price) {
        super(id, ts, symbol, quantity, price);
    }

    @Override
    public int representationId() {
        return 102;
    }
}
