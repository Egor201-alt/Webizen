package com.egor201.webizen.tags;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.tags.TagManager;
import com.egor201.webizen.Webizen;
import com.egor201.webizen.client.ClientManager;
import com.egor201.webizen.util.JsonUtil;
import com.egor201.webizen.util.UrlValidator;

public class WebizenTags {

    public static void register() {

        // <--[tag]
        // @Attribute <http_get[<url>].body>
        // @Attribute <http_get[<url>].status>
        // @Attribute <http_get[<url>].header[<name>]>
        // @Attribute <http_get[<url>].json>
        // @Attribute <http_get[<url>].json_value[<dot.path>]>
        // @Returns ElementTag
        // @Group Webizen
        //
        // @Description
        // Synchronous GET request. Use only for simple cases — prefer http_request + on http response for production scripts.
        // WARNING: Blocks the server thread while waiting for response.
        //
        // .json          — raw JSON body string (safe to store)
        // .json_value[x] — extract a field from JSON using dot-notation. Supports array indices.
        //   Example: .json_value[abilities.0.ability.name]
        //
        // @Usage
        // - define name <http_get[https://pokeapi.co/api/v2/pokemon/ditto].json_value[name]>
        // - define weight <http_get[https://pokeapi.co/api/v2/pokemon/ditto].json_value[weight]>
        // - define status <http_get[https://api.example.com/ping].status>
        // -->
        TagManager.registerTagHandler(ObjectTag.class, "http_get", attribute -> {
            if (!attribute.hasParam()) return null;
            String url = attribute.getParam();
            attribute.fulfill(1);

            // SSRF protection
            UrlValidator.ValidationResult v = UrlValidator.validate(url);
            if (!v.valid()) {
                attribute.echoError("Blocked URL in http_get: " + v.reason());
                return new ElementTag("");
            }

            ClientManager.SyncResult result;
            try {
                result = Webizen.getInstance().getClientManager().sendSync("GET", url, null, null);
            } catch (Exception e) {
                attribute.echoError("http_get failed: " + e.getMessage());
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
                // .json_value[path] — safe field extraction
                if (attribute.startsWith("json_value") && attribute.hasParam()) {
                    String path = attribute.getParam();
                    attribute.fulfill(1);
                    return new ElementTag(JsonUtil.extractPath(result.body(), path));
                }
                // .json alone — raw string, safe
                return new ElementTag(result.body());
            }

            return new ElementTag(result.body());
        });

        // <--[tag]
        // @Attribute <http_json_value[<json>].path[<dot.path>]>
        // @Returns ElementTag
        // @Group Webizen
        // @Description Extracts a value from a raw JSON string using dot-notation path.
        // Supports array indices: abilities.0.ability.name
        // Use this with context.body or context.json from http response/request events.
        //
        // @Usage
        // on http response label:get_ditto:
        //   - define name <http_json_value[<context.body>].path[name]>
        //   - define ability <http_json_value[<context.body>].path[abilities.0.ability.name]>
        // -->
        TagManager.registerTagHandler(ElementTag.class, "http_json_value", attribute -> {
            if (!attribute.hasParam()) return new ElementTag("");
            String json = attribute.getParam();
            attribute.fulfill(1);
            if (attribute.startsWith("path") && attribute.hasParam()) {
                String path = attribute.getParam();
                attribute.fulfill(1);
                return new ElementTag(JsonUtil.extractPath(json, path));
            }
            return new ElementTag(json);
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
}