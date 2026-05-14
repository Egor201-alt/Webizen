package com.egor201.webizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.webizen.Webizen;
import com.egor201.webizen.server.RequestContext;

public class HttpMiddlewareStopCommand extends AbstractCommand {
    // <--[command]
    // @Name http_middleware_stop
    // @Syntax http_middleware_stop [request:<request_id>]
    // @Required 1
    // @Maximum 1
    // @Short Stops the middleware chain — request will not reach the route handler.
    // @Group Webizen
    // -->
    public HttpMiddlewareStopCommand() {
        setName("http_middleware_stop");
        setSyntax("http_middleware_stop [request:<request_id>]");
        setRequiredArguments(1, 1);
    }

    @Override
    public void parseArgs(ScriptEntry se) throws InvalidArgumentsException {
        for (Argument arg : se) {
            if (!se.hasObject("request") && arg.matchesPrefix("request")) se.addObject("request", arg.asElement());
            else arg.reportUnhandled();
        }
        if (!se.hasObject("request")) throw new InvalidArgumentsException("Must specify request!");
    }

    @Override
    public void execute(ScriptEntry se) {
        String reqId = se.getElement("request").asString();
        RequestContext ctx = Webizen.getInstance().getServerManager().getRequest(reqId);
        if (ctx != null) ctx.setPendingRouteLabel(null);
    }
}