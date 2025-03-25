package software.openex.oms.matching;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import software.openex.oms.binary.order.BuyLimitOrder;
import software.openex.oms.binary.order.CancelOrder;
import software.openex.oms.binary.order.SellLimitOrder;
import software.openex.oms.binary.order.book.FetchOrderBook;
import software.openex.oms.context.AppContext;

import java.math.BigDecimal;

import static java.lang.System.currentTimeMillis;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.openex.oms.context.AppContext.contextTest;

/**
 * @author Alireza Pourtaghi
 */
public class LimitOrdersTest {
    private static final PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:16");
    private volatile AppContext context;

    @Test
    public void testBuyLimitOrderOffer() throws Exception {
        context.matchingEngines()
                .offer(new BuyLimitOrder(1, currentTimeMillis(), "BTC|USDT", "1", "100000")).get();

        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(1, orderBook.getBids().size());
        assertEquals(0, orderBook.getAsks().size());
    }

    @Test
    public void testSellLimitOrderOffer() throws Exception {
        context.matchingEngines()
                .offer(new SellLimitOrder(1, currentTimeMillis(), "BTC|USDT", "1", "100000")).get();

        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(0, orderBook.getBids().size());
        assertEquals(1, orderBook.getAsks().size());
    }

    @Test
    public void testBuyLimitOrderCancel() throws Exception {
        context.matchingEngines()
                .offer(new BuyLimitOrder(1, currentTimeMillis(), "BTC|USDT", "1", "100000")).get();
        context.matchingEngines()
                .offer(new BuyLimitOrder(2, currentTimeMillis(), "BTC|USDT", "1", "200000")).get();
        context.matchingEngines()
                .offer(new BuyLimitOrder(3, currentTimeMillis(), "BTC|USDT", "1", "300000")).get();

        var canceled = context.matchingEngines()
                .cancel(new CancelOrder(3, currentTimeMillis(), "BTC|USDT", "1")).get();

        assertEquals(true, canceled);

        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(2, orderBook.getBids().size());

        assertEquals(2, orderBook.getBids().get(0).getId());
        assertEquals(1, orderBook.getBids().get(1).getId());
    }

    @Test
    public void testBuyLimitOrderPartiallyCancel() throws Exception {
        context.matchingEngines()
                .offer(new BuyLimitOrder(1, currentTimeMillis(), "BTC|USDT", "1", "100000")).get();
        context.matchingEngines()
                .offer(new BuyLimitOrder(2, currentTimeMillis(), "BTC|USDT", "1", "200000")).get();
        context.matchingEngines()
                .offer(new BuyLimitOrder(3, currentTimeMillis(), "BTC|USDT", "1", "300000")).get();

        var canceled = context.matchingEngines()
                .cancel(new CancelOrder(3, currentTimeMillis(), "BTC|USDT", "0.51")).get();

        assertEquals(true, canceled);

        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(3, orderBook.getBids().size());

        assertEquals(3, orderBook.getBids().get(0).getId());
        assertEquals(2, orderBook.getBids().get(1).getId());
        assertEquals(1, orderBook.getBids().get(2).getId());

        assertEquals(new BigDecimal("0.49"), orderBook.getBids().get(0).get_remaining());
    }

    @Test
    public void testBuyLimitOrderAllCancel() throws Exception {
        context.matchingEngines()
                .offer(new BuyLimitOrder(1, currentTimeMillis(), "BTC|USDT", "1", "100000")).get();
        context.matchingEngines()
                .offer(new BuyLimitOrder(2, currentTimeMillis(), "BTC|USDT", "1", "200000")).get();
        context.matchingEngines()
                .offer(new BuyLimitOrder(3, currentTimeMillis(), "BTC|USDT", "1", "300000")).get();

        var canceled = context.matchingEngines()
                .cancel(new CancelOrder(3, currentTimeMillis(), "BTC|USDT", "0")).get();

        assertEquals(true, canceled);

        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(2, orderBook.getBids().size());

        assertEquals(2, orderBook.getBids().get(0).getId());
        assertEquals(1, orderBook.getBids().get(1).getId());
    }

    @Test
    public void testSellLimitOrderCancel() throws Exception {
        context.matchingEngines()
                .offer(new SellLimitOrder(1, currentTimeMillis(), "BTC|USDT", "1", "100000")).get();
        context.matchingEngines()
                .offer(new SellLimitOrder(2, currentTimeMillis(), "BTC|USDT", "1", "200000")).get();
        context.matchingEngines()
                .offer(new SellLimitOrder(3, currentTimeMillis(), "BTC|USDT", "1", "300000")).get();

        var canceled = context.matchingEngines()
                .cancel(new CancelOrder(3, currentTimeMillis(), "BTC|USDT", "1")).get();
        assertEquals(true, canceled);

        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(2, orderBook.getAsks().size());

        assertEquals(1, orderBook.getAsks().get(0).getId());
        assertEquals(2, orderBook.getAsks().get(1).getId());
    }

    @Test
    public void testSellLimitOrderPartiallyCancel() throws Exception {
        context.matchingEngines()
                .offer(new SellLimitOrder(1, currentTimeMillis(), "BTC|USDT", "1", "100000")).get();
        context.matchingEngines()
                .offer(new SellLimitOrder(2, currentTimeMillis(), "BTC|USDT", "1", "200000")).get();
        context.matchingEngines()
                .offer(new SellLimitOrder(3, currentTimeMillis(), "BTC|USDT", "1", "300000")).get();

        var canceled = context.matchingEngines()
                .cancel(new CancelOrder(3, currentTimeMillis(), "BTC|USDT", "0.51")).get();
        assertEquals(true, canceled);

        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(3, orderBook.getAsks().size());

        assertEquals(1, orderBook.getAsks().get(0).getId());
        assertEquals(2, orderBook.getAsks().get(1).getId());
        assertEquals(3, orderBook.getAsks().get(2).getId());

        assertEquals(new BigDecimal("0.49"), orderBook.getAsks().get(2).get_remaining());
    }

    @Test
    public void testSellLimitOrderAllCancel() throws Exception {
        context.matchingEngines()
                .offer(new SellLimitOrder(1, currentTimeMillis(), "BTC|USDT", "1", "100000")).get();
        context.matchingEngines()
                .offer(new SellLimitOrder(2, currentTimeMillis(), "BTC|USDT", "1", "200000")).get();
        context.matchingEngines()
                .offer(new SellLimitOrder(3, currentTimeMillis(), "BTC|USDT", "1", "300000")).get();

        var canceled = context.matchingEngines()
                .cancel(new CancelOrder(3, currentTimeMillis(), "BTC|USDT", "0.0")).get();
        assertEquals(true, canceled);

        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(2, orderBook.getAsks().size());

        assertEquals(1, orderBook.getAsks().get(0).getId());
        assertEquals(2, orderBook.getAsks().get(1).getId());
    }

    @Test
    public void testNotFoundCancel() throws Exception {
        context.matchingEngines()
                .offer(new SellLimitOrder(1, currentTimeMillis(), "BTC|USDT", "1", "100000")).get();
        context.matchingEngines()
                .offer(new SellLimitOrder(2, currentTimeMillis(), "BTC|USDT", "1", "200000")).get();
        context.matchingEngines()
                .offer(new SellLimitOrder(3, currentTimeMillis(), "BTC|USDT", "1", "300000")).get();

        var canceled = context.matchingEngines()
                .cancel(new CancelOrder(4, currentTimeMillis(), "BTC|USDT", "1")).get();
        assertEquals(false, canceled);

        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(3, orderBook.getAsks().size());
    }

    @Test
    public void testNotPossiblePartiallyCancel() throws Exception {
        context.matchingEngines()
                .offer(new SellLimitOrder(1, currentTimeMillis(), "BTC|USDT", "1", "100000")).get();
        context.matchingEngines()
                .offer(new SellLimitOrder(2, currentTimeMillis(), "BTC|USDT", "1", "200000")).get();
        context.matchingEngines()
                .offer(new SellLimitOrder(3, currentTimeMillis(), "BTC|USDT", "1", "300000")).get();

        var canceled = context.matchingEngines()
                .cancel(new CancelOrder(3, currentTimeMillis(), "BTC|USDT", "1.0000000001")).get();
        assertEquals(false, canceled);

        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(3, orderBook.getAsks().size());
    }

    @Test
    public void testFetchOrderBook() throws Exception {
        context.matchingEngines()
                .offer(new BuyLimitOrder(1, currentTimeMillis(), "BTC|USDT", "1", "100000")).get();
        context.matchingEngines()
                .offer(new BuyLimitOrder(2, currentTimeMillis(), "BTC|USDT", "1", "200000")).get();
        context.matchingEngines()
                .offer(new BuyLimitOrder(3, currentTimeMillis(), "BTC|USDT", "1", "300000")).get();

        context.matchingEngines()
                .offer(new SellLimitOrder(4, currentTimeMillis(), "BTC|USDT", "1", "400000")).get();
        context.matchingEngines()
                .offer(new SellLimitOrder(5, currentTimeMillis(), "BTC|USDT", "1", "500000")).get();
        context.matchingEngines()
                .offer(new SellLimitOrder(6, currentTimeMillis(), "BTC|USDT", "1", "600000")).get();

        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 0)).get();
        assertEquals(0, orderBook.getBids().size());
        assertEquals(0, orderBook.getAsks().size());

        orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 1)).get();
        assertEquals(1, orderBook.getBids().size());
        assertEquals(1, orderBook.getAsks().size());

        orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 2)).get();
        assertEquals(2, orderBook.getBids().size());
        assertEquals(2, orderBook.getAsks().size());

        orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 3)).get();
        assertEquals(3, orderBook.getBids().size());
        assertEquals(3, orderBook.getAsks().size());

        orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 4)).get();
        assertEquals(3, orderBook.getBids().size());
        assertEquals(3, orderBook.getAsks().size());

        orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 5)).get();
        assertEquals(3, orderBook.getBids().size());
        assertEquals(3, orderBook.getAsks().size());

        assertEquals(3, orderBook.getBids().get(0).getId());
        assertEquals(2, orderBook.getBids().get(1).getId());
        assertEquals(1, orderBook.getBids().get(2).getId());
        assertEquals(4, orderBook.getAsks().get(0).getId());
        assertEquals(5, orderBook.getAsks().get(1).getId());
        assertEquals(6, orderBook.getAsks().get(2).getId());
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
