package com.egor201.webizen.events;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.MapTag;

import java.util.Map;

public class HttpRequestEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // http request
    // @Group Webizen
    // @Switch label:<label>
    // @Switch method:<method>
    // @Switch server:<id>
    // @Context
    // <context.request_id> - UUID used to send a response via http_respond
    // <context.method>     - HTTP method (GET, POST...)
    // <context.path>       - request path (/api/players)
    // <context.params>     - MapTag of URL path parameters ({uuid} etc.)
    // <context.query>      - MapTag of query string parameters (?foo=bar)
    // <context.headers>    - MapTag of request headers
    // <context.body>       - raw request body
    // <context.body_json>  - raw JSON body string (same as body, safe — use json_value[path] to extract)
    // <context.ip>         - client IP address
    // <context.server>     - the server ID
    // <context.label>      - the matched route or middleware label
    // -->

    public static HttpRequestEvent instance;
    private ElementTag requestId, method, path, body, ip, server, label;
    private MapTag params, query, headers;

    public HttpRequestEvent() {
        instance = this;
        registerCouldMatcher("http request");
        registerSwitches("label", "method", "server");
    }

    @Override public boolean matches(ScriptPath scriptPath) {
        if (!runGenericSwitchCheck(scriptPath, "label",  label.asString()))  return false;
        if (!runGenericSwitchCheck(scriptPath, "method", method.asString())) return false;
        if (!runGenericSwitchCheck(scriptPath, "server", server.asString())) return false;
        return super.matches(scriptPath);
    }

    @Override public ObjectTag getContext(String name) {
        return switch (name) {
            case "request_id" -> requestId;
            case "method"     -> method;
            case "path"       -> path;
            case "params"     -> params;
            case "query"      -> query;
            case "headers"    -> headers;
            case "body"       -> body;
            case "body_json"  -> body;
            case "ip"         -> ip;
            case "server"     -> server;
            case "label"      -> label;
            default           -> super.getContext(name);
        };
    }

    public void fireFor(String reqId, String httpMethod, String reqPath,
                        Map<String, String> pathParams, Map<String, String> queryParams,
                        Map<String, String> reqHeaders, String rawBody,
                        String clientIp, String serverId, String routeLabel, boolean isMiddleware) {
        this.requestId = new ElementTag(reqId);
        this.method    = new ElementTag(httpMethod);
        this.path      = new ElementTag(reqPath);
        this.body      = new ElementTag(rawBody != null ? rawBody : "");
        this.ip        = new ElementTag(clientIp != null ? clientIp : "");
        this.server    = new ElementTag(serverId != null ? serverId : "");
        this.label     = new ElementTag(routeLabel != null ? routeLabel : "");

        this.params  = new MapTag();
        if (pathParams  != null) pathParams.forEach((k, v)  -> this.params.putObject(k, new ElementTag(v)));
        this.query   = new MapTag();
        if (queryParams != null) queryParams.forEach((k, v) -> this.query.putObject(k, new ElementTag(v)));
        this.headers = new MapTag();
        if (reqHeaders  != null) reqHeaders.forEach((k, v)  -> this.headers.putObject(k, new ElementTag(v)));

        fire();
    }
}