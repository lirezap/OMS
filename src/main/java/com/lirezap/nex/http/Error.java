package com.lirezap.nex.http;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Different HTTP level error types list.
 *
 * @author Alireza Pourtaghi
 */
public enum Error {
    // General
    HANDLER_NOT_FOUND("handler.not_found", "", HttpResponseStatus.NOT_FOUND.code()),
    REQUEST_BODY_NOT_VALID("request_body.not_valid", "", HttpResponseStatus.BAD_REQUEST.code()),
    PARAMETER_NOT_VALID("parameter.not_valid", "", HttpResponseStatus.BAD_REQUEST.code()),
    RESOURCE_NOT_FOUND("resource.not_found", "", HttpResponseStatus.NOT_FOUND.code()),
    FORBIDDEN("forbidden", "", HttpResponseStatus.FORBIDDEN.code()),
    SERVER_ERROR("server.error", "", HttpResponseStatus.INTERNAL_SERVER_ERROR.code());

    private final String code;
    private final String message;
    private final int httpStatusCode;

    Error(final String code, final String message, final int httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    /**
     * Sends current instance of error as an HTTP response.
     *
     * @param routingContext vertx context instance
     */
    public void send(final RoutingContext routingContext) {
        routingContext.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .setStatusCode(getHttpStatusCode())
                .end(toJson().toBuffer());
    }

    /**
     * Serializes current instance of error as JSON.
     *
     * @return json representation of current instance
     */
    private JsonObject toJson() {
        final var json = new JsonObject();
        json.put("code", code);
        json.put("message", message);

        return json;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
