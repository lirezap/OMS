package com.lirezap.nex.http;

import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

import static com.lirezap.nex.http.Error.SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

/**
 * Json body responder handler implementation.
 *
 * @author Alireza Pourtaghi
 */
public final class JsonBodyResponderHandler extends HTTPHandler {

    @Override
    public void handle(final RoutingContext routingContext) {
        try {
            routingContext.response().putHeader(X_FRAME_OPTIONS, "nosniff");

            final var response = Json.encodeToBuffer(routingContext.get(RESPONSE_BODY));
            routingContext.response()
                    .setStatusCode(OK.code())
                    .putHeader(CONTENT_TYPE, "application/json")
                    .end(response);
        } catch (RuntimeException ex) {
            logger.error("{}", ex.getMessage());
            SERVER_ERROR.send(routingContext);
        }
    }
}
