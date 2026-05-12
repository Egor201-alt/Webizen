package com.egor201.webizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.webizen.Webizen;

public class HttpWsSendCommand extends AbstractCommand {

    // <--[command]
    // @Name http_ws_send
    // @Syntax http_ws_send [id:<id>] [message:<text>] (close:true/false)
    // @Required 2
    // @Maximum 3
    // @Short Sends a message over an active WebSocket connection.
    // @Group Webizen
    //
    // @Usage
    // - http_ws_send id:ws1 message:<map[type=ping].to_json>
    // - http_ws_send id:ws1 message:<map[type=bye].to_json> close:true
    // -->

    public HttpWsSendCommand() {
        setName("http_ws_send");
        setSyntax("http_ws_send [id:<id>] [message:<text>] (close:true/false)");
        setRequiredArguments(2, 3);
    }

    @Override
    public void parseArgs(ScriptEntry se) throws InvalidArgumentsException {
        for (Argument arg : se) {
            if (!se.hasObject("id")      && arg.matchesPrefix("id"))      se.addObject("id",      arg.asElement());
            else if (!se.hasObject("message") && arg.matchesPrefix("message")) se.addObject("message", arg.asElement());
            else if (!se.hasObject("close")   && arg.matchesPrefix("close"))   se.addObject("close",   arg.asElement());
            else arg.reportUnhandled();
        }
        if (!se.hasObject("id") || !se.hasObject("message")) {
            throw new InvalidArgumentsException("Must specify id and message!");
        }
    }

    @Override
    public void execute(ScriptEntry se) {
        String id      = se.getElement("id").asString();
        String message = se.getElement("message").asString();
        boolean close  = se.hasObject("close") && se.getElement("close").asBoolean();

        Webizen.getInstance().getWebSocketManager().send(id, message);
        if (close) {
            Webizen.getInstance().getWebSocketManager().close(id, "Closed by script");
        }
    }
}