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
    private final String remaining;
    private final String price;
    private final String currency;

    public Order(final long id, final long ts, final String symbol, final String quantity, final String remaining,
                 final String price, final String currency) {

        this.id = id;
        this.ts = ts;
        this.symbol = symbol == null ? "" : symbol;
        this.quantity = quantity == null ? "" : quantity;
        this.remaining = remaining == null ? "" : remaining;
        this.price = price == null ? "" : price;
        this.currency = currency == null ? "" : currency;
    }

    public final int size() {
        return 8 + 8 + representationSize(symbol) + representationSize(quantity) + representationSize(remaining) +
                representationSize(price) + representationSize(currency);
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

    public String getRemaining() {
        return remaining;
    }

    public String getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }
}
