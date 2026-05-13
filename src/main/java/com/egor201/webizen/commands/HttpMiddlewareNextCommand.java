package com.egor201.webizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.webizen.Webizen;
import com.egor201.webizen.events.HttpRequestEvent;
import com.egor201.webizen.server.RequestContext;
import org.bukkit.Bukkit;

public class HttpMiddlewareNextCommand extends AbstractCommand {

    // <--[command]
    // @Name http_middleware_next
    // @Syntax http_middleware_next
    // @Required 0
    // @Maximum 0
    // @Short Passes the request from middleware to its matched route handler.
    // @Group Webizen
    // -->

    public HttpMiddlewareNextCommand() {
        setName("http_middleware_next");
        setSyntax("http_middleware_next");
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
        if (ctx == null || ctx.isCompleted()) return;

        String routeLabel = ctx.getPendingRouteLabel();
        if (routeLabel == null) return;

        Bukkit.getScheduler().runTask(Webizen.getInstance(), () ->
            HttpRequestEvent.instance.fireFor(
                reqId, ctx.getMethod(), ctx.getPath(),
                ctx.getParams(), ctx.getQuery(), ctx.getHeaders(),
                ctx.getBody(), ctx.getIp(), ctx.getServerId(),
                routeLabel, false
            )
        );
    }
}