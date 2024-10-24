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

    public long getId() {
        return id;
    }

    public long getTs() {
        return ts;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getPrice() {
        return price;
    }
}
