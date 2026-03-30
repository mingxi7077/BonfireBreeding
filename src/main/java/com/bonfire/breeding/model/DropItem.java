package com.bonfire.breeding.model;

import com.google.gson.JsonObject;

public class DropItem {
    private int levelRequired;
    private LiteItem item;
    private String amount;
    private double chance;
    private boolean femaleOnly;
    private LiteAnimation animation;

    public static DropItem fromJson(JsonObject json) {
        DropItem drop = new DropItem();
        drop.levelRequired = json.has("level_required") ? json.get("level_required").getAsInt() : 0;
        drop.item = json.has("item") ? LiteItem.fromJson(json.getAsJsonObject("item")) : null;
        drop.amount = json.has("amount") ? json.get("amount").getAsString() : "1";
        drop.chance = json.has("chance") ? json.get("chance").getAsDouble() : 100.0;
        drop.femaleOnly = json.has("female_only") && json.get("female_only").getAsBoolean();
        drop.animation = json.has("animation") ? LiteAnimation.fromJson(json.getAsJsonObject("animation")) : null;
        return drop;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("level_required", levelRequired);
        if (item != null) json.add("item", item.toJson());
        json.addProperty("amount", amount);
        json.addProperty("chance", chance);
        if (animation != null) json.add("animation", animation.toJson());
        return json;
    }

    public int getLevelRequired() { return levelRequired; }
    public LiteItem getItem() { return item; }
    public String getAmount() { return amount; }
    public double getChance() { return chance; }
    public boolean isFemaleOnly() { return femaleOnly; }
    public LiteAnimation getAnimation() { return animation; }
}
