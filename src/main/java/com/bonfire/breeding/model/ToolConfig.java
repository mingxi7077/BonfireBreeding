package com.bonfire.breeding.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ToolConfig {
    private long id;
    private LiteItem item;
    private int time;
    private boolean baby;
    private List<DropItem> drops;

    public static ToolConfig fromJson(JsonObject json) {
        ToolConfig tc = new ToolConfig();
        tc.id = json.has("id") ? json.get("id").getAsLong() : System.currentTimeMillis();
        tc.item = json.has("item") ? LiteItem.fromJson(json.getAsJsonObject("item")) : null;
        tc.time = json.has("time") ? json.get("time").getAsInt() : 300;
        tc.baby = json.has("baby") && json.get("baby").getAsBoolean();
        tc.drops = new ArrayList<>();
        if (json.has("drops")) {
            JsonArray arr = json.getAsJsonArray("drops");
            for (JsonElement el : arr) {
                tc.drops.add(DropItem.fromJson(el.getAsJsonObject()));
            }
        }
        return tc;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        if (item != null) json.add("item", item.toJson());
        json.addProperty("time", time);
        json.addProperty("baby", baby);
        JsonArray arr = new JsonArray();
        for (DropItem d : drops) arr.add(d.toJson());
        json.add("drops", arr);
        return json;
    }

    public long getId() { return id; }
    public LiteItem getItem() { return item; }
    public int getTime() { return time; }
    public boolean isBaby() { return baby; }
    public List<DropItem> getDrops() { return drops; }
}
