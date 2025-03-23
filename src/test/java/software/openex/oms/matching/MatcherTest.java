package software.openex.oms.matching;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import software.openex.oms.binary.order.BuyLimitOrder;
import software.openex.oms.binary.order.SellLimitOrder;
import software.openex.oms.binary.order.book.FetchOrderBook;
import software.openex.oms.context.AppContext;

import java.math.BigDecimal;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.math.BigDecimal.ZERO;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.openex.oms.context.AppContext.contextTest;

/**
 * @author Alireza Pourtaghi
 */
public class MatcherTest {
    private static final PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:16");
    private volatile AppContext context;

    @Test
    public void testEqualityMatching1() throws Exception {
        var bol = new BuyLimitOrder(1, currentTimeMillis(), "BTC|USDT", "1", "100000");
        var sol = new SellLimitOrder(2, currentTimeMillis(), "BTC|USDT", "1", "100000");

        context.matchingEngines().offer(bol).get();
        context.matchingEngines().offer(sol).get();

        sleep(500);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(ZERO, bol.get_remaining());
        assertEquals(ZERO, sol.get_remaining());
        assertEquals(0, orderBook.getBids().size());
        assertEquals(0, orderBook.getAsks().size());
    }

    @Test
    public void testEqualityMatching2() throws Exception {
        var bol = new BuyLimitOrder(1, currentTimeMillis(), "BTC|USDT", "1", "99000");
        var sol = new SellLimitOrder(2, currentTimeMillis(), "BTC|USDT", "1", "100000");

        context.matchingEngines().offer(bol).get();
        context.matchingEngines().offer(sol).get();

        sleep(500);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(new BigDecimal("1"), bol.get_remaining());
        assertEquals(new BigDecimal("1"), sol.get_remaining());
        assertEquals(1, orderBook.getBids().size());
        assertEquals(1, orderBook.getAsks().size());
    }

    @Test
    public void testGreaterThanMatching1() throws Exception {
        var bol = new BuyLimitOrder(1, currentTimeMillis(), "BTC|USDT", "2", "101000");
        var sol = new SellLimitOrder(2, currentTimeMillis(), "BTC|USDT", "1", "100000");

        context.matchingEngines().offer(bol).get();
        context.matchingEngines().offer(sol).get();

        sleep(500);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(new BigDecimal("1"), bol.get_remaining());
        assertEquals(ZERO, sol.get_remaining());
        assertEquals(1, orderBook.getBids().size());
        assertEquals(0, orderBook.getAsks().size());
    }

    @Test
    public void testGreaterThanMatching2() throws Exception {
        var bol = new BuyLimitOrder(1, currentTimeMillis(), "BTC|USDT", "2", "99000");
        var sol = new SellLimitOrder(2, currentTimeMillis(), "BTC|USDT", "1", "100000");

        context.matchingEngines().offer(bol).get();
        context.matchingEngines().offer(sol).get();

        sleep(500);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(new BigDecimal("2"), bol.get_remaining());
        assertEquals(new BigDecimal("1"), sol.get_remaining());
        assertEquals(1, orderBook.getBids().size());
        assertEquals(1, orderBook.getAsks().size());
    }

    @Test
    public void testLessThanMatching1() throws Exception {
        var bol = new BuyLimitOrder(1, currentTimeMillis(), "BTC|USDT", "1", "101000");
        var sol = new SellLimitOrder(2, currentTimeMillis(), "BTC|USDT", "2", "100000");

        context.matchingEngines().offer(bol).get();
        context.matchingEngines().offer(sol).get();

        sleep(500);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(ZERO, bol.get_remaining());
        assertEquals(new BigDecimal("1"), sol.get_remaining());
        assertEquals(0, orderBook.getBids().size());
        assertEquals(1, orderBook.getAsks().size());
    }

    @Test
    public void testLessThanMatching2() throws Exception {
        var bol = new BuyLimitOrder(1, currentTimeMillis(), "BTC|USDT", "1", "99000");
        var sol = new SellLimitOrder(2, currentTimeMillis(), "BTC|USDT", "2", "100000");

        context.matchingEngines().offer(bol).get();
        context.matchingEngines().offer(sol).get();

        sleep(500);
        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(new BigDecimal("1"), bol.get_remaining());
        assertEquals(new BigDecimal("2"), sol.get_remaining());
        assertEquals(1, orderBook.getBids().size());
        assertEquals(1, orderBook.getAsks().size());
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
