package com.lirezap.nex.binary.trade;

import static com.lirezap.nex.binary.BinaryRepresentable.representationSize;

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
        return 8 + 8 + representationSize(symbol) + representationSize(quantity) + representationSize(buyPrice) +
                representationSize(sellPrice) + representationSize(metadata) + 8;
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
}
