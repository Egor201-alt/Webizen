package com.egor201.webizen.util;

import com.google.gson.*;

public class JsonUtil {
    /**
     * Extracts a value from a JSON string using dot-notation path.
     * Supports array indices: abilities.0.ability.name
     * Always returns a plain string — never a Denizen MapTag/ListTag.
     * This avoids crashes from URLs and special characters in Denizen's serializer.
     */
    public static String extractPath(String rawJson, String path) {
        if (rawJson == null || rawJson.isBlank() || path == null || path.isBlank()) return "";
        try {
            JsonElement current = JsonParser.parseString(rawJson);
            for (String part : path.split("\\.")) {
                if (current == null || current.isJsonNull()) return "";
                if (current.isJsonArray()) {
                    try {
                        current = current.getAsJsonArray().get(Integer.parseInt(part));
                    } catch (Exception e) { return ""; }
                } else if (current.isJsonObject()) {
                    current = current.getAsJsonObject().get(part);
                } else {
                    return "";
                }
            }
            if (current == null || current.isJsonNull()) return "null";
            if (current.isJsonPrimitive()) return current.getAsString();
            return current.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Returns a safe preview of JSON for logging — truncated, no special chars.
     */
    public static String preview(String raw, int maxLen) {
        if (raw == null) return "";
        return raw.length() > maxLen ? raw.substring(0, maxLen) + "..." : raw;
    }
}
