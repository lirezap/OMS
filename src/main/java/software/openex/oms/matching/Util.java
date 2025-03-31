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
package software.openex.oms.matching;

import software.openex.oms.binary.order.CancelOrder;
import software.openex.oms.binary.order.OrderBinaryRepresentation;
import software.openex.oms.binary.trade.Trade;
import software.openex.oms.binary.trade.TradeBinaryRepresentation;
import software.openex.oms.storage.AtomicFile;

import static java.lang.foreign.Arena.ofConfined;

/**
 * Package level utility class.
 *
 * @author Alireza Pourtaghi
 */
final class Util {

    static void append(final Trade trade, final AtomicFile file) {
        try (final var arena = ofConfined()) {
            final var binary = new TradeBinaryRepresentation(arena, trade);
            binary.encodeV1();

            file.append(binary.segment());
        }
    }

    static void append(final CancelOrder cancelOrder, final AtomicFile file) {
        try (final var arena = ofConfined()) {
            final var binary = new OrderBinaryRepresentation(arena, cancelOrder);
            binary.encodeV1();

            file.append(binary.segment());
        }
    }
}
