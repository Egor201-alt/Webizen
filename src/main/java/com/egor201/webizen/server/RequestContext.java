package com.egor201.webizen.server;

import io.undertow.server.HttpServerExchange;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class RequestContext {
    private final String requestId;
    private final String method;
    private final String path;
    private final Map<String, String> params;
    private final Map<String, String> query;
    private final Map<String, String> headers;
    private final String body;
    private final String ip;
    private final HttpServerExchange exchange;
    private final String serverId;
    private final AtomicBoolean completed = new AtomicBoolean(false);

    private String pendingRouteLabel;

    public RequestContext(String requestId, String method, String path,
                          Map<String, String> params, Map<String, String> query,
                          Map<String, String> headers, String body, String ip,
                          HttpServerExchange exchange, String serverId) {
        this.requestId = requestId;
        this.method    = method;
        this.path      = path;
        this.params    = params;
        this.query     = query;
        this.headers   = headers;
        this.body      = body;
        this.ip        = ip;
        this.exchange  = exchange;
        this.serverId  = serverId;
    }

    public String getRequestId()   { return requestId; }
    public String getMethod()      { return method; }
    public String getPath()        { return path; }
    public Map<String, String> getParams()   { return params; }
    public Map<String, String> getQuery()    { return query; }
    public Map<String, String> getHeaders()  { return headers; }
    public String getBody()        { return body; }
    public String getIp()          { return ip; }
    public HttpServerExchange exchange() { return exchange; }
    public String getServerId()    { return serverId; }
    public String getPendingRouteLabel() { return pendingRouteLabel; }
    public void setPendingRouteLabel(String label) { this.pendingRouteLabel = label; }

    public void markCompleted() { completed.set(true); }
    public boolean isCompleted() { return completed.get(); }
}