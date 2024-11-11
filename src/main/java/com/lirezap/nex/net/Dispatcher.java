package com.lirezap.nex.net;

import static com.lirezap.nex.binary.BinaryRepresentable.*;
import static com.lirezap.nex.context.AppContext.context;
import static com.lirezap.nex.net.ErrorMessages.*;

/**
 * Dispatcher implementation that dispatches incoming messages to appropriate handlers.
 *
 * @author Alireza Pourtaghi
 */
public final class Dispatcher implements Responder {
    private static final Handlers handlers = new Handlers();

    public void dispatch(final Connection connection) {
        if (isValid(connection)) {
            context().executors().worker().submit(() -> {
                switch (id(connection.segment())) {
                    case 101 -> handlers.handleBuyOrder(connection);
                    case 102 -> handlers.handleSellOrder(connection);

                    default -> write(connection, MESSAGE_NOT_SUPPORTED);
                }
            });
        }
    }

    private boolean isValid(final Connection connection) {
        if (connection.buffer().limit() <= RHS) {
            write(connection, MESSAGE_FORMAT_NOT_VALID);
            return false;
        }

        if (version(connection.segment()) != 1) {
            write(connection, MESSAGE_VERSION_NOT_SUPPORTED);
            return false;
        }

        if (size(connection.segment()) != (connection.buffer().limit() - RHS)) {
            write(connection, MESSAGE_SIZE_NOT_VALID);
            return false;
        }

        return true;
    }
}
