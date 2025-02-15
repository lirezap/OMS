package software.openex.oms.matching;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import software.openex.oms.binary.order.BuyLimitOrder;
import software.openex.oms.binary.order.SellLimitOrder;
import software.openex.oms.binary.order.book.FetchOrderBook;
import software.openex.oms.context.AppContext;

import static java.lang.System.currentTimeMillis;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.openex.oms.context.AppContext.contextTest;

/**
 * @author Alireza Pourtaghi
 */
public class EngineTest {
    private static final PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:16");
    private AppContext context;

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
