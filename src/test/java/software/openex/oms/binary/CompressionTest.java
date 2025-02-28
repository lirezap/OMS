package software.openex.oms.binary;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import software.openex.oms.binary.trade.Trade;
import software.openex.oms.binary.trade.TradeBinaryRepresentation;
import software.openex.oms.context.AppContext;

import static java.lang.System.currentTimeMillis;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.*;
import static software.openex.oms.binary.BinaryRepresentable.RHS;
import static software.openex.oms.binary.BinaryRepresentable.size;
import static software.openex.oms.context.AppContext.contextTest;

/**
 * @author Alireza Pourtaghi
 */
public class CompressionTest {
    private static final PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:16");
    private volatile AppContext context;

    @Test
    public void testCompression() throws Throwable {
        var trade = new Trade(1, 2, "BTC/USDT", "1.1111111111", "5.111", "5.111", "a:b;c:d;e:f;g:h;i:j;k:l;m:n", currentTimeMillis());
        try (var binaryRepresentation = new TradeBinaryRepresentation(trade)) {
            binaryRepresentation.encodeV1();
            var segment = binaryRepresentation.compressLZ4(context.compression());

            assertFalse(binaryRepresentation.isCompressed(binaryRepresentation.segment()));
            assertTrue(binaryRepresentation.isCompressed(segment));

            var decompressionSize = context.compression().lz4()
                    .decompressSafe(segment.asSlice(RHS), binaryRepresentation.segment().asSlice(RHS), size(segment), binaryRepresentation.size());

            assertEquals(binaryRepresentation.size(), decompressionSize);

            var decoded = TradeBinaryRepresentation.decode(binaryRepresentation.segment());
            assertEquals(trade.getBuyOrderId(), decoded.getBuyOrderId());
            assertEquals(trade.getSellOrderId(), decoded.getSellOrderId());
            assertEquals(trade.getSymbol(), decoded.getSymbol());
            assertEquals(trade.getQuantity(), decoded.getQuantity());
            assertEquals(trade.getBuyPrice(), decoded.getBuyPrice());
            assertEquals(trade.getSellPrice(), decoded.getSellPrice());
            assertEquals(trade.getMetadata(), decoded.getMetadata());
            assertEquals(trade.getTs(), decoded.getTs());
        }
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
