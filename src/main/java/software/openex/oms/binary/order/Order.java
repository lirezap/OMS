/*
 * ISC License
 *
 * Copyright (c) 2025, Alireza Pourtaghi <lirezap@protonmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package software.openex.oms.binary.order;

import java.math.BigDecimal;

import static java.lang.Math.addExact;
import static software.openex.oms.binary.BinaryRepresentable.representationSize;

/**
 * @author Alireza Pourtaghi
 */
public abstract sealed class Order permits LimitOrder, CancelOrder, MarketOrder {
    private final long id;
    private final long ts;
    private final String symbol;
    private final String quantity;
    private final BigDecimal _quantity;
    private BigDecimal _remaining;

    public Order(final long id, final long ts, final String symbol, final String quantity) {
        this(id, ts, symbol, quantity, quantity);
    }

    public Order(final long id, final long ts, final String symbol, final String quantity, final String remaining) {
        this.id = id;
        this.ts = ts;
        this.symbol = symbol == null ? "" : symbol;
        this.quantity = quantity == null ? "" : quantity;
        this._quantity = new BigDecimal(this.quantity);
        this._remaining = new BigDecimal(remaining);
    }

    public int size() {
        return addExact(8, addExact(8, addExact(representationSize(symbol), representationSize(quantity))));
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

    public final BigDecimal get_quantity() {
        return _quantity;
    }

    public final BigDecimal get_remaining() {
        return _remaining;
    }

    public final void set_remaining(final BigDecimal _remaining) {
        this._remaining = _remaining;
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
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", ts=" + ts +
                ", symbol='" + symbol + '\'' +
                ", quantity='" + quantity + '\'' +
                ", _quantity=" + _quantity +
                ", _remaining=" + _remaining +
                '}';
    }
}
