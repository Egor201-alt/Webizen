package com.egor201.webizen.events;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;

public class HttpWsMessageEvent extends ScriptEvent {
    // <--[event]
    // @Events
    // http ws message
    // @Group Webizen
    // @Switch id:<id>
    // @Context
    // <context.id>      - WebSocket connection ID
    // <context.message> - raw message text
    // <context.json>    - same as message — raw string, use http_json_value to extract fields
    // -->
    public static HttpWsMessageEvent instance;
    private ElementTag id, message;

    public HttpWsMessageEvent() { instance = this; registerCouldMatcher("http ws message"); registerSwitches("id"); }

    @Override public boolean matches(ScriptPath path) {
        if (!runGenericSwitchCheck(path, "id", id.asString())) return false;
        return super.matches(path);
    }

    @Override public ObjectTag getContext(String name) {
        return switch (name) {
            case "id" -> id; case "message" -> message; case "json" -> message;
            default -> super.getContext(name);
        };
    }

    public void fireFor(String wsId, String msg) {
        this.id      = new ElementTag(wsId);
        this.message = new ElementTag(msg != null ? msg : "");
        fire();
    }
}