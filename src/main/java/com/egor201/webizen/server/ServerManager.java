package com.egor201.webizen.server;

import com.egor201.webizen.Webizen;
import com.egor201.webizen.events.HttpRequestEvent;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.bukkit.Bukkit;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerManager {

    private final Map<String, Undertow>           servers         = new ConcurrentHashMap<>();
    private final Map<String, RouteRegistry>      routes          = new ConcurrentHashMap<>();
    private final Map<String, MiddlewareRegistry> middlewares     = new ConcurrentHashMap<>();
    private final Map<String, RequestContext>     pendingRequests = new ConcurrentHashMap<>();

    public boolean start(String id, int port) {
        if (servers.containsKey(id)) return false;

        RouteRegistry registry = new RouteRegistry();
        routes.put(id, registry);
        middlewares.put(id, new MiddlewareRegistry());

        HttpHandler handler = exchange -> {
            String method = exchange.getRequestMethod().toString();
            String path   = exchange.getRequestPath();

            exchange.getRequestReceiver().receiveFullBytes((ex, body) -> {
                String rawBody = new String(body, StandardCharsets.UTF_8);

                RouteRegistry.RouteMatch match = registry.match(method, path);
                if (match == null) {
                    ex.setStatusCode(404);
                    ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                    ex.getResponseSender().send("{\"error\":\"Not Found\"}");
                    return;
                }

                Map<String, String> headers = new LinkedHashMap<>();
                ex.getRequestHeaders().forEach(h -> h.forEach(v -> headers.put(h.getHeaderName().toString(), v)));

                Map<String, String> query = new LinkedHashMap<>();
                ex.getQueryParameters().forEach((k, v) -> query.put(k, v.peek()));

                String reqId = UUID.randomUUID().toString();
                String ip    = ex.getSourceAddress() != null
                    ? ex.getSourceAddress().getAddress().getHostAddress() : "unknown";

                String middlewareLabel = middlewares.get(id) != null ? middlewares.get(id).getLabel() : null;

                RequestContext ctx = new RequestContext(reqId, method, path,
                    match.params(), query, headers, rawBody, ip, ex, id);
                ctx.setPendingRouteLabel(match.label());
                pendingRequests.put(reqId, ctx);

                ex.dispatch();

                String fireLabel  = middlewareLabel != null ? middlewareLabel : match.label();
                boolean isMW      = middlewareLabel != null;

                Bukkit.getScheduler().runTask(Webizen.getInstance(), () ->
                    HttpRequestEvent.instance.fireFor(reqId, method, path, match.params(),
                        query, headers, rawBody, ip, id, fireLabel, isMW)
                );

                Bukkit.getScheduler().runTaskLater(Webizen.getInstance(), () -> {
                    RequestContext pending = pendingRequests.remove(reqId);
                    if (pending != null && !pending.isCompleted()) {
                        ex.setStatusCode(503);
                        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                        ex.getResponseSender().send("{\"error\":\"Script timeout\"}");
                    }
                }, 200L);
            });
        };

        try {
            Undertow server = Undertow.builder().addHttpListener(port, "0.0.0.0", handler).build();
            server.start();
            servers.put(id, server);
            return true;
        } catch (Exception e) {
            Webizen.getInstance().getLogger().severe("[Webizen] Failed to start server '" + id + "': " + e.getMessage());
            routes.remove(id);
            middlewares.remove(id);
            return false;
        }
    }

    public boolean stop(String id) {
        Undertow server = servers.remove(id);
        routes.remove(id);
        middlewares.remove(id);
        if (server != null) { server.stop(); return true; }
        return false;
    }

    public void stopAll() {
        servers.forEach((id, s) -> s.stop());
        servers.clear();
        routes.clear();
        middlewares.clear();
    }

    public boolean addRoute(String serverId, String method, String path, String label) {
        RouteRegistry r = routes.get(serverId);
        if (r == null) return false;
        r.register(method, path, label);
        return true;
    }

    public boolean setMiddleware(String serverId, String label) {
        MiddlewareRegistry m = middlewares.get(serverId);
        if (m == null) return false;
        m.setLabel(label);
        return true;
    }

    public boolean addStatic(String serverId, String urlPath, String folderPath) {
        RouteRegistry r = routes.get(serverId);
        if (r == null) return false;
        r.registerStatic(urlPath, folderPath);
        return true;
    }

    public RequestContext getRequest(String reqId) {
        return pendingRequests.get(reqId);
    }

    public void completeRequest(String reqId, int status, String body, Map<String, String> headers) {
        RequestContext ctx = pendingRequests.remove(reqId);
        if (ctx == null || ctx.isCompleted()) return;
        ctx.markCompleted();
        HttpServerExchange ex = ctx.exchange();
        ex.setStatusCode(status);
        if (headers != null) headers.forEach((k, v) -> ex.getResponseHeaders().put(new HttpString(k), v));
        if (!ex.getResponseHeaders().contains(Headers.CONTENT_TYPE))
            ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        ex.getResponseSender().send(body != null ? body : "");
    }

    public boolean isRunning(String id)         { return servers.containsKey(id); }
    public Set<String> getRunningIds()          { return Collections.unmodifiableSet(servers.keySet()); }
    public RouteRegistry getRoutes(String id)   { return routes.get(id); }
}