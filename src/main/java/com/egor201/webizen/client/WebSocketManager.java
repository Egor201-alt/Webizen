package com.egor201.webizen.client;

import com.egor201.webizen.Webizen;
import com.egor201.webizen.events.HttpWsClosedEvent;
import com.egor201.webizen.events.HttpWsMessageEvent;
import com.egor201.webizen.events.HttpWsOpenEvent;
import okhttp3.*;
import okio.ByteString;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketManager {

    private final Map<String, WebSocket> activeSockets = new ConcurrentHashMap<>();
    private final OkHttpClient wsClient;

    public WebSocketManager() {
        wsClient = new OkHttpClient.Builder().build();
    }

    public void connect(String id, String url) {
        WebSocket existing = activeSockets.get(id);
        if (existing != null) existing.close(1000, "Reconnecting");

        Request request = new Request.Builder().url(url).build();
        wsClient.newWebSocket(request, new WebSocketListener() {
            @Override public void onOpen(WebSocket ws, Response response) {
                activeSockets.put(id, ws);
                Bukkit.getScheduler().runTask(Webizen.getInstance(), () ->
                    HttpWsOpenEvent.instance.fireFor(id)
                );
            }
            @Override public void onMessage(WebSocket ws, String text) {
                Bukkit.getScheduler().runTask(Webizen.getInstance(), () ->
                    HttpWsMessageEvent.instance.fireFor(id, text)
                );
            }
            @Override public void onMessage(WebSocket ws, ByteString bytes) {
                Bukkit.getScheduler().runTask(Webizen.getInstance(), () ->
                    HttpWsMessageEvent.instance.fireFor(id, bytes.utf8())
                );
            }
            @Override public void onClosed(WebSocket ws, int code, String reason) {
                activeSockets.remove(id);
                Bukkit.getScheduler().runTask(Webizen.getInstance(), () ->
                    HttpWsClosedEvent.instance.fireFor(id, code, reason)
                );
            }
            @Override public void onFailure(WebSocket ws, Throwable t, Response response) {
                activeSockets.remove(id);
                Bukkit.getScheduler().runTask(Webizen.getInstance(), () ->
                    HttpWsClosedEvent.instance.fireFor(id, -1, t.getMessage())
                );
            }
        });
    }

    public boolean send(String id, String message) {
        WebSocket ws = activeSockets.get(id);
        if (ws == null) return false;
        return ws.send(message);
    }

    public boolean close(String id, String reason) {
        WebSocket ws = activeSockets.remove(id);
        if (ws == null) return false;
        ws.close(1000, reason != null ? reason : "Closed by script");
        return true;
    }

    public boolean isConnected(String id) {
        return activeSockets.containsKey(id);
    }

    public void closeAll() {
        activeSockets.forEach((id, ws) -> ws.close(1000, "Plugin shutdown"));
        activeSockets.clear();
        wsClient.dispatcher().executorService().shutdown();
    }
}