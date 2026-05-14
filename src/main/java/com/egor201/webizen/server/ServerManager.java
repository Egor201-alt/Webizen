package com.egor201.webizen.server;

import com.egor201.webizen.Webizen;
import com.egor201.webizen.events.HttpRequestEvent;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.RequestLimit;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.bukkit.Bukkit;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerManager {

    private static final long MAX_BODY_SIZE     = 10 * 1024 * 1024; // 10 MB
    private static final long DEFAULT_TIMEOUT_TICKS = 200L;          // 10 seconds

    private final Map<String, Undertow>           servers         = new ConcurrentHashMap<>();
    private final Map<String, RouteRegistry>      routes          = new ConcurrentHashMap<>();
    private final Map<String, MiddlewareRegistry> middlewares     = new ConcurrentHashMap<>();
    private final Map<String, RequestContext>     pendingRequests = new ConcurrentHashMap<>();

    public boolean start(String id, int port, String bindHost) {
        if (servers.containsKey(id)) return false;

        String host = (bindHost != null && !bindHost.isBlank()) ? bindHost : "0.0.0.0";

        RouteRegistry registry = new RouteRegistry();
        routes.put(id, registry);
        middlewares.put(id, new MiddlewareRegistry());

        HttpHandler handler = exchange -> {
            if (exchange.getRequestContentLength() > MAX_BODY_SIZE) {
                exchange.setStatusCode(413);
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                exchange.getResponseSender().send("{\"error\":\"Request body too large\"}");
                return;
            }

            String method = exchange.getRequestMethod().toString();
            String path   = exchange.getRequestPath();

            RouteRegistry.StaticEntry staticEntry = registry.matchStatic(path);
            if (staticEntry != null) {
                serveStatic(exchange, staticEntry, path);
                return;
            }

            exchange.getRequestReceiver().setMaxBufferSize((int) MAX_BODY_SIZE);
            exchange.getRequestReceiver().receiveFullBytes((ex, body) -> {
                String rawBody = new String(body, StandardCharsets.UTF_8);

                RouteRegistry.RouteMatch match = registry.match(method, path);
                if (match == null) {
                    ex.setStatusCode(404);
                    ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                    ex.getResponseSender().send("{\"error\":\"Not Found\"}");
                    return;
                }

                String ip = ex.getSourceAddress() != null
                    ? ex.getSourceAddress().getAddress().getHostAddress() : "unknown";

                if (match.rateLimiter() != null && !match.rateLimiter().allow(ip)) {
                    ex.setStatusCode(429);
                    ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                    ex.getResponseSender().send("{\"error\":\"Too Many Requests\"}");
                    return;
                }

                Map<String, String> headers = new LinkedHashMap<>();
                ex.getRequestHeaders().forEach(h -> h.forEach(v -> headers.put(h.getHeaderName().toString(), v)));

                Map<String, String> query = new LinkedHashMap<>();
                ex.getQueryParameters().forEach((k, v) -> query.put(k, v.peek()));

                String reqId = UUID.randomUUID().toString();
                String middlewareLabel = middlewares.get(id) != null ? middlewares.get(id).getLabel() : null;

                RequestContext ctx = new RequestContext(reqId, method, path,
                    match.params(), query, headers, rawBody, ip, ex, id);
                ctx.setPendingRouteLabel(match.label());
                pendingRequests.put(reqId, ctx);

                ex.dispatch();

                String fireLabel = middlewareLabel != null ? middlewareLabel : match.label();
                boolean isMW     = middlewareLabel != null;

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
                }, DEFAULT_TIMEOUT_TICKS);
            });
        };

        try {
            Undertow server = Undertow.builder()
                .addHttpListener(port, host, handler)
                .build();
            server.start();
            servers.put(id, server);
            Webizen.getInstance().getLogger().info("[Webizen] Server '" + id + "' started on " + host + ":" + port);
            return true;
        } catch (Exception e) {
            Webizen.getInstance().getLogger().severe("[Webizen] Failed to start server '" + id + "': " + e.getMessage());
            routes.remove(id);
            middlewares.remove(id);
            return false;
        }
    }

    private void serveStatic(HttpServerExchange exchange, RouteRegistry.StaticEntry entry, String requestPath) {
        try {
            String relativePath = requestPath.substring(entry.urlPath().length());
            if (relativePath.isBlank()) relativePath = "index.html";

            File base   = new File(entry.folderPath()).getCanonicalFile();
            File target = new File(base, relativePath).getCanonicalFile();

            if (!target.toPath().startsWith(base.toPath())) {
                exchange.setStatusCode(403);
                exchange.getResponseSender().send("{\"error\":\"Forbidden\"}");
                return;
            }

            if (!target.exists() || !target.isFile()) {
                exchange.setStatusCode(404);
                exchange.getResponseSender().send("{\"error\":\"Not Found\"}");
                return;
            }

            String mime = Files.probeContentType(target.toPath());
            if (mime == null) mime = "application/octet-stream";

            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, mime);
            exchange.setStatusCode(200);
            exchange.getResponseSender().send(java.nio.ByteBuffer.wrap(Files.readAllBytes(target.toPath())));
        } catch (Exception e) {
            exchange.setStatusCode(500);
            exchange.getResponseSender().send("{\"error\":\"Internal Server Error\"}");
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
        servers.clear(); routes.clear(); middlewares.clear();
    }

    public boolean addRoute(String serverId, String method, String path, String label, int rateLimit) {
        RouteRegistry r = routes.get(serverId);
        if (r == null) return false;
        r.register(method, path, label, rateLimit);
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

    public RequestContext getRequest(String reqId)  { return pendingRequests.get(reqId); }
    public boolean isRunning(String id)             { return servers.containsKey(id); }
    public Set<String> getRunningIds()              { return Collections.unmodifiableSet(servers.keySet()); }

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
}