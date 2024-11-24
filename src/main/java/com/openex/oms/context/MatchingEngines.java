package com.openex.oms.context;

import com.openex.oms.binary.order.BuyOrder;
import com.openex.oms.binary.order.SellOrder;
import com.openex.oms.matching.Engine;
import org.slf4j.Logger;

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

    public MatchingEngines() {
        this.engines = new ConcurrentHashMap<>();
    }

    public CompletableFuture<Void> offer(final BuyOrder order) {
        // TODO: Should we use hard code initial cap?
        return engines.computeIfAbsent(order.getSymbol(), symbol -> new Engine(symbol, 1000))
                .offer(order);
    }

    public CompletableFuture<Void> offer(final SellOrder order) {
        // TODO: Should we use hard code initial cap?
        return engines.computeIfAbsent(order.getSymbol(), symbol -> new Engine(symbol, 1000))
                .offer(order);
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
