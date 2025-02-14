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

            assertEquals(BinaryRepresentable.id(binaryRepresentation.segment()), -1);
            assertEquals(binaryRepresentation.size(), 21);
            assertEquals(binaryRepresentation.representationSize(), 31);
        }
    }

    @Test
    public void testFileHeader() {
        var header = new FileHeader(0);
        try (var binaryRepresentation = new FileHeaderBinaryRepresentation(header)) {
            binaryRepresentation.encodeV1();

            assertEquals(BinaryRepresentable.id(binaryRepresentation.segment()), 1);
            assertEquals(binaryRepresentation.size(), 8);
            assertEquals(binaryRepresentation.representationSize(), 18);
            assertEquals(binaryRepresentation.durabilitySize(), 0);
            binaryRepresentation.incrementDurabilitySize(8);
            assertEquals(binaryRepresentation.durabilitySize(), 8);
        }
    }

    @Test
    public void testBuyLimitOrder() {
        var order = new BuyLimitOrder(1, System.currentTimeMillis(), "BTC/USDT", "1", "100000");
        try (var binaryRepresentation = new LimitOrderBinaryRepresentation(order)) {
            binaryRepresentation.encodeV1();

            assertEquals(BinaryRepresentable.id(binaryRepresentation.segment()), 101);
            assertEquals(binaryRepresentation.size(), 46);
            assertEquals(binaryRepresentation.representationSize(), 56);

            var decoded = BuyLimitOrder.decode(binaryRepresentation.segment());
            assertEquals(decoded.getId(), order.getId());
            assertEquals(decoded.getTs(), order.getTs());
            assertEquals(decoded.getSymbol(), order.getSymbol());
            assertEquals(decoded.getQuantity(), order.getQuantity());
            assertEquals(decoded.getPrice(), order.getPrice());
        }
    }

    @Test
    public void testSellLimitOrder() {
        var order = new SellLimitOrder(1, System.currentTimeMillis(), "BTC/USDT", "1", "100000");
        try (var binaryRepresentation = new LimitOrderBinaryRepresentation(order)) {
            binaryRepresentation.encodeV1();

            assertEquals(BinaryRepresentable.id(binaryRepresentation.segment()), 102);
            assertEquals(binaryRepresentation.size(), 46);
            assertEquals(binaryRepresentation.representationSize(), 56);

            var decoded = SellLimitOrder.decode(binaryRepresentation.segment());
            assertEquals(decoded.getId(), order.getId());
            assertEquals(decoded.getTs(), order.getTs());
            assertEquals(decoded.getSymbol(), order.getSymbol());
            assertEquals(decoded.getQuantity(), order.getQuantity());
            assertEquals(decoded.getPrice(), order.getPrice());
        }
    }

    @Test
    public void testTrade() {
        var trade = new Trade(1, 2, "BTC/USDT", "1", "100000", "100000", "bor:0;sor:0", System.currentTimeMillis());
        try (var binaryRepresentation = new TradeBinaryRepresentation(trade)) {
            binaryRepresentation.encodeV1();

            assertEquals(BinaryRepresentable.id(binaryRepresentation.segment()), 103);
            assertEquals(binaryRepresentation.size(), 81);
            assertEquals(binaryRepresentation.representationSize(), 91);

            var decoded = TradeBinaryRepresentation.decode(binaryRepresentation.segment());
            assertEquals(decoded.getBuyOrderId(), trade.getBuyOrderId());
            assertEquals(decoded.getSellOrderId(), trade.getSellOrderId());
            assertEquals(decoded.getSymbol(), trade.getSymbol());
            assertEquals(decoded.getQuantity(), trade.getQuantity());
            assertEquals(decoded.getBuyPrice(), trade.getBuyPrice());
            assertEquals(decoded.getSellPrice(), trade.getSellPrice());
            assertEquals(decoded.getMetadata(), trade.getMetadata());
            assertEquals(decoded.getTs(), trade.getTs());
        }
    }

    @Test
    public void testCancelOrder() {
        var order = new CancelOrder(1, System.currentTimeMillis(), "BTC/USDT", "1");
        try (var binaryRepresentation = new OrderBinaryRepresentation(order)) {
            binaryRepresentation.encodeV1();

            assertEquals(BinaryRepresentable.id(binaryRepresentation.segment()), 104);
            assertEquals(binaryRepresentation.size(), 35);
            assertEquals(binaryRepresentation.representationSize(), 45);

            var decoded = CancelOrder.decode(binaryRepresentation.segment());
            assertEquals(decoded.getId(), order.getId());
            assertEquals(decoded.getTs(), order.getTs());
            assertEquals(decoded.getSymbol(), order.getSymbol());
            assertEquals(decoded.getQuantity(), order.getQuantity());
        }
    }

    @Test
    public void testFetchOrderBook() {
        var fetchOrderBook = new FetchOrderBook("BTC/USDT", 100);
        try (var binaryRepresentation = new FetchOrderBookBinaryRepresentation(fetchOrderBook)) {
            binaryRepresentation.encodeV1();

            assertEquals(BinaryRepresentable.id(binaryRepresentation.segment()), 105);
            assertEquals(binaryRepresentation.size(), 17);
            assertEquals(binaryRepresentation.representationSize(), 27);

            var decoded = FetchOrderBookBinaryRepresentation.decode(binaryRepresentation.segment());
            assertEquals(decoded.getSymbol(), fetchOrderBook.getSymbol());
            assertEquals(decoded.getFetchSize(), fetchOrderBook.getFetchSize());
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

                assertEquals(BinaryRepresentable.id(binaryRepresentation.segment()), 106);
                assertEquals(binaryRepresentation.size(), 288);
                assertEquals(binaryRepresentation.representationSize(), 298);

                var bids = OrderBookBinaryRepresentation.bids(binaryRepresentation.segment());
                var asks = OrderBookBinaryRepresentation.asks(binaryRepresentation.segment());

                assertEquals(bids.size(), 3);
                assertEquals(asks.size(), 2);

                assertEquality(bids.get(0), bo1);
                assertEquality(bids.get(1), bo2);
                assertEquality(bids.get(2), bo3);
                assertEquality(asks.get(0), so4);
                assertEquality(asks.get(1), so5);
            }
        }
    }

    @Test
    public void testBuyMarketOrder() {
        var order = new BuyMarketOrder(1, System.currentTimeMillis(), "BTC/USDT", "1");
        try (var binaryRepresentation = new OrderBinaryRepresentation(order)) {
            binaryRepresentation.encodeV1();

            assertEquals(BinaryRepresentable.id(binaryRepresentation.segment()), 107);
            assertEquals(binaryRepresentation.size(), 35);
            assertEquals(binaryRepresentation.representationSize(), 45);

            var decoded = BuyMarketOrder.decode(binaryRepresentation.segment());
            assertEquals(decoded.getId(), order.getId());
            assertEquals(decoded.getTs(), order.getTs());
            assertEquals(decoded.getSymbol(), order.getSymbol());
            assertEquals(decoded.getQuantity(), order.getQuantity());
        }
    }

    @Test
    public void testSellMarketOrder() {
        var order = new SellMarketOrder(1, System.currentTimeMillis(), "BTC/USDT", "1");
        try (var binaryRepresentation = new OrderBinaryRepresentation(order)) {
            binaryRepresentation.encodeV1();

            assertEquals(BinaryRepresentable.id(binaryRepresentation.segment()), 108);
            assertEquals(binaryRepresentation.size(), 35);
            assertEquals(binaryRepresentation.representationSize(), 45);

            var decoded = SellMarketOrder.decode(binaryRepresentation.segment());
            assertEquals(decoded.getId(), order.getId());
            assertEquals(decoded.getTs(), order.getTs());
            assertEquals(decoded.getSymbol(), order.getSymbol());
            assertEquals(decoded.getQuantity(), order.getQuantity());
        }
    }

    private void assertEquality(LimitOrder first, LimitOrder second) {
        assertEquals(first.getId(), second.getId());
        assertEquals(first.getTs(), second.getTs());
        assertEquals(first.getSymbol(), second.getSymbol());
        assertEquals(first.getQuantity(), second.getQuantity());
        assertEquals(first.getPrice(), second.getPrice());
    }
}
