package software.openex.oms.matching;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import software.openex.oms.binary.order.BuyLimitOrder;
import software.openex.oms.binary.order.book.FetchOrderBook;
import software.openex.oms.context.AppContext;

import static java.lang.System.currentTimeMillis;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.openex.oms.context.AppContext.contextSafe;

/**
 * @author Alireza Pourtaghi
 */
public class EngineTest {
    private static final PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:16");
    private static AppContext context;

    @BeforeAll
    public static void setup() {
        // Start postgresql container.
        postgresql.setPortBindings(of("127.0.0.1:5432:5432"));
        postgresql.withDatabaseName("oms");
        postgresql.withUsername("oms");
        postgresql.withPassword("oms");
        postgresql.start();

        // Start application context.
        context = contextSafe();
        context.databaseMigrator().migrate();
        context.matchingEngines().start();
    }

    @AfterAll
    public static void stop() {
        // Stop application context.
        if (context != null) context.close();

        // Stop postgresql container.
        postgresql.stop();
    }

    @Test
    public void testBuyLimitOrderOffer() throws Exception {
        context.matchingEngines()
                .offer(new BuyLimitOrder(1, currentTimeMillis(), "BTC_USDT", "1", "100000")).get();

        var orderBook = context.matchingEngines()
                .orderBook(new FetchOrderBook("BTC_USDT", 10)).get();

        assertEquals(1, orderBook.getBids().size());
        assertEquals(0, orderBook.getAsks().size());
    }
}
