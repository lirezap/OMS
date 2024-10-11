package com.lirezap.nex.http.handlers;

import com.lirezap.nex.binary.http.HTTPRequest;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.LoggerFormatter;

import static com.lirezap.nex.context.AppContext.context;
import static io.vertx.ext.web.impl.Utils.formatRFC1123DateTime;

/**
 * Request logging formatter to be able to log incoming HTTP requests.
 *
 * @author Alireza Pourtaghi
 */
public final class RequestLoggingFormatter implements LoggerFormatter {

    @Override
    public String format(final RoutingContext routingContext, final long responseTime) {
        final var ip = routingContext.request().getHeader("x-real-ip");
        final var method = routingContext.request().method().name();
        final var uri = routingContext.request().uri();
        final var version = routingContext.request().version().name();
        final var referrer = routingContext.request().getHeader("referrer");
        final var userAgent = routingContext.request().getHeader("user-agent");
        final var trackId = routingContext.request().getHeader("x-track-id");
        final var responseStatusCode = routingContext.response().getStatusCode();

        log(new HTTPRequest(ip, method, uri, version, null, responseStatusCode, responseTime, null, null, System.currentTimeMillis()));
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

    private void log(final HTTPRequest httpRequest) {
        if (httpRequest.getUri().equals("/ready") || httpRequest.getUri().startsWith("/health"))
            return;

        context().httpRequestsLogFile().ifPresentOrElse(file -> file.append(httpRequest), () -> {});
    }
}
