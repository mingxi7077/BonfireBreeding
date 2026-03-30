package com.bonfire.breeding.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class AnimalConfig {
    private boolean defaultEntity;
    private String configName;
    private String type;
    private String displayName;
    private String rarity;
    private int timeGrowth;
    private int harvestCooldown;
    private int harvestExp;
    private List<BreedingItem> breedingItems;
    private LiteItem egg;
    private List<DropItem> dropItems;
    private List<DropItem> deathDrops;
    private List<PassiveDropItem> passiveDropItems;
    private List<ToolConfig> tools;
    private String ambientParticle;
    private int ambientInterval;
    private String fileName;
    private MoodConfig moodConfig;
    private int matingDuration;
    private double breedCooldownRatio;
    private HungerConfig hungerConfig;

    public static AnimalConfig fromJson(JsonObject json, String fileName) {
        AnimalConfig ac = new AnimalConfig();
        ac.fileName = fileName;
        ac.defaultEntity = json.has("default_entity") && json.get("default_entity").getAsBoolean();
        ac.configName = json.has("config_name") ? json.get("config_name").getAsString() : "unknown";
        ac.type = json.has("type") ? json.get("type").getAsString() : "COW";
        ac.displayName = json.has("display_name") ? json.get("display_name").getAsString() : ac.configName;
        ac.rarity = json.has("rarity") ? json.get("rarity").getAsString() : "normal";
        ac.timeGrowth = json.has("time_growth") ? json.get("time_growth").getAsInt() : 300;
        ac.harvestCooldown = json.has("harvest_cooldown") ? json.get("harvest_cooldown").getAsInt() : 0;
        ac.harvestExp = json.has("harvest_exp") ? json.get("harvest_exp").getAsInt() : 2;

        ac.breedingItems = new ArrayList<>();
        if (json.has("breeding_items")) {
            for (JsonElement el : json.getAsJsonArray("breeding_items")) {
                ac.breedingItems.add(BreedingItem.fromJson(el.getAsJsonObject()));
            }
        }

        ac.egg = json.has("egg") ? LiteItem.fromJson(json.getAsJsonObject("egg")) : null;

        ac.dropItems = new ArrayList<>();
        if (json.has("drop_items")) {
            for (JsonElement el : json.getAsJsonArray("drop_items")) {
                ac.dropItems.add(DropItem.fromJson(el.getAsJsonObject()));
            }
        }

        ac.deathDrops = new ArrayList<>();
        if (json.has("death_drops")) {
            for (JsonElement el : json.getAsJsonArray("death_drops")) {
                ac.deathDrops.add(DropItem.fromJson(el.getAsJsonObject()));
            }
        }

        ac.passiveDropItems = new ArrayList<>();
        if (json.has("passive_drop_items")) {
            for (JsonElement el : json.getAsJsonArray("passive_drop_items")) {
                ac.passiveDropItems.add(PassiveDropItem.fromJson(el.getAsJsonObject()));
            }
        }

        ac.tools = new ArrayList<>();
        if (json.has("tools")) {
            for (JsonElement el : json.getAsJsonArray("tools")) {
                ac.tools.add(ToolConfig.fromJson(el.getAsJsonObject()));
            }
        }

        if (json.has("ambient_effects")) {
            JsonObject ae = json.getAsJsonObject("ambient_effects");
            ac.ambientParticle = ae.has("particle") ? ae.get("particle").getAsString() : null;
            ac.ambientInterval = ae.has("interval_ticks") ? ae.get("interval_ticks").getAsInt() : 100;
        }

        ac.moodConfig = json.has("mood") ? MoodConfig.fromJson(json.getAsJsonObject("mood")) : new MoodConfig();

        ac.matingDuration = json.has("mating_duration") ? json.get("mating_duration").getAsInt() : 5;
        ac.breedCooldownRatio = json.has("breed_cooldown_ratio") ? json.get("breed_cooldown_ratio").getAsDouble() : 0.5;

        ac.hungerConfig = json.has("hunger") ? HungerConfig.fromJson(json.getAsJsonObject("hunger")) : new HungerConfig();

        return ac;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("default_entity", defaultEntity);
        json.addProperty("config_name", configName);
        json.addProperty("type", type);
        json.addProperty("display_name", displayName);
        json.addProperty("rarity", rarity);
        json.addProperty("time_growth", timeGrowth);
        if (harvestCooldown > 0) json.addProperty("harvest_cooldown", harvestCooldown);
        json.addProperty("harvest_exp", harvestExp);

        JsonArray bi = new JsonArray();
        for (BreedingItem b : breedingItems) bi.add(b.toJson());
        json.add("breeding_items", bi);

        if (egg != null) json.add("egg", egg.toJson());

        JsonArray di = new JsonArray();
        for (DropItem d : dropItems) di.add(d.toJson());
        json.add("drop_items", di);

        if (deathDrops != null && !deathDrops.isEmpty()) {
            JsonArray dd = new JsonArray();
            for (DropItem d : deathDrops) dd.add(d.toJson());
            json.add("death_drops", dd);
        }

        JsonArray pdi = new JsonArray();
        for (PassiveDropItem p : passiveDropItems) pdi.add(p.toJson());
        json.add("passive_drop_items", pdi);

        JsonArray t = new JsonArray();
        for (ToolConfig tc : tools) t.add(tc.toJson());
        json.add("tools", t);

        if (ambientParticle != null) {
            JsonObject ae = new JsonObject();
            ae.addProperty("particle", ambientParticle);
            ae.addProperty("interval_ticks", ambientInterval);
            json.add("ambient_effects", ae);
        }

        json.addProperty("mating_duration", matingDuration);
        json.addProperty("breed_cooldown_ratio", breedCooldownRatio);

        return json;
    }

    public boolean isDefaultEntity() { return defaultEntity; }
    public String getConfigName() { return configName; }
    public String getType() { return type; }
    public String getDisplayName() { return displayName; }
    public String getRarity() { return rarity; }
    public int getTimeGrowth() { return timeGrowth; }
    public int getHarvestCooldown() { return harvestCooldown; }
    public int getHarvestExp() { return harvestExp; }
    public List<BreedingItem> getBreedingItems() { return breedingItems; }
    public LiteItem getEgg() { return egg; }
    public List<DropItem> getDropItems() { return dropItems; }
    public List<DropItem> getDeathDrops() { return deathDrops; }
    public List<PassiveDropItem> getPassiveDropItems() { return passiveDropItems; }
    public List<ToolConfig> getTools() { return tools; }
    public String getAmbientParticle() { return ambientParticle; }
    public int getAmbientInterval() { return ambientInterval; }
    public String getFileName() { return fileName; }
    public MoodConfig getMoodConfig() { return moodConfig; }
    public int getMatingDuration() { return matingDuration; }
    public double getBreedCooldownRatio() { return breedCooldownRatio; }
    public HungerConfig getHungerConfig() { return hungerConfig; }

    public static class MoodConfig {
        private int petGain = 5;
        private int feedGain = 8;
        private int hurtLoss = 15;
        private int harvestLoss = 3;
        private int breedMinMood = 40;

        public static MoodConfig fromJson(JsonObject json) {
            MoodConfig mc = new MoodConfig();
            if (json.has("pet_gain")) mc.petGain = json.get("pet_gain").getAsInt();
            if (json.has("feed_gain")) mc.feedGain = json.get("feed_gain").getAsInt();
            if (json.has("hurt_loss")) mc.hurtLoss = json.get("hurt_loss").getAsInt();
            if (json.has("harvest_loss")) mc.harvestLoss = json.get("harvest_loss").getAsInt();
            if (json.has("breed_min_mood")) mc.breedMinMood = json.get("breed_min_mood").getAsInt();
            return mc;
        }

        public int getPetGain() { return petGain; }
        public int getFeedGain() { return feedGain; }
        public int getHurtLoss() { return hurtLoss; }
        public int getHarvestLoss() { return harvestLoss; }
        public int getBreedMinMood() { return breedMinMood; }
    }

    public static class HungerConfig {
        private int maxHunger = 200;
        private int saturationDuration = 1800;
        private int hungerInterval = 1728;
        private int starveInterval = 30;
        private double starveDamage = 1.0;
        private double autoHeatChance = 5.0;
        private List<FeedItem> feedItems = new ArrayList<>();

        public static HungerConfig fromJson(JsonObject json) {
            HungerConfig hc = new HungerConfig();
            if (json.has("max_hunger")) hc.maxHunger = json.get("max_hunger").getAsInt();
            if (json.has("saturation_duration")) hc.saturationDuration = json.get("saturation_duration").getAsInt();
            if (json.has("hunger_interval")) hc.hungerInterval = json.get("hunger_interval").getAsInt();
            if (json.has("starve_interval")) hc.starveInterval = json.get("starve_interval").getAsInt();
            if (json.has("starve_damage")) hc.starveDamage = json.get("starve_damage").getAsDouble();
            if (json.has("auto_heat_chance")) hc.autoHeatChance = json.get("auto_heat_chance").getAsDouble();
            if (json.has("feed_items")) {
                for (JsonElement el : json.getAsJsonArray("feed_items")) {
                    JsonObject fo = el.getAsJsonObject();
                    LiteItem item = fo.has("item") ? LiteItem.fromJson(fo.getAsJsonObject("item")) : null;
                    int value = fo.has("value") ? fo.get("value").getAsInt() : 20;
                    if (item != null) hc.feedItems.add(new FeedItem(item, value));
                }
            }
            return hc;
        }

        public int getMaxHunger() { return maxHunger; }
        public int getSaturationDuration() { return saturationDuration; }
        public int getHungerInterval() { return hungerInterval; }
        public int getStarveInterval() { return starveInterval; }
        public double getStarveDamage() { return starveDamage; }
        public double getAutoHeatChance() { return autoHeatChance; }
        public List<FeedItem> getFeedItems() { return feedItems; }
    }

    public static class FeedItem {
        private final LiteItem item;
        private final int value;

        public FeedItem(LiteItem item, int value) {
            this.item = item;
            this.value = value;
        }

        public LiteItem getItem() { return item; }
        public int getValue() { return value; }
    }
}
