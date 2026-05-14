package com.egor201.webizen.server;

import java.util.*;
import java.util.regex.*;

public class RouteRegistry {

    private final List<RouteEntry> routes   = new ArrayList<>();
    private final List<StaticEntry> statics = new ArrayList<>();

    public void register(String method, String path, String label, int rateLimitPerMin) {
        routes.add(new RouteEntry(method.toUpperCase(), path, label, rateLimitPerMin));
    }

    public void registerStatic(String urlPath, String folderPath) {
        statics.add(new StaticEntry(urlPath, folderPath));
    }

    public RouteMatch match(String method, String path) {
        for (RouteEntry e : routes) {
            if (!e.method.equals(method.toUpperCase()) && !e.method.equals("ANY")) continue;
            Map<String, String> params = e.matchPath(path);
            if (params != null) return new RouteMatch(e.label, params, e.rateLimiter);
        }
        return null;
    }

    public StaticEntry matchStatic(String path) {
        for (StaticEntry e : statics) { if (path.startsWith(e.urlPath)) return e; }
        return null;
    }

    private static class RouteEntry {
        final String method, rawPath, label;
        final Pattern pattern;
        final List<String> paramNames = new ArrayList<>();
        final RateLimiter rateLimiter;

        RouteEntry(String method, String path, String label, int rateLimitPerMin) {
            this.method  = method;
            this.rawPath = path;
            this.label   = label;
            this.rateLimiter = rateLimitPerMin > 0
                ? new RateLimiter(rateLimitPerMin, 60_000) : null;

            StringBuilder regex = new StringBuilder("^");
            for (String seg : path.split("/")) {
                if (seg.isEmpty()) continue;
                regex.append("/");
                if (seg.startsWith("{") && seg.endsWith("}")) {
                    paramNames.add(seg.substring(1, seg.length() - 1));
                    regex.append("([^/]+)");
                } else {
                    regex.append(Pattern.quote(seg));
                }
            }
            regex.append("/?$");
            this.pattern = Pattern.compile(regex.toString());
        }

        Map<String, String> matchPath(String path) {
            Matcher m = pattern.matcher(path);
            if (!m.matches()) return null;
            Map<String, String> params = new LinkedHashMap<>();
            for (int i = 0; i < paramNames.size(); i++) params.put(paramNames.get(i), m.group(i + 1));
            return params;
        }
    }

    public record RouteMatch(String label, Map<String, String> params, RateLimiter rateLimiter) {}
    public record StaticEntry(String urlPath, String folderPath) {}
}