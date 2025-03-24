package software.openex.oms.matching;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import software.openex.oms.binary.order.BuyLimitOrder;
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
public class MatcherTest {
    private static final PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:16");
    private volatile AppContext context;

    @Test
    public void testEqualityMatching1() throws Exception {
        var blo = new BuyLimitOrder(1000 + 1, currentTimeMillis(), "BTC|USDT", "1", "100000");
        var slo = new SellLimitOrder(1000 + 2, currentTimeMillis(), "BTC|USDT", "1", "100000");

        context.matchingEngines().offer(blo).get();
        context.matchingEngines().offer(slo).get();

        sleep(500);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(ZERO, blo.get_remaining());
        assertEquals(ZERO, slo.get_remaining());
        assertEquals(0, orderBook.getBids().size());
        assertEquals(0, orderBook.getAsks().size());

        sleep(500);
        assertTrue(tradeExistsAndIsValid(new Trade(1000 + 1, 1000 + 2, "BTC|USDT", "1", "100000", "100000", "bor:0;sor:0", currentTimeMillis())));
    }

    @Test
    public void testEqualityMatching2() throws Exception {
        var blo = new BuyLimitOrder(1000 + 3, currentTimeMillis(), "BTC|USDT", "1", "99000");
        var slo = new SellLimitOrder(1000 + 4, currentTimeMillis(), "BTC|USDT", "1", "100000");

        context.matchingEngines().offer(blo).get();
        context.matchingEngines().offer(slo).get();

        sleep(500);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(new BigDecimal("1"), blo.get_remaining());
        assertEquals(new BigDecimal("1"), slo.get_remaining());
        assertEquals(1, orderBook.getBids().size());
        assertEquals(1, orderBook.getAsks().size());
    }

    @Test
    public void testGreaterThanMatching1() throws Exception {
        var blo = new BuyLimitOrder(1000 + 5, currentTimeMillis(), "BTC|USDT", "2", "101000");
        var slo = new SellLimitOrder(1000 + 6, currentTimeMillis(), "BTC|USDT", "1", "100000");

        context.matchingEngines().offer(blo).get();
        context.matchingEngines().offer(slo).get();

        sleep(500);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(new BigDecimal("1"), blo.get_remaining());
        assertEquals(ZERO, slo.get_remaining());
        assertEquals(1, orderBook.getBids().size());
        assertEquals(0, orderBook.getAsks().size());

        sleep(500);
        assertTrue(tradeExistsAndIsValid(new Trade(1000 + 5, 1000 + 6, "BTC|USDT", "1", "101000", "100000", "bor:1;sor:0", currentTimeMillis())));
    }

    @Test
    public void testGreaterThanMatching2() throws Exception {
        var blo = new BuyLimitOrder(1000 + 7, currentTimeMillis(), "BTC|USDT", "2", "99000");
        var slo = new SellLimitOrder(1000 + 8, currentTimeMillis(), "BTC|USDT", "1", "100000");

        context.matchingEngines().offer(blo).get();
        context.matchingEngines().offer(slo).get();

        sleep(500);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(new BigDecimal("2"), blo.get_remaining());
        assertEquals(new BigDecimal("1"), slo.get_remaining());
        assertEquals(1, orderBook.getBids().size());
        assertEquals(1, orderBook.getAsks().size());
    }

    @Test
    public void testLessThanMatching1() throws Exception {
        var blo = new BuyLimitOrder(1000 + 9, currentTimeMillis(), "BTC|USDT", "1", "101000");
        var slo = new SellLimitOrder(1000 + 10, currentTimeMillis(), "BTC|USDT", "2", "100000");

        context.matchingEngines().offer(blo).get();
        context.matchingEngines().offer(slo).get();

        sleep(500);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(ZERO, blo.get_remaining());
        assertEquals(new BigDecimal("1"), slo.get_remaining());
        assertEquals(0, orderBook.getBids().size());
        assertEquals(1, orderBook.getAsks().size());

        sleep(500);
        assertTrue(tradeExistsAndIsValid(new Trade(1000 + 9, 1000 + 10, "BTC|USDT", "1", "101000", "100000", "bor:0;sor:1", currentTimeMillis())));
    }

    @Test
    public void testLessThanMatching2() throws Exception {
        var blo = new BuyLimitOrder(1000 + 11, currentTimeMillis(), "BTC|USDT", "1", "99000");
        var slo = new SellLimitOrder(1000 + 12, currentTimeMillis(), "BTC|USDT", "2", "100000");

        context.matchingEngines().offer(blo).get();
        context.matchingEngines().offer(slo).get();

        sleep(500);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(new BigDecimal("1"), blo.get_remaining());
        assertEquals(new BigDecimal("2"), slo.get_remaining());
        assertEquals(1, orderBook.getBids().size());
        assertEquals(1, orderBook.getAsks().size());
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
