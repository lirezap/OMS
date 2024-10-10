package com.lirezap.nex.binary.order;

/**
 * @author Alireza Pourtaghi
 */
public final class BuyOrder extends Order {

    public BuyOrder(final long id, final long ts, final String symbol, final String quantity, final String price,
                    final String currency) {

        super(id, ts, symbol, quantity, price, currency);
    }

    @Override
    public int representationId() {
        return 101;
    }
}
