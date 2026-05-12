package com.egor201.webizen.tags;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.tags.TagManager;
import com.egor201.webizen.Webizen;
import com.egor201.webizen.client.ClientManager;
import com.egor201.webizen.util.JsonUtil;

public class WebizenTags {

    public static void register() {

        // <--[tag]
        // @Attribute <http_get[<url>].json>
        // @Returns MapTag or ListTag
        // @Group Webizen
        // @Description Performs a synchronous GET request and returns the body parsed as JSON.
        // Use only for simple cases — prefer http_request + on http response for complex logic.
        //
        // @Usage
        // - define data <http_get[https://api.example.com/players].json>
        // - define name <http_get[https://api.example.com/user/1].json.get[name]>
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
                return JsonUtil.toObjectTag(result.body());
            }

            return new ElementTag(result.body());
        });

        // <--[tag]
        // @Attribute <http_servers>
        // @Returns ListTag
        // @Group Webizen
        // @Description Returns a ListTag of all currently running HTTP server IDs.
        //
        // @Usage
        // - narrate "Running servers: <http_servers>"
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
        //
        // @Usage
        // - narrate "Named clients: <http_clients>"
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
        //
        // @Usage
        // - if !<http_ws_connected[ws1]>:
        //   - http_ws id:ws1 url:wss://example.com/events
        // -->
        TagManager.registerTagHandler(ElementTag.class, "http_ws_connected", attribute -> {
            if (!attribute.hasParam()) return new ElementTag(false);
            String id = attribute.getParam();
            attribute.fulfill(1);
            return new ElementTag(Webizen.getInstance().getWebSocketManager().isConnected(id));
        });
    }
}