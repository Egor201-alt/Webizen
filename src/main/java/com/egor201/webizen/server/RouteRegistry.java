package com.egor201.webizen.server;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouteRegistry {

    private final List<RouteEntry> routes = new ArrayList<>();
    private final List<StaticEntry> statics = new ArrayList<>();

    public void register(String method, String path, String label) {
        routes.add(new RouteEntry(method.toUpperCase(), path, label));
    }

    public void registerStatic(String urlPath, String folderPath) {
        statics.add(new StaticEntry(urlPath, folderPath));
    }

    public RouteMatch match(String method, String path) {
        for (RouteEntry entry : routes) {
            if (!entry.method.equals(method.toUpperCase()) && !entry.method.equals("ANY")) continue;
            Map<String, String> params = entry.matchPath(path);
            if (params != null) return new RouteMatch(entry.label, params);
        }
        return null;
    }

    public StaticEntry matchStatic(String path) {
        for (StaticEntry e : statics) {
            if (path.startsWith(e.urlPath)) return e;
        }
        return null;
    }

    private static class RouteEntry {
        final String method;
        final String rawPath;
        final String label;
        final Pattern pattern;
        final List<String> paramNames = new ArrayList<>();

        RouteEntry(String method, String path, String label) {
            this.method  = method;
            this.rawPath = path;
            this.label   = label;

            StringBuilder regex = new StringBuilder("^");
            String[] segments = path.split("/");
            for (String seg : segments) {
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
            for (int i = 0; i < paramNames.size(); i++) {
                params.put(paramNames.get(i), m.group(i + 1));
            }
            return params;
        }
    }

    public record RouteMatch(String label, Map<String, String> params) {}
    public record StaticEntry(String urlPath, String folderPath) {}
}