package com.egor201.webizen.client;

import java.util.Map;

public class HttpClientConfig {
    public final String baseUrl;
    public final Map<String, String> headers;
    public final long timeoutMs;

    public HttpClientConfig(String baseUrl, Map<String, String> headers, long timeoutMs) {
        this.baseUrl   = baseUrl != null ? baseUrl : "";
        this.headers   = headers;
        this.timeoutMs = timeoutMs;
    }

    public String resolveUrl(String path) {
        if (path == null || path.isEmpty()) return baseUrl;
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        return baseUrl + (path.startsWith("/") ? path : "/" + path);
    }
}