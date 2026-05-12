package com.egor201.webizen;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.egor201.webizen.client.ClientManager;
import com.egor201.webizen.client.WebSocketManager;
import com.egor201.webizen.commands.*;
import com.egor201.webizen.events.*;
import com.egor201.webizen.server.ServerManager;
import com.egor201.webizen.tags.WebizenTags;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Webizen extends JavaPlugin {

    private static Webizen instance;
    private ClientManager clientManager;
    private ServerManager serverManager;
    private WebSocketManager webSocketManager;

    @Override
    public void onEnable() {
        instance = this;
        if (Bukkit.getPluginManager().getPlugin("Denizen") == null) {
            getLogger().severe("Denizen not found! Disabling Webizen.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        clientManager    = new ClientManager();
        serverManager    = new ServerManager();
        webSocketManager = new WebSocketManager();

        DenizenCore.commandRegistry.registerCommand(HttpRequestCommand.class);
        DenizenCore.commandRegistry.registerCommand(HttpClientCommand.class);
        DenizenCore.commandRegistry.registerCommand(HttpRespondCommand.class);
        DenizenCore.commandRegistry.registerCommand(HttpServerCommand.class);
        DenizenCore.commandRegistry.registerCommand(HttpRouteCommand.class);
        DenizenCore.commandRegistry.registerCommand(HttpMiddlewareCommand.class);
        DenizenCore.commandRegistry.registerCommand(HttpMiddlewareNextCommand.class);
        DenizenCore.commandRegistry.registerCommand(HttpMiddlewareStopCommand.class);
        DenizenCore.commandRegistry.registerCommand(HttpStaticCommand.class);
        DenizenCore.commandRegistry.registerCommand(HttpWsCommand.class);
        DenizenCore.commandRegistry.registerCommand(HttpWsSendCommand.class);

        ScriptEvent.registerScriptEvent(new HttpResponseEvent());
        ScriptEvent.registerScriptEvent(new HttpErrorEvent());
        ScriptEvent.registerScriptEvent(new HttpRequestEvent());
        ScriptEvent.registerScriptEvent(new HttpWsMessageEvent());
        ScriptEvent.registerScriptEvent(new HttpWsOpenEvent());
        ScriptEvent.registerScriptEvent(new HttpWsClosedEvent());

        WebizenTags.register();
        getLogger().info("Webizen enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (serverManager    != null) serverManager.stopAll();
        if (webSocketManager != null) webSocketManager.closeAll();
        if (clientManager    != null) clientManager.closeAll();
        getLogger().info("Webizen disabled.");
    }

    public static Webizen getInstance()             { return instance; }
    public ClientManager getClientManager()         { return clientManager; }
    public ServerManager getServerManager()         { return serverManager; }
    public WebSocketManager getWebSocketManager()   { return webSocketManager; }
}