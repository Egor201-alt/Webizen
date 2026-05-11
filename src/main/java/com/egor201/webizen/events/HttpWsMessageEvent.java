package com.egor201.webizen.events;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.egor201.webizen.util.JsonUtil;

public class HttpWsMessageEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // http ws message
    // @Group Webizen
    // @Switch id:<id> to only fire for a specific WebSocket connection id.
    // @Context
    // <context.id>      - the WebSocket connection ID
    // <context.message> - raw message text
    // <context.json>    - message parsed as MapTag/ListTag if valid JSON
    // -->

    public static HttpWsMessageEvent instance;
    private ElementTag id, message;
    private ObjectTag json;

    public HttpWsMessageEvent() {
        instance = this;
        registerCouldMatcher("http ws message");
        registerSwitches("id");
    }

    @Override public boolean matches(ScriptPath path) {
        if (!runGenericSwitchCheck(path, "id", id.asString())) return false;
        return super.matches(path);
    }

    @Override public ObjectTag getContext(String name) {
        return switch (name) {
            case "id"      -> id;
            case "message" -> message;
            case "json"    -> json;
            default        -> super.getContext(name);
        };
    }

    public void fireFor(String wsId, String msg) {
        this.id      = new ElementTag(wsId);
        this.message = new ElementTag(msg != null ? msg : "");
        this.json    = JsonUtil.toObjectTag(msg);
        fire();
    }
}