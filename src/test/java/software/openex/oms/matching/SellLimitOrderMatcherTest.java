package software.openex.oms.matching;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import software.openex.oms.binary.order.BuyLimitOrder;
import software.openex.oms.binary.order.FOKSellLimitOrder;
import software.openex.oms.binary.order.IOCSellLimitOrder;
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
public class SellLimitOrderMatcherTest {
    private static final PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:16");
    private volatile AppContext context;

    @Test
    public void testIOCEqualityMatching() throws Exception {
        var blo1 = new BuyLimitOrder(5000 + 1, currentTimeMillis(), "BTC|USDT", "1", "100000");
        var slo = new IOCSellLimitOrder(5000 + 2, currentTimeMillis(), "BTC|USDT", "1", "100000");

        context.matchingEngines().offer(blo1).get();
        context.matchingEngines().offer(slo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(ZERO, blo1.get_remaining());
        assertEquals(ZERO, slo.get_remaining());
        assertEquals(0, orderBook.getBids().size());

        sleep(500);
        assertTrue(tradeExistsAndIsValid(new Trade(5000 + 1, 5000 + 2, "BTC|USDT", "1", "100000", "100000", "bor:0;sor:0", currentTimeMillis())));
    }

    @Test
    public void testIOCEqualityMatching2() throws Exception {
        var blo1 = new BuyLimitOrder(5000 + 1, currentTimeMillis(), "BTC|USDT", "1", "100000");
        var slo = new IOCSellLimitOrder(5000 + 2, currentTimeMillis(), "BTC|USDT", "1", "110000");

        context.matchingEngines().offer(blo1).get();
        context.matchingEngines().offer(slo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(1, orderBook.getBids().size());
    }

    @Test
    public void testIOCGreaterThanMatching() throws Exception {
        var blo1 = new BuyLimitOrder(5000 + 3, currentTimeMillis(), "BTC|USDT", "1", "100000");
        var slo = new IOCSellLimitOrder(5000 + 4, currentTimeMillis(), "BTC|USDT", "2", "100000");

        context.matchingEngines().offer(blo1).get();
        context.matchingEngines().offer(slo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(ZERO, blo1.get_remaining());
        assertEquals(new BigDecimal("1"), slo.get_remaining());
        assertEquals(0, orderBook.getBids().size());

        sleep(500);
        tradeExistsAndIsValid(new Trade(5000 + 3, 5000 + 4, "BTC|USDT", "1", "100000", "100000", "bor:0;sor:1", currentTimeMillis()));
    }

    @Test
    public void testIOCLessThanMatching() throws Exception {
        var blo1 = new BuyLimitOrder(5000 + 5, currentTimeMillis(), "BTC|USDT", "2", "100000");
        var slo = new IOCSellLimitOrder(5000 + 6, currentTimeMillis(), "BTC|USDT", "1", "100000");

        context.matchingEngines().offer(blo1).get();
        context.matchingEngines().offer(slo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(new BigDecimal("1"), blo1.get_remaining());
        assertEquals(ZERO, slo.get_remaining());
        assertEquals(1, orderBook.getBids().size());

        sleep(500);
        tradeExistsAndIsValid(new Trade(5000 + 5, 5000 + 6, "BTC|USDT", "1", "100000", "100000", "bor:1;sor:0", currentTimeMillis()));
    }

    @Test
    public void testIOCForLoop() throws Exception {
        var blo1 = new BuyLimitOrder(5000 + 7, currentTimeMillis(), "BTC|USDT", "1", "100004");
        var blo2 = new BuyLimitOrder(5000 + 8, currentTimeMillis(), "BTC|USDT", "1.25", "100003");
        var blo3 = new BuyLimitOrder(5000 + 9, currentTimeMillis(), "BTC|USDT", "2", "100002");
        var blo4 = new BuyLimitOrder(5000 + 10, currentTimeMillis(), "BTC|USDT", "4.75", "100001");
        var blo5 = new BuyLimitOrder(5000 + 11, currentTimeMillis(), "BTC|USDT", "5", "100000");
        var slo = new IOCSellLimitOrder(5000 + 12, currentTimeMillis(), "BTC|USDT", "10", "100000");

        context.matchingEngines().offer(blo1).get();
        context.matchingEngines().offer(blo2).get();
        context.matchingEngines().offer(blo3).get();
        context.matchingEngines().offer(blo4).get();
        context.matchingEngines().offer(blo5).get();
        context.matchingEngines().offer(slo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(ZERO, blo1.get_remaining());
        assertEquals(ZERO, blo2.get_remaining());
        assertEquals(ZERO, blo3.get_remaining());
        assertEquals(ZERO, blo4.get_remaining());
        assertEquals(new BigDecimal("4.00"), blo5.get_remaining());
        assertEquals(ZERO, slo.get_remaining());
        assertEquals(1, orderBook.getBids().size());

        sleep(500);
        assertTrue(tradeExistsAndIsValid(new Trade(5000 + 7, 5000 + 12, "BTC|USDT", "1", "100004", "100000", "bor:0;sor:9", currentTimeMillis())));
        assertTrue(tradeExistsAndIsValid(new Trade(5000 + 8, 5000 + 12, "BTC|USDT", "1.25", "100003", "100000", "bor:0;sor:7.75", currentTimeMillis())));
        assertTrue(tradeExistsAndIsValid(new Trade(5000 + 9, 5000 + 12, "BTC|USDT", "2", "100002", "100000", "bor:0;sor:5.75", currentTimeMillis())));
        assertTrue(tradeExistsAndIsValid(new Trade(5000 + 10, 5000 + 12, "BTC|USDT", "4.75", "100001", "100000", "bor:0;sor:1", currentTimeMillis())));
        assertTrue(tradeExistsAndIsValid(new Trade(5000 + 11, 5000 + 12, "BTC|USDT", "1.00", "100000", "100000", "bor:4;sor:0", currentTimeMillis())));
    }

    @Test
    public void testFOKEqualityMatching() throws Exception {
        var blo1 = new BuyLimitOrder(5000 + 13, currentTimeMillis(), "BTC|USDT", "1", "100000");
        var slo = new FOKSellLimitOrder(5000 + 14, currentTimeMillis(), "BTC|USDT", "1", "100000");

        context.matchingEngines().offer(blo1).get();
        context.matchingEngines().offer(slo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(ZERO, blo1.get_remaining());
        assertEquals(ZERO, slo.get_remaining());
        assertEquals(0, orderBook.getBids().size());

        sleep(500);
        assertTrue(tradeExistsAndIsValid(new Trade(5000 + 13, 5000 + 14, "BTC|USDT", "1", "100000", "100000", "bor:0;sor:0", currentTimeMillis())));
    }

    @Test
    public void testFOKEqualityMatching2() throws Exception {
        var blo1 = new BuyLimitOrder(5000 + 13, currentTimeMillis(), "BTC|USDT", "1", "100000");
        var slo = new FOKSellLimitOrder(5000 + 14, currentTimeMillis(), "BTC|USDT", "1", "110000");

        context.matchingEngines().offer(blo1).get();
        context.matchingEngines().offer(slo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(1, orderBook.getBids().size());
    }

    @Test
    public void testFOKGreaterThanMatching() throws Exception {
        var blo1 = new BuyLimitOrder(5000 + 15, currentTimeMillis(), "BTC|USDT", "1", "100000");
        var slo = new FOKSellLimitOrder(5000 + 16, currentTimeMillis(), "BTC|USDT", "2", "100000");

        context.matchingEngines().offer(blo1).get();
        context.matchingEngines().offer(slo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(1, orderBook.getBids().size());
    }

    @Test
    public void testFOKLessThanMatching() throws Exception {
        var blo1 = new BuyLimitOrder(5000 + 17, currentTimeMillis(), "BTC|USDT", "2", "100000");
        var slo = new FOKSellLimitOrder(5000 + 18, currentTimeMillis(), "BTC|USDT", "1", "100000");

        context.matchingEngines().offer(blo1).get();
        context.matchingEngines().offer(slo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(new BigDecimal("1"), blo1.get_remaining());
        assertEquals(ZERO, slo.get_remaining());
        assertEquals(1, orderBook.getBids().size());

        sleep(500);
        tradeExistsAndIsValid(new Trade(5000 + 17, 5000 + 18, "BTC|USDT", "1", "100000", "100000", "bor:1;sor:0", currentTimeMillis()));
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
