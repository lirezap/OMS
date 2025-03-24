package software.openex.oms.matching;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import software.openex.oms.binary.order.BuyLimitOrder;
import software.openex.oms.binary.order.SellMarketOrder;
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
public class SellMarketOrderMatcherTest {
    private static final PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:16");
    private volatile AppContext context;

    @Test
    public void testEqualityMatching() throws Exception {
        var blo1 = new BuyLimitOrder(3000 + 1, currentTimeMillis(), "BTC|USDT", "1", "100000");
        var smo = new SellMarketOrder(3000 + 2, currentTimeMillis(), "BTC|USDT", "1");

        context.matchingEngines().offer(blo1).get();
        context.matchingEngines().offer(smo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(ZERO, blo1.get_remaining());
        assertEquals(ZERO, smo.get_remaining());
        assertEquals(0, orderBook.getBids().size());

        sleep(500);
        assertTrue(tradeExistsAndIsValid(new Trade(3000 + 1, 3000 + 2, "BTC|USDT", "1", "100000", "0", "bor:0;sor:0", currentTimeMillis())));
    }

    @Test
    public void testGreaterThanMatching() throws Exception {
        var blo1 = new BuyLimitOrder(3000 + 3, currentTimeMillis(), "BTC|USDT", "1", "100000");
        var smo = new SellMarketOrder(3000 + 4, currentTimeMillis(), "BTC|USDT", "2");

        context.matchingEngines().offer(blo1).get();
        context.matchingEngines().offer(smo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(ZERO, blo1.get_remaining());
        assertEquals(new BigDecimal("1"), smo.get_remaining());
        assertEquals(0, orderBook.getBids().size());

        sleep(500);
        tradeExistsAndIsValid(new Trade(3000 + 3, 3000 + 4, "BTC|USDT", "1", "100000", "0", "bor:0;sor:1", currentTimeMillis()));
    }

    @Test
    public void testLessThanMatching() throws Exception {
        var blo1 = new BuyLimitOrder(3000 + 5, currentTimeMillis(), "BTC|USDT", "2", "100000");
        var smo = new SellMarketOrder(3000 + 6, currentTimeMillis(), "BTC|USDT", "1");

        context.matchingEngines().offer(blo1).get();
        context.matchingEngines().offer(smo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(new BigDecimal("1"), blo1.get_remaining());
        assertEquals(ZERO, smo.get_remaining());
        assertEquals(1, orderBook.getBids().size());

        sleep(500);
        tradeExistsAndIsValid(new Trade(3000 + 5, 3000 + 6, "BTC|USDT", "1", "100000", "0", "bor:1;sor:0", currentTimeMillis()));
    }

    @Test
    public void testForLoop() throws Exception {
        var blo1 = new BuyLimitOrder(3000 + 7, currentTimeMillis(), "BTC|USDT", "1", "100004");
        var blo2 = new BuyLimitOrder(3000 + 8, currentTimeMillis(), "BTC|USDT", "1.25", "100003");
        var blo3 = new BuyLimitOrder(3000 + 9, currentTimeMillis(), "BTC|USDT", "2", "100002");
        var blo4 = new BuyLimitOrder(3000 + 10, currentTimeMillis(), "BTC|USDT", "4.75", "100001");
        var blo5 = new BuyLimitOrder(3000 + 11, currentTimeMillis(), "BTC|USDT", "5", "100000");
        var smo = new SellMarketOrder(3000 + 12, currentTimeMillis(), "BTC|USDT", "10");

        context.matchingEngines().offer(blo1).get();
        context.matchingEngines().offer(blo2).get();
        context.matchingEngines().offer(blo3).get();
        context.matchingEngines().offer(blo4).get();
        context.matchingEngines().offer(blo5).get();
        context.matchingEngines().offer(smo);

        sleep(100);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(ZERO, blo1.get_remaining());
        assertEquals(ZERO, blo2.get_remaining());
        assertEquals(ZERO, blo3.get_remaining());
        assertEquals(ZERO, blo4.get_remaining());
        assertEquals(new BigDecimal("4.00"), blo5.get_remaining());
        assertEquals(ZERO, smo.get_remaining());
        assertEquals(1, orderBook.getBids().size());

        sleep(500);
        assertTrue(tradeExistsAndIsValid(new Trade(3000 + 7, 3000 + 12, "BTC|USDT", "1", "100004", "0", "bor:0;sor:9", currentTimeMillis())));
        assertTrue(tradeExistsAndIsValid(new Trade(3000 + 8, 3000 + 12, "BTC|USDT", "1.25", "100003", "0", "bor:0;sor:7.75", currentTimeMillis())));
        assertTrue(tradeExistsAndIsValid(new Trade(3000 + 9, 3000 + 12, "BTC|USDT", "2", "100002", "0", "bor:0;sor:5.75", currentTimeMillis())));
        assertTrue(tradeExistsAndIsValid(new Trade(3000 + 10, 3000 + 12, "BTC|USDT", "4.75", "100001", "0", "bor:0;sor:1", currentTimeMillis())));
        assertTrue(tradeExistsAndIsValid(new Trade(3000 + 11, 3000 + 12, "BTC|USDT", "1.00", "100000", "0", "bor:4;sor:0", currentTimeMillis())));
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
