package com.bonfire.breeding.model;

import com.google.gson.JsonObject;

public class BreedingItem {
    private LiteItem item;
    private int time;
    private double chance;
    private boolean needPartner;
    private int courtshipDuration;

    public static BreedingItem fromJson(JsonObject json) {
        BreedingItem bi = new BreedingItem();
        bi.item = json.has("item") ? LiteItem.fromJson(json.getAsJsonObject("item")) : null;
        bi.time = json.has("time") ? json.get("time").getAsInt() : 600;
        bi.chance = json.has("chance") ? json.get("chance").getAsDouble() : 50.0;
        bi.needPartner = !json.has("need_partner") || json.get("need_partner").getAsBoolean();
        bi.courtshipDuration = json.has("courtship_duration") ? json.get("courtship_duration").getAsInt() : 45;
        return bi;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (item != null) json.add("item", item.toJson());
        json.addProperty("time", time);
        json.addProperty("chance", chance);
        json.addProperty("need_partner", needPartner);
        json.addProperty("courtship_duration", courtshipDuration);
        return json;
    }

    public LiteItem getItem() { return item; }
    public int getTime() { return time; }
    public double getChance() { return chance; }
    public boolean isNeedPartner() { return needPartner; }
    public int getCourtshipDuration() { return courtshipDuration; }
}
