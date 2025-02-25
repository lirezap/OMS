package software.openex.oms.data;

import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import software.openex.oms.binary.order.BuyLimitOrder;
import software.openex.oms.binary.order.BuyMarketOrder;
import software.openex.oms.binary.order.SellLimitOrder;
import software.openex.oms.binary.order.SellMarketOrder;
import software.openex.oms.context.AppContext;

import static java.lang.System.currentTimeMillis;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.*;
import static software.openex.oms.context.AppContext.contextTest;
import static software.openex.oms.models.enums.OrderMessageSide.BUY;
import static software.openex.oms.models.enums.OrderMessageSide.SELL;
import static software.openex.oms.models.enums.OrderMessageState.ACTIVE;
import static software.openex.oms.models.enums.OrderMessageType.LIMIT;
import static software.openex.oms.models.enums.OrderMessageType.MARKET;

/**
 * @author Alireza Pourtaghi
 */
public class DatabaseTest {
    private static final PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:16");
    private static AppContext context;

    @Test
    public void testInsertLimitOrder() {
        var buyLimitOrder = new BuyLimitOrder(1, currentTimeMillis(), "BTC|USDT", "1", "100000");
        assertTrue(context.dataBase().insertLimitOrder(buyLimitOrder, BUY));

        var sellLimitOrder = new SellLimitOrder(2, currentTimeMillis(), "BTC|USDT", "1", "100000");
        assertTrue(context.dataBase().insertLimitOrder(sellLimitOrder, SELL));

        var recordFetched = context.dataBase().fetchOrderMessage(1, "BTC|USDT");
        assertNotNull(recordFetched);
        assertEquals(1, recordFetched.component1());
        assertEquals("BTC|USDT", recordFetched.component2());
        assertEquals(BUY, recordFetched.component3());
        assertEquals(LIMIT, recordFetched.component4());
        assertEquals("1", recordFetched.component5());
        assertEquals("100000", recordFetched.component6());
        assertEquals("1", recordFetched.component7());
        assertEquals(ACTIVE, recordFetched.component8());

        recordFetched = context.dataBase().fetchOrderMessage(2, "BTC|USDT");
        assertNotNull(recordFetched);
        assertEquals(SELL, recordFetched.component3());
    }

    @Test
    public void testInsertMarketOrder() {
        var buyMarketOrder = new BuyMarketOrder(3, currentTimeMillis(), "BTC|USDT", "1");
        assertTrue(context.dataBase().insertMarketOrder(buyMarketOrder, BUY));

        var sellMarketOrder = new SellMarketOrder(4, currentTimeMillis(), "BTC|USDT", "1");
        assertTrue(context.dataBase().insertMarketOrder(sellMarketOrder, SELL));

        var recordFetched = context.dataBase().fetchOrderMessage(3, "BTC|USDT");
        assertNotNull(recordFetched);
        assertEquals(3, recordFetched.component1());
        assertEquals("BTC|USDT", recordFetched.component2());
        assertEquals(BUY, recordFetched.component3());
        assertEquals(MARKET, recordFetched.component4());
        assertEquals("1", recordFetched.component5());
        assertNull(recordFetched.component6());
        assertEquals("1", recordFetched.component7());
        assertEquals(ACTIVE, recordFetched.component8());

        recordFetched = context.dataBase().fetchOrderMessage(4, "BTC|USDT");
        assertNotNull(recordFetched);
        assertEquals(SELL, recordFetched.component3());
    }

    @Test
    public void testOrderMessageAlreadyExists() {
        context.dataBase().insertLimitOrder(new BuyLimitOrder(5, currentTimeMillis(), "BTC|USDT", "1", "100000"), BUY);

        var buyLimitOrder = new BuyLimitOrder(5, currentTimeMillis(), "BTC|USDT", "1", "100000");
        assertThrows(DataAccessException.class, () -> context.dataBase().insertLimitOrder(buyLimitOrder, BUY));
    }

    @BeforeAll
    public static void setup() {
        // Start postgresql container.
        postgresql.setPortBindings(of("127.0.0.1:5432:5432"));
        postgresql.withDatabaseName("oms");
        postgresql.withUsername("oms");
        postgresql.withPassword("oms");
        postgresql.start();

        // Start/Replace application context.
        context = contextTest();
        context.databaseMigrator().migrate();
    }

    @AfterAll
    public static void stop() {
        // Stop application context.
        context.close();

        // Stop postgresql container.
        postgresql.stop();
    }
}
