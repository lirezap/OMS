package software.openex.oms.binary.trade;

import java.lang.foreign.MemorySegment;

import static software.openex.oms.binary.BinaryRepresentable.*;

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

    public static Trade decode(final MemorySegment segment) {
        long position = RHS;

        var buyOrderId = segment.get(LONG, position);
        position += LONG.byteSize();

        var sellOrderId = segment.get(LONG, position);
        position += LONG.byteSize();

        var symbolSize = segment.get(INT, position);
        position += INT.byteSize();

        var symbol = segment.getString(position);
        position += symbolSize;

        var quantitySize = segment.get(INT, position);
        position += INT.byteSize();

        var quantity = segment.getString(position);
        position += quantitySize;

        var buyPriceSize = segment.get(INT, position);
        position += INT.byteSize();

        var buyPrice = segment.getString(position);
        position += buyPriceSize;

        var sellPriceSize = segment.get(INT, position);
        position += INT.byteSize();

        var sellPrice = segment.getString(position);
        position += sellPriceSize;

        var metadataSize = segment.get(INT, position);
        position += INT.byteSize();

        var metadata = segment.getString(position);
        position += metadataSize;

        var ts = segment.get(LONG, position);

        return new Trade(buyOrderId, sellOrderId, symbol, quantity, buyPrice, sellPrice, metadata, ts);
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
