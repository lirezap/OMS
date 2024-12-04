package software.openex.oms.binary.order;

import java.math.BigDecimal;

import static java.lang.Long.compare;
import static software.openex.oms.binary.BinaryRepresentable.representationSize;

/**
 * @author Alireza Pourtaghi
 */
public abstract sealed class Order implements Comparable<Order> permits BuyOrder, SellOrder, CancelOrder {
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

    public final BigDecimal get_quantity() {
        return _quantity;
    }

    public final BigDecimal get_price() {
        return _price;
    }

    public final BigDecimal get_remaining() {
        return _remaining;
    }

    public final void set_remaining(final BigDecimal _remaining) {
        this._remaining = _remaining;
    }

    @Override
    public final int compareTo(final Order o) {
        final var compare = _price.compareTo(o._price);
        return compare == 0 ? -compare(ts, o.ts) : compare;
    }

    @Override
    public final boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Order order)) return false;

        return getId() == order.getId() && getSymbol().equals(order.getSymbol());
    }

    @Override
    public final int hashCode() {
        var result = Long.hashCode(getId());
        result = 31 * result + getSymbol().hashCode();
        return result;
    }

    @Override
    public final String toString() {
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
