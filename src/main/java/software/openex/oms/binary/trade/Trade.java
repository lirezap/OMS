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
package software.openex.oms.binary.trade;

import static java.lang.Math.addExact;
import static software.openex.oms.binary.BinaryRepresentable.representationSize;

/**
 * @author Alireza Pourtaghi
 */
public final class Trade {
    private final long buyOrderId;
    private final long sellOrderId;
    private final String symbol;
    private final String quantity;
    private final String buyPrice;
    private final String sellPrice;
    private final String metadata;
    private final long ts;

    public Trade(final long buyOrderId, final long sellOrderId, final String symbol, final String quantity,
                 final String buyPrice, final String sellPrice, final String metadata, final long ts) {

        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.symbol = symbol == null ? "" : symbol;
        this.quantity = quantity == null ? "" : quantity;
        this.buyPrice = buyPrice == null ? "" : buyPrice;
        this.sellPrice = sellPrice == null ? "" : sellPrice;
        this.metadata = metadata == null ? "" : metadata;
        this.ts = ts;
    }

    public int size() {
        return addExact(8,
                addExact(8,
                        addExact(representationSize(symbol),
                                addExact(representationSize(quantity),
                                        addExact(representationSize(buyPrice),
                                                addExact(representationSize(sellPrice),
                                                        addExact(representationSize(metadata), 8)))))));
    }

    public long getBuyOrderId() {
        return buyOrderId;
    }

    public long getSellOrderId() {
        return sellOrderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getBuyPrice() {
        return buyPrice;
    }

    public String getSellPrice() {
        return sellPrice;
    }

    public String getMetadata() {
        return metadata;
    }

    public long getTs() {
        return ts;
    }

    @Override
    public String toString() {
        return "Trade{" +
                "buyOrderId=" + buyOrderId +
                ", sellOrderId=" + sellOrderId +
                ", symbol='" + symbol + '\'' +
                ", quantity='" + quantity + '\'' +
                ", buyPrice='" + buyPrice + '\'' +
                ", sellPrice='" + sellPrice + '\'' +
                ", metadata='" + metadata + '\'' +
                ", ts=" + ts +
                '}';
    }
}
