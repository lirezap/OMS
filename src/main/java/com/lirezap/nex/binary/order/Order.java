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
    private final String currency;

    public Order(final long id, final long ts, final String symbol, final String quantity, final String price,
                 final String currency) {

        this.id = id;
        this.ts = ts;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.currency = currency;
    }

    public final int size() {
        return 8 + 8 + representationSize(symbol) + representationSize(quantity) + representationSize(price) + representationSize(currency);
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

    public String getCurrency() {
        return currency;
    }
}
