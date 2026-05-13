package com.egor201.webizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.webizen.Webizen;

public class HttpServerCommand extends AbstractCommand {

    // <--[command]
    // @Name http_server
    // @Syntax http_server [id:<id>] (action:start/stop) (port:<port>)
    // @Required 1
    // @Maximum 3
    // @Short Starts or stops an embedded HTTP server.
    // @Group Webizen
    //
    // @Usage
    // - http_server id:api port:8080
    // - http_server id:api action:stop
    // -->

    public HttpServerCommand() {
        setName("http_server");
        setSyntax("http_server [id:<id>] (action:start/stop) (port:<port>)");
        setRequiredArguments(1, 3);
    }

    @Override
    public void parseArgs(ScriptEntry se) throws InvalidArgumentsException {
        for (Argument arg : se) {
            if (!se.hasObject("id")     && arg.matchesPrefix("id"))     se.addObject("id",     arg.asElement());
            else if (!se.hasObject("action") && arg.matchesPrefix("action")) se.addObject("action", arg.asElement());
            else if (!se.hasObject("port")   && arg.matchesPrefix("port"))   se.addObject("port",   arg.asElement());
            else arg.reportUnhandled();
        }
        if (!se.hasObject("id")) throw new InvalidArgumentsException("Must specify id!");
    }

    @Override
    public void execute(ScriptEntry se) {
        String id     = se.getElement("id").asString();
        String action = se.hasObject("action") ? se.getElement("action").asString().toLowerCase() : "start";

        if (action.equals("stop")) {
            Webizen.getInstance().getServerManager().stop(id);
            return;
        }

        if (!se.hasObject("port")) {
            Webizen.getInstance().getLogger().warning("[Webizen] http_server requires port when starting.");
            return;
        }

        int port = se.getElement("port").asInt();
        boolean ok = Webizen.getInstance().getServerManager().start(id, port);
        if (!ok) {
            Webizen.getInstance().getLogger().warning("[Webizen] Server '" + id + "' already running or failed to start.");
        }
    }
}