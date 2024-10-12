package com.lirezap.nex.http;

import com.lirezap.nex.binary.http.HTTPRequest;
import io.vertx.ext.web.RoutingContext;

import static com.lirezap.nex.context.AppContext.context;

/**
 * Request logger handler.
 *
 * @author Alireza Pourtaghi
 */
public final class RequestLoggerHandler extends HTTPHandler {

    @Override
    public void handle(final RoutingContext routingContext) {
        final var start = System.nanoTime();
        routingContext.addHeadersEndHandler(_ -> {
            final var duration = System.nanoTime() - start;
            routingContext.response().putHeader(X_RESPONSE_TIME, String.valueOf(duration));
            log(routingContext, duration);
        });

        routingContext.next();
    }

    private void log(final RoutingContext routingContext, final long duration) {
        final var uri = routingContext.request().uri();
        if (uri.equals("/ready") || uri.startsWith("/health"))
            return;

        final var ip = routingContext.request().getHeader("x-real-ip");
        final var method = routingContext.request().method().name();
        final var version = routingContext.request().version().name();
        final var trackId = routingContext.request().getHeader("x-track-id");
        final var responseStatusCode = routingContext.response().getStatusCode();
        final var httpRequest = new HTTPRequest(ip, method, uri, version, trackId, responseStatusCode, duration,
                null, null, System.currentTimeMillis());

        context().httpRequestsLogFile().ifPresentOrElse(file -> file.append(httpRequest), () -> {});
    }
}
