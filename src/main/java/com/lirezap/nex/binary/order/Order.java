package com.lirezap.nex.binary.order;

import static com.lirezap.nex.binary.BinaryRepresentable.representationSize;

/**
 * @author Alireza Pourtaghi
 */
public abstract sealed class Order permits BuyOrder, SellOrder {
    private final long id;
    private final long ts;
    private final String symbol;
    private final String quantity;
    private final String price;

    public Order(final long id, final long ts, final String symbol, final String quantity, final String price) {
        this.id = id;
        this.ts = ts;
        this.symbol = symbol == null ? "" : symbol;
        this.quantity = quantity == null ? "" : quantity;
        this.price = price == null ? "" : price;
    }

    public final int size() {
        return 8 + 8 + representationSize(symbol) + representationSize(quantity) + representationSize(price);
    }

    public abstract int representationId();

    public final long getId() {
        return id;
    }

    public final long getTs() {
        return ts;
    }

    public final String getSymbol() {
        return symbol;
    }

    public final String getQuantity() {
        return quantity;
    }

    public final String getPrice() {
        return price;
    }
}
