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

/**
 * @author Alireza Pourtaghi
 */
public sealed class IOCSellLimitOrder extends SellLimitOrder permits FOKSellLimitOrder {

    public IOCSellLimitOrder(final long id, final long ts, final String symbol, final String quantity,
                             final String price) {

        this(id, ts, symbol, quantity, quantity, price);
    }

    public IOCSellLimitOrder(final long id, final long ts, final String symbol, final String quantity,
                             final String remaining, final String price) {

        super(id, ts, symbol, quantity, remaining, price);
    }

    @Override
    public int representationId() {
        return 110;
    }

    @Override
    public String toString() {
        return "IOCSellLimitOrder{} " + super.toString();
    }
}
