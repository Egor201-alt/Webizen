package com.egor201.webizen.events;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;

public class HttpErrorEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // http error
    //
    // @Group Webizen
    //
    // @Switch label:<label> to only fire for a specific request label.
    //
    // @Triggers when an async http_request fails (network error, timeout, etc).
    //
    // @Context
    // <context.label> - the label from http_request
    // <context.error> - the error message
    // <context.url>   - the request URL
    // -->

    public static HttpErrorEvent instance;
    private ElementTag label, error, url;

    public HttpErrorEvent() {
        instance = this;
        registerCouldMatcher("http error");
        registerSwitches("label");
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (!runGenericSwitchCheck(path, "label", label.asString())) return false;
        return super.matches(path);
    }

    @Override
    public ObjectTag getContext(String name) {
        return switch (name) {
            case "label" -> label;
            case "error" -> error;
            case "url"   -> url;
            default      -> super.getContext(name);
        };
    }

    public void fireFor(String lbl, String errorMsg, String requestUrl) {
        this.label = new ElementTag(lbl != null ? lbl : "");
        this.error = new ElementTag(errorMsg != null ? errorMsg : "");
        this.url   = new ElementTag(requestUrl != null ? requestUrl : "");
        fire();
    }
}