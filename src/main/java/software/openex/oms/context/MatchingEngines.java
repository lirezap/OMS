package software.openex.oms.context;

import org.slf4j.Logger;
import software.openex.oms.binary.order.BuyOrder;
import software.openex.oms.binary.order.CancelOrder;
import software.openex.oms.binary.order.SellOrder;
import software.openex.oms.matching.Engine;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A component that holds all matching engines.
 *
 * @author Alireza Pourtaghi
 */
public final class MatchingEngines implements Closeable {
    private static final Logger logger = getLogger(MatchingEngines.class);

    private final ConcurrentHashMap<String, Engine> engines;
    private final int initialCap;

    public MatchingEngines(final Configuration configuration) {
        this.engines = new ConcurrentHashMap<>();
        this.initialCap = configuration.loadInt("matching.engine.queues_initial_cap");
    }

    public CompletableFuture<Void> offer(final BuyOrder order) {
        return engines.computeIfAbsent(order.getSymbol(), symbol -> new Engine(symbol, initialCap))
                .offer(order);
    }

    public CompletableFuture<Void> offer(final SellOrder order) {
        return engines.computeIfAbsent(order.getSymbol(), symbol -> new Engine(symbol, initialCap))
                .offer(order);
    }

    public CompletableFuture<Boolean> cancel(final CancelOrder order) {
        return engines.computeIfAbsent(order.getSymbol(), symbol -> new Engine(symbol, initialCap))
                .cancel(order);
    }

    @Override
    public void close() throws IOException {
        logger.info("Closing matching engines ...");

        engines.forEach((symbol, engine) -> {
            try {
                engine.close();
            } catch (Exception ex) {
                logger.error("could not close matching engine of symbol: {}", symbol);
            }
        });
    }
}
