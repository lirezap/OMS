package com.openex.oms.binary.order;

import java.math.BigDecimal;

import static com.openex.oms.binary.BinaryRepresentable.representationSize;
import static java.lang.Long.compare;

/**
 * @author Alireza Pourtaghi
 */
public abstract sealed class Order implements Comparable<Order> permits BuyOrder, SellOrder {
    private final long id;
    private final long ts;
    private final String symbol;
    private final String quantity;
    private final String price;

    private final BigDecimal _quantity;
    private final BigDecimal _price;
    private BigDecimal _remaining;

    public Order(final long id, final long ts, final String symbol, final String quantity, final String price) {
        this.id = id;
        this.ts = ts;
        this.symbol = symbol == null ? "" : symbol;
        this.quantity = quantity == null ? "" : quantity;
        this.price = price == null ? "" : price;

        this._quantity = new BigDecimal(this.quantity);
        this._price = new BigDecimal(this.price);
        this._remaining = new BigDecimal(this.quantity);
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

    public BigDecimal get_quantity() {
        return _quantity;
    }

    public BigDecimal get_price() {
        return _price;
    }

    public BigDecimal get_remaining() {
        return _remaining;
    }

    public void set_remaining(final BigDecimal _remaining) {
        this._remaining = _remaining;
    }

    @Override
    public int compareTo(final Order o) {
        final var compare = _price.compareTo(o._price);
        return compare == 0 ? -compare(ts, o.ts) : compare;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", ts=" + ts +
                ", symbol='" + symbol + '\'' +
                ", quantity='" + quantity + '\'' +
                ", price='" + price + '\'' +
                ", _quantity=" + _quantity +
                ", _price=" + _price +
                ", _remaining=" + _remaining +
                '}';
    }
}
