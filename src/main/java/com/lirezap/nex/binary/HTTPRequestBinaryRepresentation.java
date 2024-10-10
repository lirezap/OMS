package com.lirezap.nex.binary;

import java.lang.foreign.Arena;
import java.net.Inet4Address;
import java.net.UnknownHostException;

import static com.lirezap.nex.binary.BinaryRepresentations.representationSize;

/**
 * @author Alireza Pourtaghi
 */
public final class HTTPRequestBinaryRepresentation
        extends BinaryRepresentation<HTTPRequestBinaryRepresentation.HTTPRequest> {

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
        return 1;
    }

    @Override
    protected void encodeRecord() {
        try {
            encodeSourceIp();
            encodeHttpMethod();
            encodeUri();
            encodeHttpVersion();
            encodeCallerTrackId();
            encodeResponseStatusCode();
            encodeResponseTime();
            encodeCallerType();
            encodeCallerIdentifier();
            encodeTimestamp();
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

    private void encodeHttpMethod() {
        putString(httpRequest.getHttpMethod());
    }

    private void encodeUri() {
        putString(httpRequest.getUri());
    }

    private void encodeHttpVersion() {
        putString(httpRequest.getHttpVersion());
    }

    private void encodeCallerTrackId() {
        putString(httpRequest.getCallerTrackId());
    }

    private void encodeResponseStatusCode() {
        putInt(httpRequest.getResponseStatusCode());
    }

    private void encodeResponseTime() {
        putLong(httpRequest.getResponseTime());
    }

    private void encodeCallerType() {
        putString(httpRequest.getCallerType());
    }

    private void encodeCallerIdentifier() {
        putString(httpRequest.getCallerIdentifier());
    }

    private void encodeTimestamp() {
        putLong(httpRequest.getTs());
    }

    /**
     * @author Alireza Pourtaghi
     */
    public static final class HTTPRequest {
        private final String sourceIp;
        private final String httpMethod;
        private final String uri;
        private final String httpVersion;
        private final String callerTrackId;
        private final int responseStatusCode;
        private final long responseTime;
        private final String callerType;
        private final String callerIdentifier;
        private final long ts;

        public HTTPRequest(final String sourceIp, final String httpMethod, final String uri, final String httpVersion,
                           final String callerTrackId, final int responseStatusCode, final long responseTime,
                           final String callerType, final String callerIdentifier, final long ts) {

            this.sourceIp = sourceIp == null ? "0.0.0.0" : sourceIp;
            this.httpMethod = httpMethod == null ? "" : httpMethod;
            this.uri = uri == null ? "" : uri;
            this.httpVersion = httpVersion == null ? "" : httpVersion;
            this.callerTrackId = callerTrackId == null ? "" : callerTrackId;
            this.responseStatusCode = responseStatusCode;
            this.responseTime = responseTime;
            this.callerType = callerType == null ? "" : callerType;
            this.callerIdentifier = callerIdentifier == null ? "" : callerIdentifier;
            this.ts = ts;
        }

        public int size() {
            final var httpMethodSize = representationSize(httpMethod);
            final var uriSize = representationSize(uri);
            final var httpVersionSize = representationSize(httpVersion);
            final var callerTrackIdSize = representationSize(callerTrackId);
            final var callerTypeSize = representationSize(callerType);
            final var callerIdentifierSize = representationSize(callerIdentifier);

            return 4 + httpMethodSize + uriSize + httpVersionSize + callerTrackIdSize + 4 + 8 + callerTypeSize +
                    callerIdentifierSize + 8;
        }

        public String getSourceIp() {
            return sourceIp;
        }

        public String getHttpMethod() {
            return httpMethod;
        }

        public String getUri() {
            return uri;
        }

        public String getHttpVersion() {
            return httpVersion;
        }

        public String getCallerTrackId() {
            return callerTrackId;
        }

        public int getResponseStatusCode() {
            return responseStatusCode;
        }

        public long getResponseTime() {
            return responseTime;
        }

        public String getCallerType() {
            return callerType;
        }

        public String getCallerIdentifier() {
            return callerIdentifier;
        }

        public long getTs() {
            return ts;
        }
    }
}
