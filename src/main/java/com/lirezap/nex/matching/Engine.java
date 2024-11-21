package com.lirezap.nex.matching;

import com.lirezap.nex.binary.order.BuyOrder;
import com.lirezap.nex.binary.order.SellOrder;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static java.time.Duration.ofSeconds;
import static java.util.Comparator.reverseOrder;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A buy/sell orders matching engine.
 *
 * @author Alireza Pourtaghi
 */
public final class Engine implements Closeable {
    private static final Logger logger = getLogger(Engine.class);

    private final ExecutorService executor;
    private final PriorityQueue<BuyOrder> buyOrders;
    private final PriorityQueue<SellOrder> sellOrders;
    private final Matcher matcher;

    public Engine(final int initialCapacity) {
        this.executor = newSingleThreadExecutor();
        this.buyOrders = new PriorityQueue<>(initialCapacity, reverseOrder());
        this.sellOrders = new PriorityQueue<>(initialCapacity, reverseOrder());
        this.matcher = new Matcher(this.executor, this.buyOrders, this.sellOrders);

        match();
    }

    private void match() {
        executor.submit(matcher);
    }

    public CompletableFuture<Void> offer(final BuyOrder order) {
        final var future = new CompletableFuture<Void>();
        executor.submit(() -> {
            if (buyOrders.offer(order)) {
                future.complete(null);
                logger.trace("offer: buy: {}", order);
            } else {
                future.completeExceptionally(new RuntimeException("could not insert buy order into queue!"));
            }
        });

        return future;
    }

    public CompletableFuture<Void> offer(final SellOrder order) {
        final var future = new CompletableFuture<Void>();
        executor.submit(() -> {
            if (sellOrders.offer(order)) {
                future.complete(null);
                logger.trace("offer: sell: {}", order);
            } else {
                future.completeExceptionally(new RuntimeException("could not insert sell order into queue!"));
            }
        });

        return future;
    }

    @Override
    public void close() throws IOException {
        logger.info("Closing matching engine ...");

        try {
            executor.shutdown();
            final var timeout = ofSeconds(60);
            if (!executor.awaitTermination(timeout.toSeconds(), SECONDS)) {
                // Safe to ignore runnable list!
                executor.shutdownNow();
            }
        } catch (Exception ex) {
            logger.error("{}", ex.getMessage());
        }
    }
}
