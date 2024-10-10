package com.lirezap.nex.binary.http;

import static com.lirezap.nex.binary.BinaryRepresentable.representationSize;

/**
 * @author Alireza Pourtaghi
 */
public final class HTTPRequest {
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
        return 4 + representationSize(httpMethod) + representationSize(uri) + representationSize(httpVersion) +
                representationSize(callerTrackId) + 4 + 8 + representationSize(callerType) +
                representationSize(callerIdentifier) + 8;
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
