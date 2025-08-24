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
public abstract sealed class StopOrder extends MarketOrder permits BuyStopOrder, SellStopOrder {
    private final String stopPrice;
    private final BigDecimal _stopPrice;

    public StopOrder(final long id, final long ts, final String symbol, final String quantity,
                     final String stopPrice) {

        super(id, ts, symbol, quantity);
        this.stopPrice = stopPrice == null ? "" : stopPrice;
        this._stopPrice = new BigDecimal(this.stopPrice);
    }

    @Override
    public int size() {
        return addExact(super.size(), representationSize(stopPrice));
    }

    public final String getStopPrice() {
        return stopPrice;
    }

    public final BigDecimal get_stopPrice() {
        return _stopPrice;
    }

    @Override
    public String toString() {
        return "StopOrder{" +
                "stopPrice='" + stopPrice + '\'' +
                ", _stopPrice=" + _stopPrice +
                "} " + super.toString();
    }
}
