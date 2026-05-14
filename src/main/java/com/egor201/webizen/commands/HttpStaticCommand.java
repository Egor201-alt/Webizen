package com.egor201.webizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.webizen.Webizen;

public class HttpStaticCommand extends AbstractCommand {
    // <--[command]
    // @Name http_static
    // @Syntax http_static [server:<id>] [path:<url_path>] [folder:<folder_path>]
    // @Required 3
    // @Maximum 3
    // @Short Serves static files from a folder on the given URL path.
    // @Group Webizen
    //
    // @Usage
    // - http_static server:api path:/public folder:plugins/Webizen/public
    // -->
    public HttpStaticCommand() {
        setName("http_static");
        setSyntax("http_static [server:<id>] [path:<url_path>] [folder:<folder_path>]");
        setRequiredArguments(3, 3);
    }

    @Override
    public void parseArgs(ScriptEntry se) throws InvalidArgumentsException {
        for (Argument arg : se) {
            if (!se.hasObject("server") && arg.matchesPrefix("server")) se.addObject("server", arg.asElement());
            else if (!se.hasObject("path")   && arg.matchesPrefix("path"))   se.addObject("path",   arg.asElement());
            else if (!se.hasObject("folder") && arg.matchesPrefix("folder")) se.addObject("folder", arg.asElement());
            else arg.reportUnhandled();
        }
        if (!se.hasObject("server") || !se.hasObject("path") || !se.hasObject("folder")) {
            throw new InvalidArgumentsException("Must specify server, path, and folder!");
        }
    }

    @Override
    public void execute(ScriptEntry se) {
        String server = se.getElement("server").asString();
        String path   = se.getElement("path").asString();
        String folder = se.getElement("folder").asString();
        Webizen.getInstance().getServerManager().addStatic(server, path, folder);
    }
}