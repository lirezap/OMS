package software.openex.oms.binary.order;

import java.lang.foreign.MemorySegment;

import static software.openex.oms.binary.BinaryRepresentable.*;

/**
 * @author Alireza Pourtaghi
 */
public final class CancelOrder extends Order {

    public CancelOrder(final long id, final long ts, final String symbol, final String quantity, final String price) {
        super(id, ts, symbol, quantity, price);
    }

    @Override
    public int representationId() {
        return 104;
    }

    public static CancelOrder decode(final MemorySegment segment) {
        long position = RHS;

        var id = segment.get(LONG, position);
        position += LONG.byteSize();

        var ts = segment.get(LONG, position);
        position += LONG.byteSize();

        var symbolSize = segment.get(INT, position);
        position += INT.byteSize();

        var symbol = segment.getString(position);
        position += symbolSize;

        var quantitySize = segment.get(INT, position);
        position += INT.byteSize();

        var quantity = segment.getString(position);
        position += quantitySize;

        var priceSize = segment.get(INT, position);
        position += INT.byteSize();

        var price = segment.getString(position);

        return new CancelOrder(id, ts, symbol, quantity, price);
    }
}
