/*
 * ISC License
 *
 * Copyright (c) 2025, Alireza Pourtaghi <lirezap@protonmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package software.openex.oms.net;

import static software.openex.oms.binary.BinaryRepresentable.*;
import static software.openex.oms.context.AppContext.context;
import static software.openex.oms.net.ErrorMessages.*;

/**
 * Dispatcher implementation that dispatches incoming messages to appropriate handlers.
 *
 * @author Alireza Pourtaghi
 */
public final class Dispatcher implements Responder {
    private static final Handlers handlers = new Handlers();

    public void dispatch(final Connection connection) {
        if (isValid(connection)) {
            context().executors().worker().execute(() -> {
                switch (id(connection.segment())) {
                    case 101 -> handlers.handleBuyLimitOrder(connection);
                    case 102 -> handlers.handleSellLimitOrder(connection);
                    case 104 -> handlers.handleCancelOrder(connection);
                    case 105 -> handlers.handleFetchOrderBook(connection);
                    case 107 -> handlers.handleBuyMarketOrder(connection);
                    case 108 -> handlers.handleSellMarketOrder(connection);

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
