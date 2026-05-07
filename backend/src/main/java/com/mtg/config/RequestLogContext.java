package com.mtg.config;

final class RequestLogContext {
    private static final ThreadLocal<RequestInfo> CURRENT = new ThreadLocal<>();

    private RequestLogContext() {}

    static void set(RequestInfo info) {
        CURRENT.set(info);
    }

    static RequestInfo get() {
        return CURRENT.get();
    }

    static void clear() {
        CURRENT.remove();
    }

    record RequestInfo(String requestId, String method, String endpoint, long startedAtMillis) {}
}
