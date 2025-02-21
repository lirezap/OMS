package software.openex.oms.matching;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import software.openex.oms.binary.order.BuyLimitOrder;
import software.openex.oms.binary.order.BuyMarketOrder;
import software.openex.oms.binary.order.SellLimitOrder;
import software.openex.oms.binary.order.SellMarketOrder;
import software.openex.oms.binary.order.book.FetchOrderBook;
import software.openex.oms.context.AppContext;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.openex.oms.context.AppContext.contextTest;

/**
 * @author Alireza Pourtaghi
 */
public class MarketOrdersTest {
    private static final PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:16");
    private volatile AppContext context;

    @Test
    public void testBuyMarketOrderOffer() throws Exception {
        context.matchingEngines()
                .offer(new SellLimitOrder(1, currentTimeMillis() + 1, "BTC|USDT", "1", "100000")).get();
        context.matchingEngines()
                .offer(new SellLimitOrder(2, currentTimeMillis() + 2, "BTC|USDT", "2", "100000")).get();
        context.matchingEngines()
                .offer(new SellLimitOrder(3, currentTimeMillis() + 3, "BTC|USDT", "3", "100000")).get();

        var bmo1 = new BuyMarketOrder(4, currentTimeMillis() + 4, "BTC|USDT", "1");
        var bmo2 = new BuyMarketOrder(5, currentTimeMillis() + 5, "BTC|USDT", "0.5");

        context.matchingEngines().offer(bmo1);
        context.matchingEngines().offer(bmo2);

        sleep(500);
        assertEquals("0", bmo1.get_remaining().toPlainString());
        assertEquals("0", bmo2.get_remaining().toPlainString());

        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(2, orderBook.getAsks().size());
        assertEquals("1.5", orderBook.getAsks().get(0).get_remaining().toPlainString());
        assertEquals("3", orderBook.getAsks().get(1).get_remaining().toPlainString());
    }

    @Test
    public void testSellMarketOrderOffer() throws Exception {
        context.matchingEngines()
                .offer(new BuyLimitOrder(1, currentTimeMillis() + 1, "BTC|USDT", "1", "100000")).get();
        context.matchingEngines()
                .offer(new BuyLimitOrder(2, currentTimeMillis() + 2, "BTC|USDT", "2", "100000")).get();
        context.matchingEngines()
                .offer(new BuyLimitOrder(3, currentTimeMillis() + 3, "BTC|USDT", "3", "100000")).get();

        var smo1 = new SellMarketOrder(4, currentTimeMillis() + 4, "BTC|USDT", "1");
        var smo2 = new SellMarketOrder(5, currentTimeMillis() + 5, "BTC|USDT", "0.5");

        context.matchingEngines().offer(smo1);
        context.matchingEngines().offer(smo2);

        sleep(500);
        assertEquals("0", smo1.get_remaining().toPlainString());
        assertEquals("0", smo2.get_remaining().toPlainString());

        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(2, orderBook.getBids().size());
        assertEquals("1.5", orderBook.getBids().get(0).get_remaining().toPlainString());
        assertEquals("3", orderBook.getBids().get(1).get_remaining().toPlainString());
    }

    @Test
    public void testMarketOrderRemaining() throws Exception {
        context.matchingEngines()
                .offer(new BuyLimitOrder(1, currentTimeMillis() + 1, "BTC|USDT", "1", "100000")).get();
        context.matchingEngines()
                .offer(new BuyLimitOrder(2, currentTimeMillis() + 2, "BTC|USDT", "2", "100000")).get();
        context.matchingEngines()
                .offer(new BuyLimitOrder(3, currentTimeMillis() + 3, "BTC|USDT", "3", "100000")).get();

        var smo1 = new SellMarketOrder(4, currentTimeMillis() + 4, "BTC|USDT", "10.1111111111");
        context.matchingEngines().offer(smo1);

        sleep(500);
        assertEquals("4.1111111111", smo1.get_remaining().toPlainString());

        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC|USDT", 10)).get();

        assertEquals(0, orderBook.getBids().size());
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
