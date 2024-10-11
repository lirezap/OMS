package com.lirezap.nex.http.handlers;

import io.vertx.ext.web.RoutingContext;

import static java.net.HttpURLConnection.HTTP_OK;

/**
 * Server live-ness checker handler.
 *
 * @author Alireza Pourtaghi
 */
public final class LiveNessHandler extends HTTPHandler {

    @Override
    public void handle(final RoutingContext routingContext) {
        routingContext.request().response()
                .setStatusCode(HTTP_OK)
                .end();
    }
}
