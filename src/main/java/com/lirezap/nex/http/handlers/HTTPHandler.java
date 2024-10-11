package com.lirezap.nex.http.handlers;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base HTTP handler abstraction that can be added into the chain of HTTP handlers.
 *
 * @author Alireza Pourtaghi
 */
public abstract class HTTPHandler implements Handler<RoutingContext> {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected static final CharSequence X_RESPONSE_TIME = HttpHeaders.createOptimized("x-response-time");
    protected static final CharSequence X_FRAME_OPTIONS = HttpHeaders.createOptimized("x-frame-options");

    protected static final String RESPONSE_BODY = "RESPONSE_BODY";
}
