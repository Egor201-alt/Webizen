package com.egor201.webizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.webizen.Webizen;

public class HttpServerCommand extends AbstractCommand {
    // <--[command]
    // @Name http_server
    // @Syntax http_server [id:<id>] (action:start/stop) (port:<port>) (host:<host>)
    // @Required 1
    // @Maximum 4
    // @Short Starts or stops an embedded HTTP server.
    // @Group Webizen
    //
    // @Description
    // Starts an HTTP server on the specified port.
    // Use host:127.0.0.1 to bind only to localhost (recommended for internal APIs).
    // Default host is 0.0.0.0 (all interfaces — accessible from outside).
    //
    // @Usage
    // - http_server id:api port:8080 host:127.0.0.1
    // - http_server id:api action:stop
    // -->
    public HttpServerCommand() {
        setName("http_server");
        setSyntax("http_server [id:<id>] (action:start/stop) (port:<port>) (host:<host>)");
        setRequiredArguments(1, 4);
    }

    @Override
    public void parseArgs(ScriptEntry se) throws InvalidArgumentsException {
        for (Argument arg : se) {
            if (!se.hasObject("id")     && arg.matchesPrefix("id"))     se.addObject("id",     arg.asElement());
            else if (!se.hasObject("action") && arg.matchesPrefix("action")) se.addObject("action", arg.asElement());
            else if (!se.hasObject("port")   && arg.matchesPrefix("port"))   se.addObject("port",   arg.asElement());
            else if (!se.hasObject("host")   && arg.matchesPrefix("host"))   se.addObject("host",   arg.asElement());
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

        int port    = se.getElement("port").asInt();
        String host = se.hasObject("host") ? se.getElement("host").asString() : "0.0.0.0";

        boolean ok = Webizen.getInstance().getServerManager().start(id, port, host);
        if (!ok) {
            Webizen.getInstance().getLogger().warning("[Webizen] Server '" + id + "' already running or failed to start.");
        }
    }
}