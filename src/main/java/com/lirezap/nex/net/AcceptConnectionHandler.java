package com.lirezap.nex.net;

import com.lirezap.nex.context.AppContext;

import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * @author Alireza Pourtaghi
 */
public final class AcceptConnectionHandler implements CompletionHandler<AsynchronousSocketChannel, AppContext> {

    @Override
    public void completed(final AsynchronousSocketChannel result, final AppContext context) {
        // TODO: Complete implementation.
    }

    @Override
    public void failed(final Throwable exc, final AppContext context) {
        // TODO: Complete implementation.
    }
}
