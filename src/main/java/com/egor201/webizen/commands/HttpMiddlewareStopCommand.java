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
    // @Syntax http_middleware_stop
    // @Required 0
    // @Maximum 0
    // @Short Stops the middleware chain — request will not reach the route handler.
    // @Group Webizen
    // -->

    public HttpMiddlewareStopCommand() {
        setName("http_middleware_stop");
        setSyntax("http_middleware_stop");
        setRequiredArguments(0, 0);
    }

    @Override
    public void parseArgs(ScriptEntry se) throws InvalidArgumentsException {}

    @Override
    public void execute(ScriptEntry se) {
        String reqId = null;
        try {
            var def = se.getResidingQueue().getDefinition("request_id");
            if (def != null) reqId = def.toString();
        } catch (Exception ignored) {}

        if (reqId == null) return;

        RequestContext ctx = Webizen.getInstance().getServerManager().getRequest(reqId);
        if (ctx != null) ctx.setPendingRouteLabel(null);
    }
}