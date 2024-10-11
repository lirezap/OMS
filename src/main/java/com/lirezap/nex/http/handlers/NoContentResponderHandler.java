package com.lirezap.nex.http.handlers;

import io.vertx.ext.web.RoutingContext;

import static com.lirezap.nex.http.Error.SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;

/**
 * No content responder handler implementation.
 *
 * @author Alireza Pourtaghi
 */
public final class NoContentResponderHandler extends HTTPHandler {

    @Override
    public void handle(final RoutingContext routingContext) {
        try {
            routingContext.response().putHeader(X_FRAME_OPTIONS, "nosniff");

            routingContext.response()
                    .setStatusCode(NO_CONTENT.code())
                    .end();
        } catch (RuntimeException ex) {
            logger.error("{}", ex.getMessage());
            SERVER_ERROR.send(routingContext);
        }
    }
}
