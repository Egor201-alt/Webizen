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
    // @Syntax http_middleware_next [request:<request_id>]
    // @Required 1
    // @Maximum 1
    // @Short Passes the request from middleware to its matched route handler.
    // @Group Webizen
    //
    // @Usage
    // on http request label:auth_check:
    //   - if <context.headers.get[X-Api-Key]> != <server.flag[api_key]>:
    //     - http_respond request:<context.request_id> status:401 body:<map[error=unauthorized].to_json>
    //     - http_middleware_stop request:<context.request_id>
    //     - stop
    //   - http_middleware_next request:<context.request_id>
    // -->
    public HttpMiddlewareNextCommand() {
        setName("http_middleware_next");
        setSyntax("http_middleware_next [request:<request_id>]");
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
        final String reqId = se.getElement("request").asString();

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