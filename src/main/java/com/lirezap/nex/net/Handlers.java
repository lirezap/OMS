package com.lirezap.nex.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.lirezap.nex.context.AppContext.context;

/**
 * Methods that handle incoming messages, dispatched by dispatcher.
 *
 * @author Alireza Pourtaghi
 */
public final class Handlers {
    private static final Logger logger = LoggerFactory.getLogger(Handlers.class);
    private static final Runnable doNothing = () -> {};

    public void handleBuyOrder(final Connection connection) {
        // TODO: Validate incoming message.
        // TODO: Check existence.
        logMessage(connection);
        // TODO: Persist incoming message.
        // TODO: Add into matching engine.
        // TODO: Send back response.
    }

    public void handleSellOrder(final Connection connection) {
        logMessage(connection);
        // TODO: Complete implementation.
    }

    private void logMessage(final Connection connection) {
        context().messagesLogFile().ifPresentOrElse(file ->
                file.append(connection.copyMessage().asByteBuffer()), doNothing);
    }
}
