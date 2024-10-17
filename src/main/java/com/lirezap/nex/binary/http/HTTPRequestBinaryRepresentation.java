package com.lirezap.nex.binary.http;

import com.lirezap.nex.binary.BinaryRepresentation;

import java.lang.foreign.Arena;
import java.net.Inet4Address;
import java.net.UnknownHostException;

/**
 * @author Alireza Pourtaghi
 */
public final class HTTPRequestBinaryRepresentation extends BinaryRepresentation<HTTPRequest> {
    private final HTTPRequest httpRequest;

    public HTTPRequestBinaryRepresentation(final HTTPRequest httpRequest) {
        super(httpRequest.size());
        this.httpRequest = httpRequest;
    }

    public HTTPRequestBinaryRepresentation(final Arena arena, final HTTPRequest httpRequest) {
        super(arena, httpRequest.size());
        this.httpRequest = httpRequest;
    }

    @Override
    protected int id() {
        return 100;
    }

    @Override
    protected void encodeRecord() {
        try {
            encodeSourceIp();
            putString(httpRequest.getHttpMethod());
            putString(httpRequest.getUri());
            putString(httpRequest.getHttpVersion());
            putString(httpRequest.getCallerTrackId());
            putInt(httpRequest.getResponseStatusCode());
            putLong(httpRequest.getResponseTime());
            putString(httpRequest.getCallerType());
            putString(httpRequest.getCallerIdentifier());
            putLong(httpRequest.getTs());
            putByte(LFD);
            putByte(CRT);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void encodeSourceIp() throws UnknownHostException {
        final var ip = Inet4Address.getByName(httpRequest.getSourceIp());
        putByte(ip.getAddress()[0]);
        putByte(ip.getAddress()[1]);
        putByte(ip.getAddress()[2]);
        putByte(ip.getAddress()[3]);
    }
}
