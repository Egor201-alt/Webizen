package com.egor201.webizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.webizen.Webizen;
import com.egor201.webizen.client.ClientManager;
import com.egor201.webizen.client.HttpClientConfig;
import com.egor201.webizen.events.HttpErrorEvent;
import com.egor201.webizen.events.HttpResponseEvent;
import org.bukkit.Bukkit;

import java.util.LinkedHashMap;
import java.util.Map;

public class HttpRequestCommand extends AbstractCommand {

    // <--[command]
    // @Name http_request
    // @Syntax http_request [method:<method>] [url:<url>] (path:<path>) (client:<id>) (body:<text>) (headers:<map>) (label:<label>) (timeout:<ms>)
    // @Required 1
    // @Maximum 8
    // @Short Sends an async HTTP request.
    // @Group Webizen
    //
    // @Description
    // Sends an HTTP request asynchronously.
    // Use 'client' to reference a named client registered with http_client (inherits base_url and headers).
    // Use 'path' with 'client' to append a path to the base_url.
    // Fires 'on http response' on success, 'on http error' on failure.
    //
    // @Usage
    // - http_request method:GET url:https://api.example.com/players label:get_players
    //
    // @Usage
    // - http_request method:POST url:https://api.example.com/ban body:<map[uuid=<player.uuid>].to_json> headers:<map[Content-Type=application/json]> label:ban
    //
    // @Usage
    // - http_request client:myapi method:GET path:/players label:get_players
    // -->

    public HttpRequestCommand() {
        setName("http_request");
        setSyntax("http_request [method:<method>] [url:<url>] (path:<path>) (client:<id>) (body:<text>) (headers:<map>) (label:<label>) (timeout:<ms>)");
        setRequiredArguments(1, 8);
    }

    @Override
    public void parseArgs(ScriptEntry se) throws InvalidArgumentsException {
        for (Argument arg : se) {
            if (!se.hasObject("method")  && arg.matchesPrefix("method"))  se.addObject("method",  arg.asElement());
            else if (!se.hasObject("url")     && arg.matchesPrefix("url"))     se.addObject("url",     arg.asElement());
            else if (!se.hasObject("path")    && arg.matchesPrefix("path"))    se.addObject("path",    arg.asElement());
            else if (!se.hasObject("client")  && arg.matchesPrefix("client"))  se.addObject("client",  arg.asElement());
            else if (!se.hasObject("body")    && arg.matchesPrefix("body"))    se.addObject("body",    arg.asElement());
            else if (!se.hasObject("headers") && arg.matchesPrefix("headers")) se.addObject("headers", arg.asType(MapTag.class));
            else if (!se.hasObject("label")   && arg.matchesPrefix("label"))   se.addObject("label",   arg.asElement());
            else if (!se.hasObject("timeout") && arg.matchesPrefix("timeout")) se.addObject("timeout", arg.asElement());
            else arg.reportUnhandled();
        }
        if (!se.hasObject("method")) throw new InvalidArgumentsException("Must specify method!");
        if (!se.hasObject("url") && !se.hasObject("client")) {
            throw new InvalidArgumentsException("Must specify url or client!");
        }
    }

    @Override
    public void execute(ScriptEntry se) {
        String method  = se.getElement("method").asString().toUpperCase();
        String body    = se.hasObject("body")    ? se.getElement("body").asString()    : null;
        String label   = se.hasObject("label")   ? se.getElement("label").asString()   : "";
        long timeout   = se.hasObject("timeout") ? se.getElement("timeout").asLong()   : 0;
        ElementTag clientId = se.getElement("client");

        Map<String, String> headers = new LinkedHashMap<>();
        MapTag headersMap = se.getObjectTag("headers");
        if (headersMap != null) {
            headersMap.entrySet().forEach(e -> headers.put(e.getKey().str, e.getValue().toString()));
        }

        String url;
        if (clientId != null) {
            HttpClientConfig cfg = Webizen.getInstance().getClientManager().getClient(clientId.asString());
            if (cfg == null) {
                Bukkit.getScheduler().runTask(Webizen.getInstance(), () ->
                    HttpErrorEvent.instance.fireFor(label, "Named client not found: " + clientId, "")
                );
                return;
            }
            String path = se.hasObject("path") ? se.getElement("path").asString() : "";
            url = cfg.resolveUrl(path);
            if (cfg.headers != null) {
                Map<String, String> merged = new LinkedHashMap<>(cfg.headers);
                merged.putAll(headers);
                headers.clear();
                headers.putAll(merged);
            }
            if (timeout == 0) timeout = cfg.timeoutMs;
        } else {
            url = se.getElement("url").asString();
        }

        final String finalUrl = url;
        final long finalTimeout = timeout;

        Webizen.getInstance().getClientManager().sendAsync(method, url, body, headers, finalTimeout,
            new ClientManager.AsyncCallback() {
                @Override public void onSuccess(int status, String respBody, Map<String, String> respHeaders) {
                    Bukkit.getScheduler().runTask(Webizen.getInstance(), () ->
                        HttpResponseEvent.instance.fireFor(label, status, respBody, respHeaders, finalUrl)
                    );
                }
                @Override public void onError(String error) {
                    Bukkit.getScheduler().runTask(Webizen.getInstance(), () ->
                        HttpErrorEvent.instance.fireFor(label, error, finalUrl)
                    );
                }
            }
        );
    }
}