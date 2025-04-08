package software.openex.oms.matching;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import software.openex.oms.binary.order.BuyMarketOrder;
import software.openex.oms.binary.order.FOKBuyMarketOrder;
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
public class BuyMarketOrderMatcherTest {
    private static final PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:16");
    private volatile AppContext context;

    @Test
    public void testEqualityMatching() throws Exception {
        var slo1 = new SellLimitOrder(2000 + 1, currentTimeMillis(), "BTC|USDT", "1", "100000");
        var bmo = new BuyMarketOrder(2000 + 2, currentTimeMillis(), "BTC|USDT", "1");

        context.matchingEngines().offer(slo1).get();
        context.matchingEngines().offer(bmo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(ZERO, slo1.get_remaining());
        assertEquals(ZERO, bmo.get_remaining());
        assertEquals(0, orderBook.getAsks().size());

        sleep(500);
        assertTrue(tradeExistsAndIsValid(new Trade(2000 + 2, 2000 + 1, "BTC|USDT", "1", "0", "100000", "bor:0;sor:0", currentTimeMillis())));
    }

    @Test
    public void testGreaterThanMatching() throws Exception {
        var slo1 = new SellLimitOrder(2000 + 3, currentTimeMillis(), "BTC|USDT", "1", "100000");
        var bmo = new BuyMarketOrder(2000 + 4, currentTimeMillis(), "BTC|USDT", "2");

        context.matchingEngines().offer(slo1).get();
        context.matchingEngines().offer(bmo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(ZERO, slo1.get_remaining());
        assertEquals(new BigDecimal("1"), bmo.get_remaining());
        assertEquals(0, orderBook.getAsks().size());

        sleep(500);
        assertTrue(tradeExistsAndIsValid(new Trade(2000 + 4, 2000 + 3, "BTC|USDT", "1", "0", "100000", "bor:1;sor:0", currentTimeMillis())));
    }

    @Test
    public void testLessThanMatching() throws Exception {
        var slo1 = new SellLimitOrder(2000 + 5, currentTimeMillis(), "BTC|USDT", "2", "100000");
        var bmo = new BuyMarketOrder(2000 + 6, currentTimeMillis(), "BTC|USDT", "1");

        context.matchingEngines().offer(slo1).get();
        context.matchingEngines().offer(bmo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(new BigDecimal("1"), slo1.get_remaining());
        assertEquals(ZERO, bmo.get_remaining());
        assertEquals(1, orderBook.getAsks().size());

        sleep(500);
        assertTrue(tradeExistsAndIsValid(new Trade(2000 + 6, 2000 + 5, "BTC|USDT", "1", "0", "100000", "bor:0;sor:1", currentTimeMillis())));
    }

    @Test
    public void testForLoop() throws Exception {
        var slo1 = new SellLimitOrder(2000 + 7, currentTimeMillis(), "BTC|USDT", "1", "100000");
        var slo2 = new SellLimitOrder(2000 + 8, currentTimeMillis(), "BTC|USDT", "1.25", "100001");
        var slo3 = new SellLimitOrder(2000 + 9, currentTimeMillis(), "BTC|USDT", "2", "100002");
        var slo4 = new SellLimitOrder(2000 + 10, currentTimeMillis(), "BTC|USDT", "4.75", "100003");
        var slo5 = new SellLimitOrder(2000 + 11, currentTimeMillis(), "BTC|USDT", "5", "100004");
        var bmo = new BuyMarketOrder(2000 + 12, currentTimeMillis(), "BTC|USDT", "10");

        context.matchingEngines().offer(slo1).get();
        context.matchingEngines().offer(slo2).get();
        context.matchingEngines().offer(slo3).get();
        context.matchingEngines().offer(slo4).get();
        context.matchingEngines().offer(slo5).get();
        context.matchingEngines().offer(bmo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(ZERO, slo1.get_remaining());
        assertEquals(ZERO, slo2.get_remaining());
        assertEquals(ZERO, slo3.get_remaining());
        assertEquals(ZERO, slo4.get_remaining());
        assertEquals(new BigDecimal("4.00"), slo5.get_remaining());
        assertEquals(ZERO, bmo.get_remaining());
        assertEquals(1, orderBook.getAsks().size());

        sleep(500);
        assertTrue(tradeExistsAndIsValid(new Trade(2000 + 12, 2000 + 7, "BTC|USDT", "1", "0", "100000", "bor:9;sor:0", currentTimeMillis())));
        assertTrue(tradeExistsAndIsValid(new Trade(2000 + 12, 2000 + 8, "BTC|USDT", "1.25", "0", "100001", "bor:7.75;sor:0", currentTimeMillis())));
        assertTrue(tradeExistsAndIsValid(new Trade(2000 + 12, 2000 + 9, "BTC|USDT", "2", "0", "100002", "bor:5.75;sor:0", currentTimeMillis())));
        assertTrue(tradeExistsAndIsValid(new Trade(2000 + 12, 2000 + 10, "BTC|USDT", "4.75", "0", "100003", "bor:1;sor:0", currentTimeMillis())));
        assertTrue(tradeExistsAndIsValid(new Trade(2000 + 12, 2000 + 11, "BTC|USDT", "1.00", "0", "100004", "bor:0;sor:4", currentTimeMillis())));
    }

    @Test
    public void testFOKEqualityMatching() throws Exception {
        var slo1 = new SellLimitOrder(2000 + 13, currentTimeMillis(), "BTC|USDT", "1", "100000");
        var bmo = new FOKBuyMarketOrder(2000 + 14, currentTimeMillis(), "BTC|USDT", "1");

        context.matchingEngines().offer(slo1).get();
        context.matchingEngines().offer(bmo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(ZERO, slo1.get_remaining());
        assertEquals(ZERO, bmo.get_remaining());
        assertEquals(0, orderBook.getAsks().size());

        sleep(500);
        assertTrue(tradeExistsAndIsValid(new Trade(2000 + 14, 2000 + 13, "BTC|USDT", "1", "0", "100000", "bor:0;sor:0", currentTimeMillis())));
    }

    @Test
    public void testFOKGreaterThanMatching() throws Exception {
        var slo1 = new SellLimitOrder(2000 + 15, currentTimeMillis(), "BTC|USDT", "1", "100000");
        var bmo = new FOKBuyMarketOrder(2000 + 16, currentTimeMillis(), "BTC|USDT", "2");

        context.matchingEngines().offer(slo1).get();
        context.matchingEngines().offer(bmo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(1, orderBook.getAsks().size());
    }

    @Test
    public void testFOKLessThanMatching() throws Exception {
        var slo1 = new SellLimitOrder(2000 + 17, currentTimeMillis(), "BTC|USDT", "2", "100000");
        var bmo = new FOKBuyMarketOrder(2000 + 18, currentTimeMillis(), "BTC|USDT", "1");

        context.matchingEngines().offer(slo1).get();
        context.matchingEngines().offer(bmo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(new BigDecimal("1"), slo1.get_remaining());
        assertEquals(ZERO, bmo.get_remaining());
        assertEquals(1, orderBook.getAsks().size());

        sleep(500);
        assertTrue(tradeExistsAndIsValid(new Trade(2000 + 18, 2000 + 17, "BTC|USDT", "1", "0", "100000", "bor:0;sor:1", currentTimeMillis())));
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
