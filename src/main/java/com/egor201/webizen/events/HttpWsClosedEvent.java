package com.egor201.webizen.events;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;

public class HttpWsClosedEvent extends ScriptEvent {
    // <--[event]
    // @Events
    // http ws closed
    // @Group Webizen
    // @Switch id:<id>
    // @Context
    // <context.id>     - the WebSocket connection ID
    // <context.code>   - close code (1000 = normal, -1 = error)
    // <context.reason> - close reason or error message
    // -->
    public static HttpWsClosedEvent instance;
    private ElementTag id, code, reason;

    public HttpWsClosedEvent() { instance = this; registerCouldMatcher("http ws closed"); registerSwitches("id"); }
    @Override public boolean matches(ScriptPath path) {
        if (!runGenericSwitchCheck(path, "id", id.asString())) return false;
        return super.matches(path);
    }
    @Override public ObjectTag getContext(String name) {
        return switch (name) {
            case "id"     -> id;
            case "code"   -> code;
            case "reason" -> reason;
            default       -> super.getContext(name);
        };
    }
    public void fireFor(String wsId, int closeCode, String closeReason) {
        this.id     = new ElementTag(wsId);
        this.code   = new ElementTag(closeCode);
        this.reason = new ElementTag(closeReason != null ? closeReason : "");
        fire();
    }
}