package com.egor201.webizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.webizen.Webizen;

import java.util.LinkedHashMap;
import java.util.Map;

public class HttpRespondCommand extends AbstractCommand {

    // <--[command]
    // @Name http_respond
    // @Syntax http_respond [request:<request_id>] (status:<code>) (body:<text>) (headers:<map>)
    // @Required 1
    // @Maximum 4
    // @Short Sends an HTTP response to a pending incoming request.
    // @Group Webizen
    //
    // @Usage
    // - http_respond request:<context.request_id> status:200 body:<map[players=<server.online_players.parse[name]>].to_json>
    // - http_respond request:<context.request_id> status:404 body:<map[error=not found].to_json>
    // -->

    public HttpRespondCommand() {
        setName("http_respond");
        setSyntax("http_respond [request:<request_id>] (status:<code>) (body:<text>) (headers:<map>)");
        setRequiredArguments(1, 4);
    }

    @Override
    public void parseArgs(ScriptEntry se) throws InvalidArgumentsException {
        for (Argument arg : se) {
            if (!se.hasObject("request") && arg.matchesPrefix("request")) se.addObject("request", arg.asElement());
            else if (!se.hasObject("status")  && arg.matchesPrefix("status"))  se.addObject("status",  arg.asElement());
            else if (!se.hasObject("body")    && arg.matchesPrefix("body"))    se.addObject("body",    arg.asElement());
            else if (!se.hasObject("headers") && arg.matchesPrefix("headers")) se.addObject("headers", arg.asType(MapTag.class));
            else arg.reportUnhandled();
        }
        if (!se.hasObject("request")) throw new InvalidArgumentsException("Must specify request!");
    }

    @Override
    public void execute(ScriptEntry se) {
        String reqId  = se.getElement("request").asString();
        int status    = se.hasObject("status") ? se.getElement("status").asInt() : 200;
        String body   = se.hasObject("body")   ? se.getElement("body").asString() : "";

        Map<String, String> headers = new LinkedHashMap<>();
        MapTag headersMap = se.getObjectTag("headers");
        if (headersMap != null) {
            headersMap.entrySet().forEach(e -> headers.put(e.getKey().str, e.getValue().toString()));
        }
        if (!headers.containsKey("Content-Type")) {
            headers.put("Content-Type", "application/json");
        }

        Webizen.getInstance().getServerManager().completeRequest(reqId, status, body, headers);
    }
}