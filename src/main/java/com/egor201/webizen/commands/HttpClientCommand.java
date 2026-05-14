package com.egor201.webizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.webizen.Webizen;
import com.egor201.webizen.client.HttpClientConfig;

import java.util.LinkedHashMap;
import java.util.Map;

public class HttpClientCommand extends AbstractCommand {
    // <--[command]
    // @Name http_client
    // @Syntax http_client [id:<id>] (base_url:<url>) (headers:<map>) (timeout:<ms>)
    // @Required 1
    // @Maximum 4
    // @Short Registers a named HTTP client with shared settings.
    // @Group Webizen
    //
    // @Usage
    // - http_client id:myapi base_url:https://api.example.com headers:<map[Authorization=Bearer abc|Content-Type=application/json]> timeout:5000
    // -->
    public HttpClientCommand() {
        setName("http_client");
        setSyntax("http_client [id:<id>] (base_url:<url>) (headers:<map>) (timeout:<ms>)");
        setRequiredArguments(1, 4);
    }

    @Override
    public void parseArgs(ScriptEntry se) throws InvalidArgumentsException {
        for (Argument arg : se) {
            if (!se.hasObject("id")       && arg.matchesPrefix("id"))       se.addObject("id",       arg.asElement());
            else if (!se.hasObject("base_url") && arg.matchesPrefix("base_url")) se.addObject("base_url", arg.asElement());
            else if (!se.hasObject("headers")  && arg.matchesPrefix("headers"))  se.addObject("headers",  arg.asType(MapTag.class));
            else if (!se.hasObject("timeout")  && arg.matchesPrefix("timeout"))  se.addObject("timeout",  arg.asElement());
            else arg.reportUnhandled();
        }
        if (!se.hasObject("id")) throw new InvalidArgumentsException("Must specify id!");
    }

    @Override
    public void execute(ScriptEntry se) {
        String id      = se.getElement("id").asString();
        String baseUrl = se.hasObject("base_url") ? se.getElement("base_url").asString() : "";
        long timeout   = se.hasObject("timeout")  ? se.getElement("timeout").asLong()   : 10000;

        Map<String, String> headers = new LinkedHashMap<>();
        MapTag headersMap = se.getObjectTag("headers");
        if (headersMap != null) {
            headersMap.entrySet().forEach(e -> headers.put(e.getKey().str, e.getValue().toString()));
        }

        Webizen.getInstance().getClientManager().registerClient(id, new HttpClientConfig(baseUrl, headers, timeout));
    }
}