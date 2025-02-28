package software.openex.oms.context;

import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import software.openex.oms.binary.order.BuyLimitOrder;
import software.openex.oms.binary.order.BuyMarketOrder;
import software.openex.oms.binary.order.SellLimitOrder;
import software.openex.oms.binary.order.SellMarketOrder;
import software.openex.oms.binary.trade.Trade;

import java.time.Instant;

import static java.lang.System.currentTimeMillis;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.*;
import static software.openex.oms.context.AppContext.contextTest;
import static software.openex.oms.models.Tables.TRADE;
import static software.openex.oms.models.enums.OrderMessageSide.BUY;
import static software.openex.oms.models.enums.OrderMessageSide.SELL;
import static software.openex.oms.models.enums.OrderMessageState.*;
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

    @Test
    public void testInsertTrade() {
        var trade = new Trade(1, 2, "BTC/USDT", "1", "100000", "100000", "bor:0;sor:0", currentTimeMillis());
        context.dataBase().insertTrade(context.dataBase().postgresql(), trade);
        assertTrue(tradeExistsAndIsMatched(trade));
    }

    @Test
    public void testCancelOrder() {
        var buyLimitOrder = new BuyLimitOrder(6, currentTimeMillis(), "BTC|USDT", "1", "100000");
        context.dataBase().insertLimitOrder(buyLimitOrder, BUY);
        context.dataBase().cancelOrder(context.dataBase().postgresql(), buyLimitOrder);

        var recordFetched = context.dataBase().fetchOrderMessage(6, "BTC|USDT");
        assertEquals(CANCELED, recordFetched.component8());
    }

    @Test
    public void testExecuteOrder() {
        var buyLimitOrder = new BuyLimitOrder(7, currentTimeMillis(), "BTC|USDT", "1", "100000");
        context.dataBase().insertLimitOrder(buyLimitOrder, BUY);
        context.dataBase().executeOrder(context.dataBase().postgresql(), buyLimitOrder.getId(), "BTC|USDT", "0.5");

        var recordFetched = context.dataBase().fetchOrderMessage(7, "BTC|USDT");
        assertEquals("0.5", recordFetched.component7());
        assertEquals(EXECUTED, recordFetched.component8());
    }

    @Test
    public void testUpdateRemaining() {
        var buyLimitOrder = new BuyLimitOrder(8, currentTimeMillis(), "BTC|USDT", "1", "100000");
        context.dataBase().insertLimitOrder(buyLimitOrder, BUY);
        context.dataBase().updateRemaining(context.dataBase().postgresql(), buyLimitOrder.getId(), "BTC|USDT", "0.1");

        var recordFetched = context.dataBase().fetchOrderMessage(8, "BTC|USDT");
        assertEquals("0.1", recordFetched.component7());
    }

    private boolean tradeExistsAndIsMatched(final Trade trade) {
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
                && recordFetched.component8().equals(Instant.ofEpochMilli(trade.getTs()));
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
