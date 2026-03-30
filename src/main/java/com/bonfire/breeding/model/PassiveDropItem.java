package com.bonfire.breeding.model;

import com.google.gson.JsonObject;

public class PassiveDropItem {
    private long id;
    private LiteItem item;
    private String amount;
    private double chance;
    private int time;
    private boolean femaleOnly;
    private LiteAnimation animation;

    public static PassiveDropItem fromJson(JsonObject json) {
        PassiveDropItem pdi = new PassiveDropItem();
        pdi.id = json.has("id") ? json.get("id").getAsLong() : System.currentTimeMillis();
        pdi.item = json.has("item") ? LiteItem.fromJson(json.getAsJsonObject("item")) : null;
        pdi.amount = json.has("amount") ? json.get("amount").getAsString() : "1";
        pdi.chance = json.has("chance") ? json.get("chance").getAsDouble() : 100.0;
        pdi.time = json.has("time") ? json.get("time").getAsInt() : 300;
        pdi.femaleOnly = json.has("female_only") && json.get("female_only").getAsBoolean();
        pdi.animation = json.has("animation") ? LiteAnimation.fromJson(json.getAsJsonObject("animation")) : null;
        return pdi;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        if (item != null) json.add("item", item.toJson());
        json.addProperty("amount", amount);
        json.addProperty("chance", chance);
        json.addProperty("time", time);
        if (animation != null) json.add("animation", animation.toJson());
        return json;
    }

    public long getId() { return id; }
    public LiteItem getItem() { return item; }
    public String getAmount() { return amount; }
    public double getChance() { return chance; }
    public int getTime() { return time; }
    public boolean isFemaleOnly() { return femaleOnly; }
    public LiteAnimation getAnimation() { return animation; }
}
