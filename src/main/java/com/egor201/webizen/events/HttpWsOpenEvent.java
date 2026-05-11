package com.egor201.webizen.events;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;

public class HttpWsOpenEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // http ws open
    // @Group Webizen
    // @Switch id:<id>
    // @Context
    // <context.id> - the WebSocket connection ID
    // -->

    public static HttpWsOpenEvent instance;
    private ElementTag id;

    public HttpWsOpenEvent() { instance = this; registerCouldMatcher("http ws open"); registerSwitches("id"); }
    @Override public boolean matches(ScriptPath path) {
        if (!runGenericSwitchCheck(path, "id", id.asString())) return false;
        return super.matches(path);
    }
    @Override public ObjectTag getContext(String name) { return name.equals("id") ? id : super.getContext(name); }
    public void fireFor(String wsId) { this.id = new ElementTag(wsId); fire(); }
}