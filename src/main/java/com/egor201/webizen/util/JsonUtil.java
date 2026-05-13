package com.egor201.webizen.util;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.google.gson.*;

public class JsonUtil {

    public static ObjectTag toObjectTag(String raw) {
        if (raw == null || raw.isBlank()) return new ElementTag("");
        try {
            JsonElement el = JsonParser.parseString(raw);
            return convert(el);
        } catch (Exception e) {
            return new ElementTag(raw);
        }
    }

    private static ObjectTag convert(JsonElement el) {
        if (el == null || el.isJsonNull()) return new ElementTag("null");
        if (el.isJsonPrimitive())          return new ElementTag(el.getAsString());
        if (el.isJsonArray()) {
            ListTag list = new ListTag();
            for (JsonElement item : el.getAsJsonArray()) list.addObject(convert(item));
            return list;
        }
        if (el.isJsonObject()) {
            MapTag map = new MapTag();
            for (var entry : el.getAsJsonObject().entrySet())
                map.putObject(entry.getKey(), convert(entry.getValue()));
            return map;
        }
        return new ElementTag(el.toString());
    }
}