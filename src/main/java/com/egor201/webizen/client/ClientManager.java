package com.egor201.webizen.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import okhttp3.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ClientManager {

    private static final Gson GSON = new Gson();
    private final OkHttpClient defaultClient;
    private final Map<String, HttpClientConfig> namedClients = new ConcurrentHashMap<>();

    public ClientManager() {
        defaultClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    public void registerClient(String id, HttpClientConfig config) {
        namedClients.put(id, config);
    }

    public void removeClient(String id) {
        namedClients.remove(id);
    }

    public HttpClientConfig getClient(String id) {
        return namedClients.get(id);
    }

    public void sendAsync(String method, String url, String body,
                          Map<String, String> headers, long timeoutMs,
                          AsyncCallback callback) {

        OkHttpClient client = timeoutMs > 0
            ? defaultClient.newBuilder()
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build()
            : defaultClient;

        Request.Builder rb = new Request.Builder().url(url);
        if (headers != null) headers.forEach(rb::addHeader);

        RequestBody requestBody = null;
        if (body != null && !body.isEmpty()) {
            String ct = headers != null
                ? headers.getOrDefault("Content-Type", "application/json")
                : "application/json";
            requestBody = RequestBody.create(body, MediaType.parse(ct));
        }

        switch (method.toUpperCase()) {
            case "GET"    -> rb.get();
            case "POST"   -> rb.post(requestBody != null ? requestBody : RequestBody.create(new byte[0]));
            case "PUT"    -> rb.put(requestBody != null ? requestBody : RequestBody.create(new byte[0]));
            case "PATCH"  -> rb.patch(requestBody != null ? requestBody : RequestBody.create(new byte[0]));
            case "DELETE" -> rb.delete(requestBody);
            default       -> rb.get();
        }

        client.newCall(rb.build()).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody respBody = response.body()) {
                    String rawBody = respBody != null ? respBody.string() : "";
                    Map<String, String> respHeaders = new LinkedHashMap<>();
                    response.headers().forEach(p -> respHeaders.put(p.getFirst(), p.getSecond()));
                    callback.onSuccess(response.code(), rawBody, respHeaders);
                }
            }
        });
    }

    public SyncResult sendSync(String method, String url, String body,
                                Map<String, String> headers) throws IOException {
        Request.Builder rb = new Request.Builder().url(url);
        if (headers != null) headers.forEach(rb::addHeader);
        RequestBody requestBody = body != null && !body.isEmpty()
            ? RequestBody.create(body, MediaType.parse("application/json")) : null;
        switch (method.toUpperCase()) {
            case "POST"   -> rb.post(requestBody != null ? requestBody : RequestBody.create(new byte[0]));
            case "PUT"    -> rb.put(requestBody != null ? requestBody : RequestBody.create(new byte[0]));
            case "PATCH"  -> rb.patch(requestBody != null ? requestBody : RequestBody.create(new byte[0]));
            case "DELETE" -> rb.delete(requestBody);
            default       -> rb.get();
        }
        try (Response response = defaultClient.newCall(rb.build()).execute()) {
            String rawBody = response.body() != null ? response.body().string() : "";
            Map<String, String> respHeaders = new LinkedHashMap<>();
            response.headers().forEach(p -> respHeaders.put(p.getFirst(), p.getSecond()));
            return new SyncResult(response.code(), rawBody, respHeaders);
        }
    }

    public static JsonElement parseJson(String raw) {
        try { return GSON.fromJson(raw, JsonElement.class); } catch (Exception e) { return null; }
    }

    public void closeAll() {
        defaultClient.dispatcher().executorService().shutdown();
        defaultClient.connectionPool().evictAll();
    }

    public record SyncResult(int status, String body, Map<String, String> headers) {}

    public interface AsyncCallback {
        void onSuccess(int status, String body, Map<String, String> headers);
        void onError(String error);
    }
}