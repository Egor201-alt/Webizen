package com.egor201.webizen.tags;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.tags.TagManager;
import com.egor201.webizen.Webizen;
import com.egor201.webizen.client.ClientManager;
import com.egor201.webizen.util.JsonUtil;
import com.google.gson.*;

public class WebizenTags {

    public static void register() {

        // <--[tag]
        // @Attribute <http_get[<url>]>
        // @Returns ElementTag
        // @Group Webizen
        // @Description Performs a synchronous GET request.
        // Chain with sub-tags to get what you need:
        //   .body              - raw response body as text
        //   .status            - HTTP status code
        //   .header[<name>]    - specific response header
        //   .json              - full body as raw JSON ElementTag (safe to store and pass around)
        //   .json_value[<path>] - extract a specific field from JSON using dot-notation path
        //                        e.g. .json_value[abilities.0.ability.name] -> "limber"
        //
        // NOTE: Use .json_value[path] instead of .json when working with complex APIs.
        // Converting large JSON to Denizen MapTag can break if values contain special chars (; | @).
        //
        // @Usage
        // - define name <http_get[https://pokeapi.co/api/v2/pokemon/ditto].json_value[name]>
        // - define raw <http_get[https://api.example.com/data].json>
        // - define code <http_get[https://api.example.com/ping].status>
        // -->
        TagManager.registerTagHandler(ObjectTag.class, "http_get", attribute -> {
            if (!attribute.hasParam()) return null;
            String url = attribute.getParam();
            attribute.fulfill(1);

            ClientManager.SyncResult result;
            try {
                result = Webizen.getInstance().getClientManager().sendSync("GET", url, null, null);
            } catch (Exception e) {
                return new ElementTag("");
            }

            if (attribute.startsWith("status")) {
                attribute.fulfill(1);
                return new ElementTag(result.status());
            }

            if (attribute.startsWith("body")) {
                attribute.fulfill(1);
                return new ElementTag(result.body());
            }

            if (attribute.startsWith("header") && attribute.hasParam()) {
                String headerName = attribute.getParam();
                attribute.fulfill(1);
                String val = result.headers().get(headerName);
                return new ElementTag(val != null ? val : "");
            }

            if (attribute.startsWith("json")) {
                attribute.fulfill(1);
                if (attribute.startsWith("json_value") && attribute.hasParam()) {
                    String path = attribute.getParam();
                    attribute.fulfill(1);
                    return extractJsonPath(result.body(), path);
                }
                return new ElementTag(result.body());
            }

            return new ElementTag(result.body());
        });

        // <--[tag]
        // @Attribute <http_servers>
        // @Returns ListTag
        // @Group Webizen
        // @Description Returns a ListTag of all currently running HTTP server IDs.
        // -->
        TagManager.registerTagHandler(ListTag.class, "http_servers", attribute -> {
            ListTag list = new ListTag();
            Webizen.getInstance().getServerManager().getRunningIds()
                .forEach(id -> list.addObject(new ElementTag(id)));
            return list;
        });

        // <--[tag]
        // @Attribute <http_clients>
        // @Returns ListTag
        // @Group Webizen
        // @Description Returns a ListTag of all registered named HTTP client IDs.
        // -->
        TagManager.registerTagHandler(ListTag.class, "http_clients", attribute -> {
            ListTag list = new ListTag();
            Webizen.getInstance().getClientManager().getClientIds()
                .forEach(id -> list.addObject(new ElementTag(id)));
            return list;
        });

        // <--[tag]
        // @Attribute <http_ws_connected[<id>]>
        // @Returns ElementTag(Boolean)
        // @Group Webizen
        // @Description Returns true if the WebSocket with the given ID is currently connected.
        // -->
        TagManager.registerTagHandler(ElementTag.class, "http_ws_connected", attribute -> {
            if (!attribute.hasParam()) return new ElementTag(false);
            String id = attribute.getParam();
            attribute.fulfill(1);
            return new ElementTag(Webizen.getInstance().getWebSocketManager().isConnected(id));
        });
    }

    /**
     * Extracts a value from JSON using dot-notation path.
     * Supports array indices: abilities.0.ability.name
     * Returns ElementTag with the string value, or empty if not found.
     */
    private static ElementTag extractJsonPath(String rawJson, String path) {
        if (rawJson == null || rawJson.isBlank() || path == null || path.isBlank()) {
            return new ElementTag("");
        }
        try {
            JsonElement current = JsonParser.parseString(rawJson);
            String[] parts = path.split("\\.");
            for (String part : parts) {
                if (current == null || current.isJsonNull()) return new ElementTag("");
                if (current.isJsonArray()) {
                    try {
                        int index = Integer.parseInt(part);
                        current = current.getAsJsonArray().get(index);
                    } catch (NumberFormatException | IndexOutOfBoundsException e) {
                        return new ElementTag("");
                    }
                } else if (current.isJsonObject()) {
                    current = current.getAsJsonObject().get(part);
                } else {
                    return new ElementTag("");
                }
            }
            if (current == null || current.isJsonNull()) return new ElementTag("null");
            if (current.isJsonPrimitive()) return new ElementTag(current.getAsString());
            return new ElementTag(current.toString());
        } catch (Exception e) {
            return new ElementTag("");
        }
    }
}