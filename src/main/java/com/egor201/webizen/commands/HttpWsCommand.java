package com.egor201.webizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.webizen.Webizen;

public class HttpWsCommand extends AbstractCommand {

    // <--[command]
    // @Name http_ws
    // @Syntax http_ws [id:<id>] (url:<url>) (action:connect/disconnect)
    // @Required 1
    // @Maximum 3
    // @Short Connects or disconnects a WebSocket connection.
    // @Group Webizen
    //
    // @Usage
    // - http_ws id:ws1 url:wss://realtime.example.com/events
    // - http_ws id:ws1 action:disconnect
    // -->

    public HttpWsCommand() {
        setName("http_ws");
        setSyntax("http_ws [id:<id>] (url:<url>) (action:connect/disconnect)");
        setRequiredArguments(1, 3);
    }

    @Override
    public void parseArgs(ScriptEntry se) throws InvalidArgumentsException {
        for (Argument arg : se) {
            if (!se.hasObject("id")     && arg.matchesPrefix("id"))     se.addObject("id",     arg.asElement());
            else if (!se.hasObject("url")    && arg.matchesPrefix("url"))    se.addObject("url",    arg.asElement());
            else if (!se.hasObject("action") && arg.matchesPrefix("action")) se.addObject("action", arg.asElement());
            else arg.reportUnhandled();
        }
        if (!se.hasObject("id")) throw new InvalidArgumentsException("Must specify id!");
    }

    @Override
    public void execute(ScriptEntry se) {
        String id     = se.getElement("id").asString();
        String action = se.hasObject("action") ? se.getElement("action").asString().toLowerCase() : "connect";

        if (action.equals("disconnect")) {
            Webizen.getInstance().getWebSocketManager().close(id, "Disconnected by script");
            return;
        }

        if (!se.hasObject("url")) {
            Webizen.getInstance().getLogger().warning("[Webizen] http_ws requires url when connecting.");
            return;
        }

        String url = se.getElement("url").asString();
        Webizen.getInstance().getWebSocketManager().connect(id, url);
    }
}