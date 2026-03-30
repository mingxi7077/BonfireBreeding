package com.bonfire.breeding.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class AnimalData {
    private String configName;
    private UUID uuid;
    private boolean male;
    private long startTime;
    private long endTime;
    private long endLove;
    private long endTool;
    private long endToolId;
    private long endHarvest;
    private UUID ownerUuid;
    private String ownerName;
    private Map<Long, Long> passiveDropTime;

    private int mood;
    private long pregnantEnd;
    private long lastPetTime;

    private long courtingEnd;
    private UUID matingPartner;
    private long matingEnd;
    private long breedCooldownEnd;

    private int hunger;
    private long saturationEnd;
    private long lastHungerTick;
    private long lastStarveTick;
    private String lastWorld;
    private double lastX;
    private double lastY;
    private double lastZ;
    private float lastYaw;
    private float lastPitch;
    private boolean entityMissing;
    private transient long inactiveSince;
    private transient boolean starvedToDeath;

    public AnimalData(String configName, UUID uuid, boolean male) {
        this.configName = configName;
        this.uuid = uuid;
        this.male = male;
        this.startTime = 0;
        this.endTime = 0;
        this.endLove = 0;
        this.endHarvest = 0;
        this.endTool = 0;
        this.endToolId = 0;
        this.passiveDropTime = new HashMap<>();
        this.mood = ThreadLocalRandom.current().nextInt(30, 71);
        this.pregnantEnd = 0;
        this.lastPetTime = 0;
        this.courtingEnd = 0;
        this.matingPartner = null;
        this.matingEnd = 0;
        this.breedCooldownEnd = 0;
        this.hunger = 100;
        this.saturationEnd = 0;
        this.lastHungerTick = 0;
        this.lastStarveTick = 0;
        this.lastWorld = null;
        this.lastX = 0D;
        this.lastY = 0D;
        this.lastZ = 0D;
        this.lastYaw = 0F;
        this.lastPitch = 0F;
        this.entityMissing = false;
        this.inactiveSince = 0;
    }

    public static AnimalData fromJson(JsonObject json) {
        String configName = json.has("config_name") ? json.get("config_name").getAsString() : "unknown";
        UUID uuid = json.has("uuid") ? UUID.fromString(json.get("uuid").getAsString()) : UUID.randomUUID();
        boolean male = !json.has("male") || json.get("male").getAsBoolean();

        AnimalData data = new AnimalData(configName, uuid, male);
        data.startTime = json.has("start_time") ? json.get("start_time").getAsLong() : 0;
        data.endTime = json.has("end_time") ? json.get("end_time").getAsLong() : 0;
        data.endLove = json.has("end_love") ? json.get("end_love").getAsLong() : 0;
        data.endTool = json.has("end_tool") ? json.get("end_tool").getAsLong() : 0;
        data.endToolId = json.has("end_tool_id") ? json.get("end_tool_id").getAsLong() : 0;
        data.endHarvest = json.has("end_harvest") ? json.get("end_harvest").getAsLong() : 0;

        if (json.has("owner_uuid") && !json.get("owner_uuid").getAsString().isEmpty()) {
            try {
                data.ownerUuid = UUID.fromString(json.get("owner_uuid").getAsString());
            } catch (IllegalArgumentException ignored) {
            }
        }
        data.ownerName = json.has("owner_name") ? json.get("owner_name").getAsString() : null;

        data.passiveDropTime = new HashMap<>();
        if (json.has("passive_drop_time")) {
            JsonObject pdt = json.getAsJsonObject("passive_drop_time");
            for (Map.Entry<String, JsonElement> entry : pdt.entrySet()) {
                data.passiveDropTime.put(Long.parseLong(entry.getKey()), entry.getValue().getAsLong());
            }
        }

        if (json.has("mood")) {
            data.setMood(json.get("mood").getAsInt());
        }
        data.pregnantEnd = json.has("pregnant_end") ? json.get("pregnant_end").getAsLong() : 0;
        data.lastPetTime = json.has("last_pet_time") ? json.get("last_pet_time").getAsLong() : 0;

        data.courtingEnd = json.has("courting_end") ? json.get("courting_end").getAsLong() : 0;
        if (json.has("mating_partner") && !json.get("mating_partner").getAsString().isEmpty()) {
            try {
                data.matingPartner = UUID.fromString(json.get("mating_partner").getAsString());
            } catch (IllegalArgumentException ignored) {
            }
        }
        data.matingEnd = json.has("mating_end") ? json.get("mating_end").getAsLong() : 0;
        data.breedCooldownEnd = json.has("breed_cooldown_end") ? json.get("breed_cooldown_end").getAsLong() : 0;

        data.hunger = json.has("hunger") ? json.get("hunger").getAsInt() : 100;
        data.saturationEnd = json.has("saturation_end") ? json.get("saturation_end").getAsLong() : 0;
        data.lastHungerTick = json.has("last_hunger_tick") ? json.get("last_hunger_tick").getAsLong() : 0;
        data.lastStarveTick = json.has("last_starve_tick") ? json.get("last_starve_tick").getAsLong() : 0;

        data.lastWorld = json.has("last_world") && !json.get("last_world").getAsString().isEmpty()
                ? json.get("last_world").getAsString()
                : null;
        data.lastX = json.has("last_x") ? json.get("last_x").getAsDouble() : 0D;
        data.lastY = json.has("last_y") ? json.get("last_y").getAsDouble() : 0D;
        data.lastZ = json.has("last_z") ? json.get("last_z").getAsDouble() : 0D;
        data.lastYaw = json.has("last_yaw") ? json.get("last_yaw").getAsFloat() : 0F;
        data.lastPitch = json.has("last_pitch") ? json.get("last_pitch").getAsFloat() : 0F;
        data.entityMissing = json.has("entity_missing") && json.get("entity_missing").getAsBoolean();

        return data;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("config_name", configName);
        json.addProperty("uuid", uuid.toString());
        json.addProperty("male", male);
        json.addProperty("start_time", startTime);
        json.addProperty("end_time", endTime);
        json.addProperty("end_love", endLove);
        json.addProperty("end_tool", endTool);
        json.addProperty("end_tool_id", endToolId);
        json.addProperty("end_harvest", endHarvest);
        json.addProperty("owner_uuid", ownerUuid != null ? ownerUuid.toString() : "");
        json.addProperty("owner_name", ownerName != null ? ownerName : "");

        JsonObject pdt = new JsonObject();
        for (Map.Entry<Long, Long> entry : passiveDropTime.entrySet()) {
            pdt.addProperty(String.valueOf(entry.getKey()), entry.getValue());
        }
        json.add("passive_drop_time", pdt);

        json.addProperty("mood", mood);
        json.addProperty("pregnant_end", pregnantEnd);
        json.addProperty("last_pet_time", lastPetTime);

        json.addProperty("courting_end", courtingEnd);
        json.addProperty("mating_partner", matingPartner != null ? matingPartner.toString() : "");
        json.addProperty("mating_end", matingEnd);
        json.addProperty("breed_cooldown_end", breedCooldownEnd);

        json.addProperty("hunger", hunger);
        json.addProperty("saturation_end", saturationEnd);
        json.addProperty("last_hunger_tick", lastHungerTick);
        json.addProperty("last_starve_tick", lastStarveTick);
        json.addProperty("last_world", lastWorld != null ? lastWorld : "");
        json.addProperty("last_x", lastX);
        json.addProperty("last_y", lastY);
        json.addProperty("last_z", lastZ);
        json.addProperty("last_yaw", lastYaw);
        json.addProperty("last_pitch", lastPitch);
        json.addProperty("entity_missing", entityMissing);

        return json;
    }

    public boolean hasOwner() {
        return ownerUuid != null;
    }

    public boolean isOwner(UUID playerUuid) {
        return ownerUuid != null && ownerUuid.equals(playerUuid);
    }

    public void setOwner(UUID playerUuid, String playerName) {
        this.ownerUuid = playerUuid;
        this.ownerName = playerName;
    }

    public void clearOwner() {
        this.ownerUuid = null;
        this.ownerName = null;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public int getMood() {
        return mood;
    }

    public void setMood(int mood) {
        this.mood = Math.max(0, Math.min(100, mood));
    }

    public void addMood(int delta) {
        setMood(this.mood + delta);
    }

    public String getMoodEmoji() {
        if (mood >= 80) return "{#FFD700}<3";
        if (mood >= 50) return "{#90EE90}<3";
        if (mood >= 20) return "{#FFA500}<3";
        return "{#FF4500}<3";
    }

    public boolean canPet() {
        return System.currentTimeMillis() / 1000 >= lastPetTime;
    }

    public void markPetted(int cooldownSeconds) {
        this.lastPetTime = System.currentTimeMillis() / 1000 + cooldownSeconds;
    }

    public boolean isPregnant() {
        return pregnantEnd > 0 && System.currentTimeMillis() / 1000 < pregnantEnd;
    }

    public boolean isPregnancyDone() {
        return pregnantEnd > 0 && System.currentTimeMillis() / 1000 >= pregnantEnd;
    }

    public void startPregnancy(int seconds) {
        this.pregnantEnd = System.currentTimeMillis() / 1000 + seconds;
    }

    public void clearPregnancy() {
        this.pregnantEnd = 0;
    }

    public long getRemainingPregnancySeconds() {
        if (pregnantEnd <= 0) return 0;
        return Math.max(0, pregnantEnd - System.currentTimeMillis() / 1000);
    }

    public boolean isCourting() {
        return courtingEnd > 0 && System.currentTimeMillis() / 1000 < courtingEnd;
    }

    public boolean isCourtingExpired() {
        return courtingEnd > 0 && System.currentTimeMillis() / 1000 >= courtingEnd;
    }

    public void startCourting(int seconds) {
        this.courtingEnd = System.currentTimeMillis() / 1000 + seconds;
    }

    public void clearCourting() {
        this.courtingEnd = 0;
    }

    public long getRemainingCourtingSeconds() {
        if (courtingEnd <= 0) return 0;
        return Math.max(0, courtingEnd - System.currentTimeMillis() / 1000);
    }

    public boolean isMating() {
        return matingEnd > 0 && System.currentTimeMillis() / 1000 < matingEnd;
    }

    public boolean isMatingDone() {
        return matingEnd > 0 && System.currentTimeMillis() / 1000 >= matingEnd;
    }

    public void startMating(UUID partnerUuid, int seconds) {
        this.matingPartner = partnerUuid;
        this.matingEnd = System.currentTimeMillis() / 1000 + seconds;
    }

    public void clearMating() {
        this.matingPartner = null;
        this.matingEnd = 0;
    }

    public UUID getMatingPartner() {
        return matingPartner;
    }

    public long getRemainingMatingSeconds() {
        if (matingEnd <= 0) return 0;
        return Math.max(0, matingEnd - System.currentTimeMillis() / 1000);
    }

    public boolean isBreedCooldown() {
        return breedCooldownEnd > 0 && System.currentTimeMillis() / 1000 < breedCooldownEnd;
    }

    public void startBreedCooldown(int seconds) {
        this.breedCooldownEnd = System.currentTimeMillis() / 1000 + seconds;
    }

    public void clearBreedCooldown() {
        this.breedCooldownEnd = 0;
    }

    public long getRemainingBreedCooldownSeconds() {
        if (breedCooldownEnd <= 0) return 0;
        return Math.max(0, breedCooldownEnd - System.currentTimeMillis() / 1000);
    }

    public boolean isGrown() {
        return endTime <= 0 || System.currentTimeMillis() / 1000 >= endTime;
    }

    public boolean isBreeding() {
        return endLove > 0 && System.currentTimeMillis() / 1000 < endLove;
    }

    public boolean isToolCooldown() {
        return endTool > 0 && System.currentTimeMillis() / 1000 < endTool;
    }

    public long getRemainingGrowthSeconds() {
        if (endTime <= 0) return 0;
        return Math.max(0, endTime - System.currentTimeMillis() / 1000);
    }

    public long getRemainingBreedingSeconds() {
        if (endLove <= 0) return 0;
        return Math.max(0, endLove - System.currentTimeMillis() / 1000);
    }

    public long getRemainingToolSeconds() {
        if (endTool <= 0) return 0;
        return Math.max(0, endTool - System.currentTimeMillis() / 1000);
    }

    public void startGrowth(int seconds) {
        long now = System.currentTimeMillis() / 1000;
        this.startTime = now;
        this.endTime = now + seconds;
    }

    public void startBreeding(int seconds) {
        this.endLove = System.currentTimeMillis() / 1000 + seconds;
    }

    public void startToolCooldown(long toolId, int seconds) {
        this.endTool = System.currentTimeMillis() / 1000 + seconds;
        this.endToolId = toolId;
    }

    public boolean canPassiveDrop(long dropId, int intervalSeconds) {
        long now = System.currentTimeMillis() / 1000;
        Long lastDrop = passiveDropTime.get(dropId);
        return lastDrop == null || now >= lastDrop;
    }

    public void markPassiveDrop(long dropId, int intervalSeconds) {
        passiveDropTime.put(dropId, System.currentTimeMillis() / 1000 + intervalSeconds);
    }

    public void initPassiveDropIfAbsent(long dropId, int intervalSeconds) {
        if (!passiveDropTime.containsKey(dropId)) {
            passiveDropTime.put(dropId, System.currentTimeMillis() / 1000 + intervalSeconds);
        }
    }

    public boolean isHarvestCooldown() {
        return endHarvest > 0 && System.currentTimeMillis() / 1000 < endHarvest;
    }

    public long getRemainingHarvestSeconds() {
        if (endHarvest <= 0) return 0;
        return Math.max(0, endHarvest - System.currentTimeMillis() / 1000);
    }

    public void startHarvestCooldown(int seconds) {
        if (seconds > 0) {
            this.endHarvest = System.currentTimeMillis() / 1000 + seconds;
        }
    }

    public String getConfigName() {
        return configName;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public boolean isMale() {
        return male;
    }

    public void setMale(boolean male) {
        this.male = male;
    }

    public long getEndToolId() {
        return endToolId;
    }

    public Map<Long, Long> getPassiveDropTime() {
        return passiveDropTime;
    }

    public int getHunger() {
        return hunger;
    }

    public void setHunger(int hunger) {
        this.hunger = Math.max(0, hunger);
    }

    public void setHunger(int hunger, int maxHunger) {
        this.hunger = Math.max(0, Math.min(Math.max(0, maxHunger), hunger));
    }

    public void addHunger(int delta) {
        setHunger(this.hunger + delta);
    }

    public void addHunger(int delta, int maxHunger) {
        setHunger(this.hunger + delta, maxHunger);
    }

    public boolean isStarving() {
        return hunger <= 0;
    }

    public boolean isSaturated() {
        return saturationEnd > 0 && System.currentTimeMillis() / 1000 < saturationEnd;
    }

    public void startSaturation(int seconds) {
        this.saturationEnd = System.currentTimeMillis() / 1000 + seconds;
    }

    public long getLastHungerTick() {
        return lastHungerTick;
    }

    public void setLastHungerTick(long tick) {
        this.lastHungerTick = tick;
    }

    public long getLastStarveTick() {
        return lastStarveTick;
    }

    public void setLastStarveTick(long tick) {
        this.lastStarveTick = tick;
    }

    public String getHungerEmoji(int maxHunger) {
        int safeMax = Math.max(1, maxHunger);
        double ratio = hunger / (double) safeMax;
        if (ratio >= 0.8D) return "{#90EE90}F";
        if (ratio >= 0.5D) return "{#FFD700}F";
        if (ratio >= 0.2D) return "{#FFA500}F";
        return "{#FF4500}F";
    }

    public boolean isStarvedToDeath() {
        return starvedToDeath;
    }

    public void setStarvedToDeath(boolean starvedToDeath) {
        this.starvedToDeath = starvedToDeath;
    }

    public void snapshotLocation(Entity entity) {
        if (entity == null || entity.getWorld() == null) {
            return;
        }
        Location location = entity.getLocation();
        this.lastWorld = entity.getWorld().getName();
        this.lastX = location.getX();
        this.lastY = location.getY();
        this.lastZ = location.getZ();
        this.lastYaw = location.getYaw();
        this.lastPitch = location.getPitch();
    }

    public boolean hasSavedLocation() {
        return lastWorld != null && !lastWorld.isBlank();
    }

    public String getLastWorld() {
        return lastWorld;
    }

    public double getLastX() {
        return lastX;
    }

    public double getLastY() {
        return lastY;
    }

    public double getLastZ() {
        return lastZ;
    }

    public float getLastYaw() {
        return lastYaw;
    }

    public float getLastPitch() {
        return lastPitch;
    }

    public boolean isEntityMissing() {
        return entityMissing;
    }

    public void markEntityMissing(long nowSeconds) {
        this.entityMissing = true;
        markInactive(nowSeconds);
    }

    public void clearEntityMissing() {
        this.entityMissing = false;
    }

    public boolean isInactive() {
        return inactiveSince > 0;
    }

    public void markInactive(long nowSeconds) {
        if (inactiveSince <= 0) {
            this.inactiveSince = nowSeconds;
        }
    }

    public void clearInactive() {
        this.inactiveSince = 0;
    }

    public void resumeAfterInactive(long nowSeconds) {
        if (inactiveSince <= 0) {
            return;
        }

        long pausedSeconds = Math.max(0, nowSeconds - inactiveSince);
        if (pausedSeconds > 0) {
            endTime = shiftFutureTimestamp(endTime, inactiveSince, pausedSeconds);
            endLove = shiftFutureTimestamp(endLove, inactiveSince, pausedSeconds);
            endTool = shiftFutureTimestamp(endTool, inactiveSince, pausedSeconds);
            endHarvest = shiftFutureTimestamp(endHarvest, inactiveSince, pausedSeconds);
            lastPetTime = shiftFutureTimestamp(lastPetTime, inactiveSince, pausedSeconds);

            pregnantEnd = shiftFutureTimestamp(pregnantEnd, inactiveSince, pausedSeconds);
            courtingEnd = shiftFutureTimestamp(courtingEnd, inactiveSince, pausedSeconds);
            matingEnd = shiftFutureTimestamp(matingEnd, inactiveSince, pausedSeconds);
            breedCooldownEnd = shiftFutureTimestamp(breedCooldownEnd, inactiveSince, pausedSeconds);

            saturationEnd = shiftFutureTimestamp(saturationEnd, inactiveSince, pausedSeconds);

            for (Map.Entry<Long, Long> entry : passiveDropTime.entrySet()) {
                entry.setValue(shiftFutureTimestamp(entry.getValue(), inactiveSince, pausedSeconds));
            }
        }

        lastHungerTick = 0;
        lastStarveTick = 0;
        inactiveSince = 0;
    }

    private long shiftFutureTimestamp(long timestamp, long reference, long delta) {
        if (timestamp <= 0 || delta <= 0 || timestamp <= reference) {
            return timestamp;
        }
        return timestamp + delta;
    }
}
