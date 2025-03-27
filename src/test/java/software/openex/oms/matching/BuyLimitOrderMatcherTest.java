package software.openex.oms.matching;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import software.openex.oms.binary.order.FOKBuyLimitOrder;
import software.openex.oms.binary.order.IOCBuyLimitOrder;
import software.openex.oms.binary.order.SellLimitOrder;
import software.openex.oms.binary.order.book.FetchOrderBook;
import software.openex.oms.binary.trade.Trade;
import software.openex.oms.context.AppContext;

import java.math.BigDecimal;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.math.BigDecimal.ZERO;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.openex.oms.context.AppContext.contextTest;
import static software.openex.oms.models.Tables.TRADE;

/**
 * @author Alireza Pourtaghi
 */
public class BuyLimitOrderMatcherTest {
    private static final PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:16");
    private volatile AppContext context;

    @Test
    public void testIOCEqualityMatching() throws Exception {
        var slo1 = new SellLimitOrder(4000 + 1, currentTimeMillis(), "BTC|USDT", "1", "100000");
        var blo = new IOCBuyLimitOrder(4000 + 2, currentTimeMillis(), "BTC|USDT", "1", "100000");

        context.matchingEngines().offer(slo1).get();
        context.matchingEngines().offer(blo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(ZERO, slo1.get_remaining());
        assertEquals(ZERO, blo.get_remaining());
        assertEquals(0, orderBook.getAsks().size());

        sleep(500);
        assertTrue(tradeExistsAndIsValid(new Trade(4000 + 2, 4000 + 1, "BTC|USDT", "1", "100000", "100000", "bor:0;sor:0", currentTimeMillis())));
    }

    @Test
    public void testIOCEqualityMatching2() throws Exception {
        var slo1 = new SellLimitOrder(4000 + 1, currentTimeMillis(), "BTC|USDT", "1", "100000");
        var blo = new IOCBuyLimitOrder(4000 + 2, currentTimeMillis(), "BTC|USDT", "1", "90000");

        context.matchingEngines().offer(slo1).get();
        context.matchingEngines().offer(blo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(1, orderBook.getAsks().size());
    }

    @Test
    public void testIOCGreaterThanMatching() throws Exception {
        var slo1 = new SellLimitOrder(4000 + 3, currentTimeMillis(), "BTC|USDT", "1", "100000");
        var blo = new IOCBuyLimitOrder(4000 + 4, currentTimeMillis(), "BTC|USDT", "2", "100000");

        context.matchingEngines().offer(slo1).get();
        context.matchingEngines().offer(blo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(ZERO, slo1.get_remaining());
        assertEquals(new BigDecimal("1"), blo.get_remaining());
        assertEquals(0, orderBook.getAsks().size());

        sleep(500);
        assertTrue(tradeExistsAndIsValid(new Trade(4000 + 4, 4000 + 3, "BTC|USDT", "1", "100000", "100000", "bor:1;sor:0", currentTimeMillis())));
    }

    @Test
    public void testIOCLessThanMatching() throws Exception {
        var slo1 = new SellLimitOrder(4000 + 5, currentTimeMillis(), "BTC|USDT", "2", "100000");
        var blo = new IOCBuyLimitOrder(4000 + 6, currentTimeMillis(), "BTC|USDT", "1", "100000");

        context.matchingEngines().offer(slo1).get();
        context.matchingEngines().offer(blo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(new BigDecimal("1"), slo1.get_remaining());
        assertEquals(ZERO, blo.get_remaining());
        assertEquals(1, orderBook.getAsks().size());

        sleep(500);
        assertTrue(tradeExistsAndIsValid(new Trade(4000 + 6, 4000 + 5, "BTC|USDT", "1", "100000", "100000", "bor:0;sor:1", currentTimeMillis())));
    }

    @Test
    public void testIOCForLoop() throws Exception {
        var slo1 = new SellLimitOrder(4000 + 7, currentTimeMillis(), "BTC|USDT", "1", "100000");
        var slo2 = new SellLimitOrder(4000 + 8, currentTimeMillis(), "BTC|USDT", "1.25", "100001");
        var slo3 = new SellLimitOrder(4000 + 9, currentTimeMillis(), "BTC|USDT", "2", "100002");
        var slo4 = new SellLimitOrder(4000 + 10, currentTimeMillis(), "BTC|USDT", "4.75", "100003");
        var slo5 = new SellLimitOrder(4000 + 11, currentTimeMillis(), "BTC|USDT", "5", "100004");
        var blo = new IOCBuyLimitOrder(4000 + 12, currentTimeMillis(), "BTC|USDT", "10", "100004");

        context.matchingEngines().offer(slo1).get();
        context.matchingEngines().offer(slo2).get();
        context.matchingEngines().offer(slo3).get();
        context.matchingEngines().offer(slo4).get();
        context.matchingEngines().offer(slo5).get();
        context.matchingEngines().offer(blo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(ZERO, slo1.get_remaining());
        assertEquals(ZERO, slo2.get_remaining());
        assertEquals(ZERO, slo3.get_remaining());
        assertEquals(ZERO, slo4.get_remaining());
        assertEquals(new BigDecimal("4.00"), slo5.get_remaining());
        assertEquals(ZERO, blo.get_remaining());
        assertEquals(1, orderBook.getAsks().size());

        sleep(500);
        assertTrue(tradeExistsAndIsValid(new Trade(4000 + 12, 4000 + 7, "BTC|USDT", "1", "100004", "100000", "bor:9;sor:0", currentTimeMillis())));
        assertTrue(tradeExistsAndIsValid(new Trade(4000 + 12, 4000 + 8, "BTC|USDT", "1.25", "100004", "100001", "bor:7.75;sor:0", currentTimeMillis())));
        assertTrue(tradeExistsAndIsValid(new Trade(4000 + 12, 4000 + 9, "BTC|USDT", "2", "100004", "100002", "bor:5.75;sor:0", currentTimeMillis())));
        assertTrue(tradeExistsAndIsValid(new Trade(4000 + 12, 4000 + 10, "BTC|USDT", "4.75", "100004", "100003", "bor:1;sor:0", currentTimeMillis())));
        assertTrue(tradeExistsAndIsValid(new Trade(4000 + 12, 4000 + 11, "BTC|USDT", "1.00", "100004", "100004", "bor:0;sor:4", currentTimeMillis())));
    }

    @Test
    public void testFOKEqualityMatching() throws Exception {
        var slo1 = new SellLimitOrder(4000 + 13, currentTimeMillis(), "BTC|USDT", "1", "100000");
        var blo = new FOKBuyLimitOrder(4000 + 14, currentTimeMillis(), "BTC|USDT", "1", "100000");

        context.matchingEngines().offer(slo1).get();
        context.matchingEngines().offer(blo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(ZERO, slo1.get_remaining());
        assertEquals(ZERO, blo.get_remaining());
        assertEquals(0, orderBook.getAsks().size());

        sleep(500);
        assertTrue(tradeExistsAndIsValid(new Trade(4000 + 14, 4000 + 13, "BTC|USDT", "1", "100000", "100000", "bor:0;sor:0", currentTimeMillis())));
    }

    @Test
    public void testFOKEqualityMatching2() throws Exception {
        var slo1 = new SellLimitOrder(4000 + 13, currentTimeMillis(), "BTC|USDT", "1", "100000");
        var blo = new FOKBuyLimitOrder(4000 + 14, currentTimeMillis(), "BTC|USDT", "1", "90000");

        context.matchingEngines().offer(slo1).get();
        context.matchingEngines().offer(blo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(1, orderBook.getAsks().size());
    }

    @Test
    public void testFOKGreaterThanMatching() throws Exception {
        var slo1 = new SellLimitOrder(4000 + 15, currentTimeMillis(), "BTC|USDT", "1", "100000");
        var blo = new FOKBuyLimitOrder(4000 + 16, currentTimeMillis(), "BTC|USDT", "2", "100000");

        context.matchingEngines().offer(slo1).get();
        context.matchingEngines().offer(blo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(1, orderBook.getAsks().size());
    }

    @Test
    public void testFOKLessThanMatching() throws Exception {
        var slo1 = new SellLimitOrder(4000 + 17, currentTimeMillis(), "BTC|USDT", "2", "100000");
        var blo = new FOKBuyLimitOrder(4000 + 18, currentTimeMillis(), "BTC|USDT", "1", "100000");

        context.matchingEngines().offer(slo1).get();
        context.matchingEngines().offer(blo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(new BigDecimal("1"), slo1.get_remaining());
        assertEquals(ZERO, blo.get_remaining());
        assertEquals(1, orderBook.getAsks().size());

        sleep(500);
        assertTrue(tradeExistsAndIsValid(new Trade(4000 + 18, 4000 + 17, "BTC|USDT", "1", "100000", "100000", "bor:0;sor:1", currentTimeMillis())));
    }

    private boolean tradeExistsAndIsValid(Trade trade) {
        var recordFetched = context.dataBase().postgresql()
                .select(TRADE.BUY_ORDER_ID,
                        TRADE.SELL_ORDER_ID,
                        TRADE.SYMBOL,
                        TRADE.QUANTITY,
                        TRADE.BUY_PRICE,
                        TRADE.SELL_PRICE,
                        TRADE.METADATA,
                        TRADE.TS)
                .from(TRADE)
                .where(TRADE.BUY_ORDER_ID.eq(trade.getBuyOrderId()))
                .and(TRADE.SELL_ORDER_ID.eq(trade.getSellOrderId()))
                .and(TRADE.SYMBOL.eq(trade.getSymbol()))
                .fetchOne();

        return recordFetched != null
                && recordFetched.component1().equals(trade.getBuyOrderId())
                && recordFetched.component2().equals(trade.getSellOrderId())
                && recordFetched.component3().equals(trade.getSymbol())
                && recordFetched.component4().equals(trade.getQuantity())
                && recordFetched.component5().equals(trade.getBuyPrice())
                && recordFetched.component6().equals(trade.getSellPrice())
                && recordFetched.component7().equals(trade.getMetadata())
                && recordFetched.component8().toEpochMilli() < trade.getTs();
    }

    @BeforeAll
    public static void setup() {
        // Start postgresql container.
        postgresql.setPortBindings(of("127.0.0.1:5432:5432"));
        postgresql.withDatabaseName("oms");
        postgresql.withUsername("oms");
        postgresql.withPassword("oms");
        postgresql.start();
    }

    @BeforeEach
    public void buildContext() {
        // Start/Replace application context.
        context = contextTest();
        context.databaseMigrator().migrate();
        context.matchingEngines().start();
    }

    @AfterEach
    public void closeContext() {
        // Stop application context.
        if (context != null) {
            context.close();
        }
    }

    @AfterAll
    public static void stop() {
        // Stop postgresql container.
        postgresql.stop();
    }
}
