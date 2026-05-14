package com.egor201.webizen.events;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import java.util.Map;

public class HttpResponseEvent extends ScriptEvent {
    // <--[event]
    // @Events
    // http response
    // @Group Webizen
    // @Switch label:<label>
    // @Switch status:<code>
    // @Context
    // <context.label>   - label from http_request
    // <context.status>  - HTTP status code
    // <context.body>    - raw response body
    // <context.json>    - same as body — raw JSON string, use http_json_value to extract fields
    // <context.headers> - MapTag of response headers
    // <context.url>     - request URL
    // -->
    public static HttpResponseEvent instance;
    private ElementTag label, status, body, url;
    private MapTag headers;

    public HttpResponseEvent() {
        instance = this;
        registerCouldMatcher("http response");
        registerSwitches("label", "status");
    }

    @Override public boolean matches(ScriptPath path) {
        if (!runGenericSwitchCheck(path, "label",  label.asString()))  return false;
        if (!runGenericSwitchCheck(path, "status", status.asString())) return false;
        return super.matches(path);
    }

    @Override public ObjectTag getContext(String name) {
        return switch (name) {
            case "label"   -> label;
            case "status"  -> status;
            case "body"    -> body;
            case "json"    -> body;
            case "headers" -> headers;
            case "url"     -> url;
            default        -> super.getContext(name);
        };
    }

    public void fireFor(String lbl, int statusCode, String rawBody, Map<String, String> responseHeaders, String requestUrl) {
        this.label   = new ElementTag(lbl != null ? lbl : "");
        this.status  = new ElementTag(statusCode);
        this.body    = new ElementTag(rawBody != null ? rawBody : "");
        this.url     = new ElementTag(requestUrl != null ? requestUrl : "");
        this.headers = new MapTag();
        if (responseHeaders != null) responseHeaders.forEach((k, v) -> this.headers.putObject(k, new ElementTag(v)));
        fire();
    }
}