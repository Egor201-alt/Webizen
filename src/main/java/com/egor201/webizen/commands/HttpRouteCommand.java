package com.egor201.webizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.webizen.Webizen;

public class HttpRouteCommand extends AbstractCommand {

    // <--[command]
    // @Name http_route
    // @Syntax http_route [server:<id>] [method:<method>] [path:<path>] [label:<label>]
    // @Required 4
    // @Maximum 4
    // @Short Registers a route on an HTTP server.
    // @Group Webizen
    //
    // @Usage
    // - http_route server:api method:GET path:/api/players label:route_players
    // - http_route server:api method:GET path:/api/player/{uuid} label:route_player
    // - http_route server:api method:POST path:/api/ban label:route_ban
    // -->

    public HttpRouteCommand() {
        setName("http_route");
        setSyntax("http_route [server:<id>] [method:<method>] [path:<path>] [label:<label>]");
        setRequiredArguments(4, 4);
    }

    @Override
    public void parseArgs(ScriptEntry se) throws InvalidArgumentsException {
        for (Argument arg : se) {
            if (!se.hasObject("server") && arg.matchesPrefix("server")) se.addObject("server", arg.asElement());
            else if (!se.hasObject("method") && arg.matchesPrefix("method")) se.addObject("method", arg.asElement());
            else if (!se.hasObject("path")   && arg.matchesPrefix("path"))   se.addObject("path",   arg.asElement());
            else if (!se.hasObject("label")  && arg.matchesPrefix("label"))  se.addObject("label",  arg.asElement());
            else arg.reportUnhandled();
        }
        if (!se.hasObject("server") || !se.hasObject("method") || !se.hasObject("path") || !se.hasObject("label")) {
            throw new InvalidArgumentsException("Must specify server, method, path, and label!");
        }
    }

    @Override
    public void execute(ScriptEntry se) {
        String server = se.getElement("server").asString();
        String method = se.getElement("method").asString().toUpperCase();
        String path   = se.getElement("path").asString();
        String label  = se.getElement("label").asString();
        Webizen.getInstance().getServerManager().addRoute(server, method, path, label);
    }
}