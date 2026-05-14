package com.egor201.webizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.webizen.Webizen;

public class HttpMiddlewareCommand extends AbstractCommand {
    // <--[command]
    // @Name http_middleware
    // @Syntax http_middleware [server:<id>] [label:<label>]
    // @Required 2
    // @Maximum 2
    // @Short Registers a middleware handler for all routes on an HTTP server.
    // @Group Webizen
    //
    // @Description
    // All incoming requests will first fire 'on http request label:<label>' before routing.
    // Inside the middleware script use http_middleware_next to pass to the route,
    // or http_middleware_stop + http_respond to reject the request.
    //
    // @Usage
    // - http_middleware server:api label:auth_check
    //
    // on http request label:auth_check:
    //   - if <context.headers.get[X-Api-Key]> != <server.flag[api_key]>:
    //     - http_respond request:<context.request_id> status:401 body:<map[error=unauthorized].to_json>
    //     - http_middleware_stop
    //     - stop
    //   - http_middleware_next
    // -->
    public HttpMiddlewareCommand() {
        setName("http_middleware");
        setSyntax("http_middleware [server:<id>] [label:<label>]");
        setRequiredArguments(2, 2);
    }

    @Override
    public void parseArgs(ScriptEntry se) throws InvalidArgumentsException {
        for (Argument arg : se) {
            if (!se.hasObject("server") && arg.matchesPrefix("server")) se.addObject("server", arg.asElement());
            else if (!se.hasObject("label") && arg.matchesPrefix("label")) se.addObject("label", arg.asElement());
            else arg.reportUnhandled();
        }
        if (!se.hasObject("server") || !se.hasObject("label")) {
            throw new InvalidArgumentsException("Must specify server and label!");
        }
    }

    @Override
    public void execute(ScriptEntry se) {
        String server = se.getElement("server").asString();
        String label  = se.getElement("label").asString();
        Webizen.getInstance().getServerManager().setMiddleware(server, label);
    }
}