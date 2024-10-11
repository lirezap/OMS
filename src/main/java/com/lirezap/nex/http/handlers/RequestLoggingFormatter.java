package com.lirezap.nex.http.handlers;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.LoggerFormatter;

import static io.vertx.ext.web.impl.Utils.formatRFC1123DateTime;

/**
 * Request logging formatter to be able to log incoming HTTP requests.
 *
 * @author Alireza Pourtaghi
 */
public final class RequestLoggingFormatter implements LoggerFormatter {

    @Override
    public String format(final RoutingContext routingContext, final long responseTime) {
        var ip = routingContext.request().getHeader("x-real-ip");
        var method = routingContext.request().method().name();
        var uri = routingContext.request().uri();
        var version = routingContext.request().version().name();
        var referrer = routingContext.request().getHeader("referrer");
        var userAgent = routingContext.request().getHeader("user-agent");
        var trackId = routingContext.request().getHeader("x-track-id");
        var responseStatusCode = routingContext.response().getStatusCode();

        return String.format("%s | %s | %s %s %s | referrer: %s | user-agent: %s | x-track-id: %s | response-status-code: %s | response-time: %s",
                ip,
                formatRFC1123DateTime(System.currentTimeMillis()),
                method,
                uri,
                version,
                referrer,
                userAgent,
                trackId,
                responseStatusCode,
                responseTime);
    }
}
