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

import static software.openex.oms.binary.BinaryRepresentable.representationSize;

/**
 * @author Alireza Pourtaghi
 */
public abstract sealed class LimitOrder extends Order implements Comparable<LimitOrder> permits
        BuyLimitOrder, SellLimitOrder, StopLimitOrder {

    private final String price;
    private final BigDecimal _price;

    public LimitOrder(final long id, final long ts, final String symbol, final String quantity, final String price) {
        this(id, ts, symbol, quantity, quantity, price);
    }

    public LimitOrder(final long id, final long ts, final String symbol, final String quantity, final String remaining,
                      final String price) {

        super(id, ts, symbol, quantity, remaining);
        this.price = price == null ? "" : price;
        this._price = new BigDecimal(this.price);
    }

    @Override
    public int size() {
        return super.size() + representationSize(price);
    }

    public final String getPrice() {
        return price;
    }

    public final BigDecimal get_price() {
        return _price;
    }

    @Override
    public String toString() {
        return "LimitOrder{" +
                "price='" + price + '\'' +
                ", _price=" + _price +
                "} " + super.toString();
    }
}
