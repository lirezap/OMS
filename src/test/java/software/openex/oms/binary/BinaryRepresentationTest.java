package software.openex.oms.binary;

import org.junit.jupiter.api.Test;
import software.openex.oms.binary.base.ErrorMessage;
import software.openex.oms.binary.base.ErrorMessageBinaryRepresentation;
import software.openex.oms.binary.file.FileHeader;
import software.openex.oms.binary.file.FileHeaderBinaryRepresentation;
import software.openex.oms.binary.order.*;
import software.openex.oms.binary.order.book.FetchOrderBook;
import software.openex.oms.binary.order.book.FetchOrderBookBinaryRepresentation;
import software.openex.oms.binary.order.book.OrderBook;
import software.openex.oms.binary.order.book.OrderBookBinaryRepresentation;
import software.openex.oms.binary.trade.Trade;
import software.openex.oms.binary.trade.TradeBinaryRepresentation;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alireza Pourtaghi
 */
public class BinaryRepresentationTest {

    @Test
    public void testErrorMessage() {
        var message = new ErrorMessage("code", "message");
        try (var binaryRepresentation = new ErrorMessageBinaryRepresentation(message)) {
            binaryRepresentation.encodeV1();

            assertEquals(-1, BinaryRepresentable.id(binaryRepresentation.segment()));
            assertEquals(21, binaryRepresentation.size());
            assertEquals(31, binaryRepresentation.representationSize());
        }
    }

    @Test
    public void testFileHeader() {
        var header = new FileHeader(0);
        try (var binaryRepresentation = new FileHeaderBinaryRepresentation(header)) {
            binaryRepresentation.encodeV1();

            assertEquals(1, BinaryRepresentable.id(binaryRepresentation.segment()));
            assertEquals(8, binaryRepresentation.size());
            assertEquals(18, binaryRepresentation.representationSize());
            assertEquals(0, binaryRepresentation.durabilitySize());
            binaryRepresentation.incrementDurabilitySize(8);
            assertEquals(8, binaryRepresentation.durabilitySize());
        }
    }

    @Test
    public void testBuyLimitOrder() {
        var order = new BuyLimitOrder(1, System.currentTimeMillis(), "BTC/USDT", "1", "100000");
        try (var binaryRepresentation = new LimitOrderBinaryRepresentation(order)) {
            binaryRepresentation.encodeV1();

            assertEquals(101, BinaryRepresentable.id(binaryRepresentation.segment()));
            assertEquals(46, binaryRepresentation.size());
            assertEquals(56, binaryRepresentation.representationSize());

            var decoded = BuyLimitOrder.decode(binaryRepresentation.segment());
            assertEquals(order.getId(), decoded.getId());
            assertEquals(order.getTs(), decoded.getTs());
            assertEquals(order.getSymbol(), decoded.getSymbol());
            assertEquals(order.getQuantity(), decoded.getQuantity());
            assertEquals(order.getPrice(), decoded.getPrice());
            assertEquals(new BigDecimal(order.getQuantity()), decoded.get_quantity());
            assertEquals(new BigDecimal(order.getQuantity()), decoded.get_remaining());
            assertEquals(new BigDecimal(order.getPrice()), decoded.get_price());
            assertEquals(order, decoded);
        }
    }

    @Test
    public void testBuyLimitOrderCompare() {
        var o1 = new BuyLimitOrder(1, 1, "BTC/USDT", "1", "100000");
        var o2 = new BuyLimitOrder(2, 1, "BTC/USDT", "1", "100000");
        assertEquals(0, o1.compareTo(o2));

        o1 = new BuyLimitOrder(1, System.currentTimeMillis(), "BTC/USDT", "1", "100001");
        o2 = new BuyLimitOrder(2, System.currentTimeMillis(), "BTC/USDT", "1", "100000");
        assertEquals(-1, o1.compareTo(o2));

        o1 = new BuyLimitOrder(1, System.currentTimeMillis(), "BTC/USDT", "1", "100000");
        o2 = new BuyLimitOrder(2, System.currentTimeMillis(), "BTC/USDT", "1", "100001");
        assertEquals(1, o1.compareTo(o2));

        o1 = new BuyLimitOrder(1, 1, "BTC/USDT", "1", "100000");
        o2 = new BuyLimitOrder(2, 2, "BTC/USDT", "1", "100000");
        assertEquals(-1, o1.compareTo(o2));
    }

    @Test
    public void testSellLimitOrder() {
        var order = new SellLimitOrder(1, System.currentTimeMillis(), "BTC/USDT", "1", "100000");
        try (var binaryRepresentation = new LimitOrderBinaryRepresentation(order)) {
            binaryRepresentation.encodeV1();

            assertEquals(102, BinaryRepresentable.id(binaryRepresentation.segment()));
            assertEquals(46, binaryRepresentation.size());
            assertEquals(56, binaryRepresentation.representationSize());

            var decoded = SellLimitOrder.decode(binaryRepresentation.segment());
            assertEquals(order.getId(), decoded.getId());
            assertEquals(order.getTs(), decoded.getTs());
            assertEquals(order.getSymbol(), decoded.getSymbol());
            assertEquals(order.getQuantity(), decoded.getQuantity());
            assertEquals(order.getPrice(), decoded.getPrice());
            assertEquals(new BigDecimal(order.getQuantity()), decoded.get_quantity());
            assertEquals(new BigDecimal(order.getQuantity()), decoded.get_remaining());
            assertEquals(new BigDecimal(order.getPrice()), decoded.get_price());
            assertEquals(order, decoded);
        }
    }

    @Test
    public void testSellLimitOrderCompare() {
        var o1 = new SellLimitOrder(1, 1, "BTC/USDT", "1", "100000");
        var o2 = new SellLimitOrder(2, 1, "BTC/USDT", "1", "100000");
        assertEquals(0, o1.compareTo(o2));

        o1 = new SellLimitOrder(1, System.currentTimeMillis(), "BTC/USDT", "1", "100001");
        o2 = new SellLimitOrder(2, System.currentTimeMillis(), "BTC/USDT", "1", "100000");
        assertEquals(1, o1.compareTo(o2));

        o1 = new SellLimitOrder(1, System.currentTimeMillis(), "BTC/USDT", "1", "100000");
        o2 = new SellLimitOrder(2, System.currentTimeMillis(), "BTC/USDT", "1", "100001");
        assertEquals(-1, o1.compareTo(o2));

        o1 = new SellLimitOrder(1, 1, "BTC/USDT", "1", "100000");
        o2 = new SellLimitOrder(2, 2, "BTC/USDT", "1", "100000");
        assertEquals(-1, o1.compareTo(o2));
    }

    @Test
    public void testTrade() {
        var trade = new Trade(1, 2, "BTC/USDT", "1", "100000", "100000", "bor:0;sor:0", System.currentTimeMillis());
        try (var binaryRepresentation = new TradeBinaryRepresentation(trade)) {
            binaryRepresentation.encodeV1();

            assertEquals(103, BinaryRepresentable.id(binaryRepresentation.segment()));
            assertEquals(81, binaryRepresentation.size());
            assertEquals(91, binaryRepresentation.representationSize());

            var decoded = TradeBinaryRepresentation.decode(binaryRepresentation.segment());
            assertEquals(trade.getBuyOrderId(), decoded.getBuyOrderId());
            assertEquals(trade.getSellOrderId(), decoded.getSellOrderId());
            assertEquals(trade.getSymbol(), decoded.getSymbol());
            assertEquals(trade.getQuantity(), decoded.getQuantity());
            assertEquals(trade.getBuyPrice(), decoded.getBuyPrice());
            assertEquals(trade.getSellPrice(), decoded.getSellPrice());
            assertEquals(trade.getMetadata(), decoded.getMetadata());
            assertEquals(trade.getTs(), decoded.getTs());
        }
    }

    @Test
    public void testCancelOrder() {
        var order = new CancelOrder(1, System.currentTimeMillis(), "BTC/USDT", "1");
        try (var binaryRepresentation = new OrderBinaryRepresentation(order)) {
            binaryRepresentation.encodeV1();

            assertEquals(104, BinaryRepresentable.id(binaryRepresentation.segment()));
            assertEquals(35, binaryRepresentation.size());
            assertEquals(45, binaryRepresentation.representationSize());

            var decoded = CancelOrder.decode(binaryRepresentation.segment());
            assertEquals(order.getId(), decoded.getId());
            assertEquals(order.getTs(), decoded.getTs());
            assertEquals(order.getSymbol(), decoded.getSymbol());
            assertEquals(order.getQuantity(), decoded.getQuantity());
            assertEquals(new BigDecimal(order.getQuantity()), decoded.get_quantity());
            assertEquals(new BigDecimal(order.getQuantity()), decoded.get_remaining());
            assertEquals(order, decoded);
        }
    }

    @Test
    public void testFetchOrderBook() {
        var fetchOrderBook = new FetchOrderBook("BTC/USDT", 100);
        try (var binaryRepresentation = new FetchOrderBookBinaryRepresentation(fetchOrderBook)) {
            binaryRepresentation.encodeV1();

            assertEquals(105, BinaryRepresentable.id(binaryRepresentation.segment()));
            assertEquals(17, binaryRepresentation.size());
            assertEquals(27, binaryRepresentation.representationSize());

            var decoded = FetchOrderBookBinaryRepresentation.decode(binaryRepresentation.segment());
            assertEquals(fetchOrderBook.getSymbol(), decoded.getSymbol());
            assertEquals(fetchOrderBook.getFetchSize(), decoded.getFetchSize());
        }
    }

    @Test
    public void testOrderBook() {
        var bo1 = new BuyLimitOrder(1, System.currentTimeMillis(), "BTC/USDT", "1", "100000");
        var bo2 = new BuyLimitOrder(2, System.currentTimeMillis(), "BTC/USDT", "2", "200000");
        var bo3 = new BuyLimitOrder(3, System.currentTimeMillis(), "BTC/USDT", "3", "300000");
        var so4 = new SellLimitOrder(4, System.currentTimeMillis(), "BTC/USDT", "4", "400000");
        var so5 = new SellLimitOrder(5, System.currentTimeMillis(), "BTC/USDT", "5", "500000");

        try (var bo1br = new LimitOrderBinaryRepresentation(bo1);
             var bo2br = new LimitOrderBinaryRepresentation(bo2);
             var bo3br = new LimitOrderBinaryRepresentation(bo3);
             var so4br = new LimitOrderBinaryRepresentation(so4);
             var so5br = new LimitOrderBinaryRepresentation(so5)) {

            bo1br.encodeV1();
            bo2br.encodeV1();
            bo3br.encodeV1();
            so4br.encodeV1();
            so5br.encodeV1();
            var orderBook = new OrderBook(List.of(bo1br, bo2br, bo3br), List.of(so4br, so5br));
            try (var binaryRepresentation = new OrderBookBinaryRepresentation(orderBook)) {
                binaryRepresentation.encodeV1();

                assertEquals(106, BinaryRepresentable.id(binaryRepresentation.segment()));
                assertEquals(288, binaryRepresentation.size());
                assertEquals(298, binaryRepresentation.representationSize());

                var bids = OrderBookBinaryRepresentation.bids(binaryRepresentation.segment());
                var asks = OrderBookBinaryRepresentation.asks(binaryRepresentation.segment());

                assertEquals(3, bids.size());
                assertEquals(2, asks.size());

                assertEquality(bo1, bids.get(0));
                assertEquality(bo2, bids.get(1));
                assertEquality(bo3, bids.get(2));
                assertEquality(so4, asks.get(0));
                assertEquality(so5, asks.get(1));
            }
        }
    }

    @Test
    public void testBuyMarketOrder() {
        var order = new BuyMarketOrder(1, System.currentTimeMillis(), "BTC/USDT", "1");
        try (var binaryRepresentation = new OrderBinaryRepresentation(order)) {
            binaryRepresentation.encodeV1();

            assertEquals(107, BinaryRepresentable.id(binaryRepresentation.segment()));
            assertEquals(35, binaryRepresentation.size());
            assertEquals(45, binaryRepresentation.representationSize());

            var decoded = BuyMarketOrder.decode(binaryRepresentation.segment());
            assertEquals(order.getId(), decoded.getId());
            assertEquals(order.getTs(), decoded.getTs());
            assertEquals(order.getSymbol(), decoded.getSymbol());
            assertEquals(order.getQuantity(), decoded.getQuantity());
            assertEquals(new BigDecimal(order.getQuantity()), decoded.get_quantity());
            assertEquals(new BigDecimal(order.getQuantity()), decoded.get_remaining());
            assertEquals(order, decoded);
        }
    }

    @Test
    public void testSellMarketOrder() {
        var order = new SellMarketOrder(1, System.currentTimeMillis(), "BTC/USDT", "1");
        try (var binaryRepresentation = new OrderBinaryRepresentation(order)) {
            binaryRepresentation.encodeV1();

            assertEquals(108, BinaryRepresentable.id(binaryRepresentation.segment()));
            assertEquals(35, binaryRepresentation.size());
            assertEquals(45, binaryRepresentation.representationSize());

            var decoded = SellMarketOrder.decode(binaryRepresentation.segment());
            assertEquals(order.getId(), decoded.getId());
            assertEquals(order.getTs(), decoded.getTs());
            assertEquals(order.getSymbol(), decoded.getSymbol());
            assertEquals(order.getQuantity(), decoded.getQuantity());
            assertEquals(new BigDecimal(order.getQuantity()), decoded.get_quantity());
            assertEquals(new BigDecimal(order.getQuantity()), decoded.get_remaining());
            assertEquals(order, decoded);
        }
    }

    private void assertEquality(LimitOrder expected, LimitOrder actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getTs(), actual.getTs());
        assertEquals(expected.getSymbol(), actual.getSymbol());
        assertEquals(expected.getQuantity(), actual.getQuantity());
        assertEquals(expected.getPrice(), actual.getPrice());
    }
}
