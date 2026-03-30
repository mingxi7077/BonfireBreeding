package com.bonfire.breeding.manager;

import com.bonfire.breeding.BonfireBreeding;
import com.bonfire.breeding.config.ConfigManager;
import com.bonfire.breeding.model.AnimalConfig;
import com.bonfire.breeding.model.AnimalData;
import com.bonfire.breeding.model.PassiveDropItem;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class AnimalManager {
    private static final int ENTITY_DATA_SCHEMA = 3;

    private final BonfireBreeding plugin;
    private final ConfigManager configManager;
    private final ExternalEntityFilter externalEntityFilter;
    private final Map<UUID, AnimalData> trackedAnimals = new ConcurrentHashMap<>();

    private final NamespacedKey pdcMoodKey;
    private final NamespacedKey pdcMaleKey;
    private final NamespacedKey pdcHungerKey;
    private final NamespacedKey pdcConfigKey;
    private final NamespacedKey pdcOwnerUuidKey;
    private final NamespacedKey pdcOwnerNameKey;
    private final NamespacedKey pdcClaimedKey;

    public AnimalManager(BonfireBreeding plugin, ConfigManager configManager, ExternalEntityFilter externalEntityFilter) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.externalEntityFilter = externalEntityFilter;
        this.pdcMoodKey = new NamespacedKey(plugin, "bb_mood");
        this.pdcMaleKey = new NamespacedKey(plugin, "bb_male");
        this.pdcHungerKey = new NamespacedKey(plugin, "bb_hunger");
        this.pdcConfigKey = new NamespacedKey(plugin, "bb_config");
        this.pdcOwnerUuidKey = new NamespacedKey(plugin, "bb_owner_uuid");
        this.pdcOwnerNameKey = new NamespacedKey(plugin, "bb_owner_name");
        this.pdcClaimedKey = new NamespacedKey(plugin, "bb_claimed");
    }

    public void loadData() {
        externalEntityFilter.reload();
        trackedAnimals.clear();

        JsonObject root = configManager.loadEntityData();
        int schemaVersion = root.has("schema_version") ? root.get("schema_version").getAsInt() : 1;
        JsonObject map = root.has("map") ? root.getAsJsonObject("map") : root;
        long now = System.currentTimeMillis() / 1000;
        int migratedHungerCount = 0;

        for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
            try {
                AnimalData data = AnimalData.fromJson(entry.getValue().getAsJsonObject());
                AnimalConfig config = getConfigFor(data);
                int maxHunger = resolveMaxHunger(config);
                data.setHunger(data.getHunger(), maxHunger);

                if (schemaVersion < ENTITY_DATA_SCHEMA && data.hasOwner() && data.getHunger() <= 0) {
                    data.setHunger(Math.max(1, maxHunger / 2), maxHunger);
                    data.setLastHungerTick(0);
                    data.setLastStarveTick(0);
                    migratedHungerCount++;
                }

                if (data.hasOwner()) {
                    data.markInactive(now);
                }

                trackedAnimals.put(data.getUuid(), data);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load animal data: " + entry.getKey());
            }
        }

        plugin.getLogger().info("Loaded " + trackedAnimals.size() + " tracked animals.");
        if (migratedHungerCount > 0) {
            plugin.getLogger().info("Migrated hunger state for " + migratedHungerCount + " claimed animals.");
        }
    }

    public void saveData() {
        snapshotTrackedEntities();
        configManager.saveEntityData(buildEntityDataJson());
    }

    public AnimalData getOrTrack(Entity entity) {
        ExternalEntityFilter.ExclusionType exclusionType = externalEntityFilter.detect(entity);
        if (exclusionType != ExternalEntityFilter.ExclusionType.NONE) {
            externalEntityFilter.logSkip(entity, exclusionType, "track");
            return null;
        }
        if (!isTrackedType(entity)) {
            return null;
        }

        UUID uuid = entity.getUniqueId();
        AnimalData data = trackedAnimals.get(uuid);
        if (data != null) {
            attachEntity(entity, data);
            return data;
        }

        data = recoverFromPdc(entity);
        if (data != null) {
            trackedAnimals.put(uuid, data);
            attachEntity(entity, data);
            return data;
        }

        AnimalConfig config = configManager.findDefaultConfig(entity.getType().name());
        if (config == null) {
            return null;
        }

        boolean male = ThreadLocalRandom.current().nextBoolean();
        data = new AnimalData(config.getConfigName(), uuid, male);
        data.setHunger(randomInitialHunger(resolveMaxHunger(config)), resolveMaxHunger(config));

        for (PassiveDropItem passiveDropItem : config.getPassiveDropItems()) {
            data.initPassiveDropIfAbsent(passiveDropItem.getId(), passiveDropItem.getTime());
        }

        trackedAnimals.put(uuid, data);
        attachEntity(entity, data);
        return data;
    }

    public AnimalData getTracked(UUID uuid) {
        return trackedAnimals.get(uuid);
    }

    public void removeTracked(UUID uuid) {
        trackedAnimals.remove(uuid);
    }

    public Map<UUID, AnimalData> getAllTracked() {
        return Collections.unmodifiableMap(trackedAnimals);
    }

    public AnimalConfig getConfigFor(AnimalData data) {
        return configManager.getAnimalConfig(data.getConfigName());
    }

    public boolean isTrackedType(Entity entity) {
        if (!(entity instanceof Animals) && !(entity instanceof Bee)) {
            return false;
        }
        if (externalEntityFilter.detect(entity) != ExternalEntityFilter.ExclusionType.NONE) {
            return false;
        }
        return configManager.findDefaultConfig(entity.getType().name()) != null;
    }

    public boolean handleExcludedEntity(Entity entity, String context) {
        if (entity == null) {
            return false;
        }
        if (!(entity instanceof Animals) && !(entity instanceof Bee) && !trackedAnimals.containsKey(entity.getUniqueId())) {
            return false;
        }

        ExternalEntityFilter.ExclusionType exclusionType = externalEntityFilter.detect(entity);
        if (exclusionType == ExternalEntityFilter.ExclusionType.NONE) {
            return false;
        }

        if (trackedAnimals.remove(entity.getUniqueId()) != null) {
            externalEntityFilter.logCleanup(entity, exclusionType, context);
        } else {
            externalEntityFilter.logSkip(entity, exclusionType, context);
        }
        return true;
    }

    public String serializeData() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
                .toJson(buildEntityDataJson());
    }

    public void writeDataToFile(String jsonString) {
        configManager.writeEntityDataRaw(jsonString);
    }

    public void snapshotTrackedEntities() {
        long now = System.currentTimeMillis() / 1000;
        for (AnimalData data : new ArrayList<>(trackedAnimals.values())) {
            Entity entity = plugin.getServer().getEntity(data.getUuid());
            if (entity != null && entity.isValid() && !entity.isDead()) {
                if (handleExcludedEntity(entity, "snapshot")) {
                    continue;
                }
                attachEntity(entity, data);
            } else if (data.hasOwner()) {
                data.markInactive(now);
            }
        }
    }

    public void syncToPDC(Entity entity, AnimalData data) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        PersistentDataContainer pdc = living.getPersistentDataContainer();
        pdc.set(pdcMoodKey, PersistentDataType.INTEGER, data.getMood());
        pdc.set(pdcMaleKey, PersistentDataType.BYTE, (byte) (data.isMale() ? 1 : 0));
        pdc.set(pdcHungerKey, PersistentDataType.INTEGER, data.getHunger());
        pdc.set(pdcConfigKey, PersistentDataType.STRING, data.getConfigName());

        if (data.hasOwner()) {
            pdc.set(pdcOwnerUuidKey, PersistentDataType.STRING, data.getOwnerUuid().toString());
            pdc.set(pdcOwnerNameKey, PersistentDataType.STRING, data.getOwnerName() != null ? data.getOwnerName() : "");
            pdc.set(pdcClaimedKey, PersistentDataType.BYTE, (byte) 1);
        } else {
            pdc.remove(pdcOwnerUuidKey);
            pdc.remove(pdcOwnerNameKey);
            pdc.remove(pdcClaimedKey);
        }
    }

    public void syncMoodToPDC(Entity entity, AnimalData data) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        living.getPersistentDataContainer().set(pdcMoodKey, PersistentDataType.INTEGER, data.getMood());
    }

    public void claimAnimal(Entity entity, AnimalData data, UUID ownerUuid, String ownerName) {
        if (entity != null) {
            updateTrackedUuid(data, entity.getUniqueId());
        }
        data.setOwner(ownerUuid, ownerName);
        data.clearEntityMissing();
        data.clearInactive();

        if (entity != null) {
            attachEntity(entity, data);
        }
    }

    public void unclaimAnimal(Entity entity, AnimalData data) {
        data.clearOwner();
        data.clearEntityMissing();
        data.clearInactive();
        if (entity != null) {
            data.snapshotLocation(entity);
            applyClaimPersistence(entity, false);
            syncToPDC(entity, data);
        }
    }

    public void markEntityMissing(Entity entity, AnimalData data) {
        if (data == null) {
            return;
        }
        if (entity != null) {
            data.snapshotLocation(entity);
        }
        data.markEntityMissing(System.currentTimeMillis() / 1000);
    }

    public void attachEntity(Entity entity, AnimalData data) {
        if (entity == null || data == null) {
            return;
        }

        updateTrackedUuid(data, entity.getUniqueId());
        data.resumeAfterInactive(System.currentTimeMillis() / 1000);
        data.clearEntityMissing();
        data.snapshotLocation(entity);

        applyClaimPersistence(entity, data.hasOwner());
        syncToPDC(entity, data);
        syncEntityVisual(entity, data);
    }

    public void bootstrapLoadedChunks() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (Entity entity : chunk.getEntities()) {
                    if (handleExcludedEntity(entity, "bootstrap")) {
                        continue;
                    }
                    if (!isTrackedType(entity)) {
                        continue;
                    }
                    AnimalData data = getOrTrack(entity);
                    if (data != null) {
                        attachEntity(entity, data);
                    }
                }
            }
        }

        for (World world : plugin.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                restoreMissingClaimedAnimalsInChunk(chunk);
            }
        }
    }

    public void restoreMissingClaimedAnimalsInChunk(Chunk chunk) {
        if (chunk == null || !chunk.isLoaded()) {
            return;
        }

        List<AnimalData> candidates = new ArrayList<>();
        for (AnimalData data : new ArrayList<>(trackedAnimals.values())) {
            if (shouldRestoreInChunk(data, chunk)) {
                candidates.add(data);
            }
        }

        for (AnimalData data : candidates) {
            Entity existing = plugin.getServer().getEntity(data.getUuid());
            if (existing != null && existing.isValid() && !existing.isDead()) {
                attachEntity(existing, data);
                continue;
            }

            Entity nearbyMatch = findExistingEntityInChunk(chunk, data);
            if (nearbyMatch != null) {
                attachEntity(nearbyMatch, data);
                continue;
            }

            AnimalConfig config = getConfigFor(data);
            if (config == null) {
                continue;
            }

            EntityType entityType;
            try {
                entityType = EntityType.valueOf(config.getType().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Failed to restore claimed animal with invalid type: " + config.getType());
                continue;
            }

            World world = chunk.getWorld();
            Location location = new Location(world, data.getLastX(), data.getLastY(), data.getLastZ(), data.getLastYaw(), data.getLastPitch());
            UUID oldUuid = data.getUuid();
            Entity restored = world.spawnEntity(location, entityType);
            attachEntity(restored, data);

            plugin.getLogger().info("Restored claimed animal " + oldUuid + " as " + restored.getUniqueId() + ".");
        }
    }

    public void syncEntityVisual(Entity entity, AnimalData data) {
        if (entity instanceof Ageable ageable) {
            if (data.isGrown()) {
                if (!ageable.isAdult()) {
                    ageable.setAdult();
                }
            } else if (ageable.isAdult()) {
                ageable.setBaby();
            }
        }

        if (entity instanceof Animals animals) {
            animals.setLoveModeTicks(0);
        }
    }

    private JsonObject buildEntityDataJson() {
        JsonObject root = new JsonObject();
        root.addProperty("schema_version", ENTITY_DATA_SCHEMA);

        JsonObject map = new JsonObject();
        for (AnimalData data : trackedAnimals.values()) {
            map.add(data.getUuid().toString(), data.toJson());
        }
        root.add("map", map);
        return root;
    }

    private AnimalData recoverFromPdc(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return null;
        }

        PersistentDataContainer pdc = living.getPersistentDataContainer();
        if (!pdc.has(pdcConfigKey, PersistentDataType.STRING)) {
            return null;
        }

        String configName = pdc.get(pdcConfigKey, PersistentDataType.STRING);
        AnimalConfig config = configManager.getAnimalConfig(configName);
        if (config == null) {
            config = configManager.findDefaultConfig(entity.getType().name());
        }
        if (config == null) {
            return null;
        }

        boolean male = pdc.has(pdcMaleKey, PersistentDataType.BYTE)
                && pdc.get(pdcMaleKey, PersistentDataType.BYTE) == 1;
        AnimalData data = new AnimalData(config.getConfigName(), entity.getUniqueId(), male);

        if (pdc.has(pdcMoodKey, PersistentDataType.INTEGER)) {
            data.setMood(pdc.get(pdcMoodKey, PersistentDataType.INTEGER));
        }
        if (pdc.has(pdcHungerKey, PersistentDataType.INTEGER)) {
            data.setHunger(pdc.get(pdcHungerKey, PersistentDataType.INTEGER), resolveMaxHunger(config));
        }

        if (pdc.has(pdcClaimedKey, PersistentDataType.BYTE)
                && pdc.get(pdcClaimedKey, PersistentDataType.BYTE) == 1
                && pdc.has(pdcOwnerUuidKey, PersistentDataType.STRING)) {
            try {
                UUID ownerUuid = UUID.fromString(pdc.get(pdcOwnerUuidKey, PersistentDataType.STRING));
                String ownerName = pdc.has(pdcOwnerNameKey, PersistentDataType.STRING)
                        ? pdc.get(pdcOwnerNameKey, PersistentDataType.STRING)
                        : null;
                data.setOwner(ownerUuid, ownerName);
            } catch (IllegalArgumentException ignored) {
            }
        }

        for (PassiveDropItem passiveDropItem : config.getPassiveDropItems()) {
            data.initPassiveDropIfAbsent(passiveDropItem.getId(), passiveDropItem.getTime());
        }

        data.snapshotLocation(entity);
        return data;
    }

    private void applyClaimPersistence(Entity entity, boolean claimed) {
        entity.setPersistent(claimed);
        if (entity instanceof LivingEntity living) {
            living.setRemoveWhenFarAway(!claimed);
        }
    }

    private int resolveMaxHunger(AnimalConfig config) {
        if (config == null || config.getHungerConfig() == null) {
            return 200;
        }
        return Math.max(1, config.getHungerConfig().getMaxHunger());
    }

    private int randomInitialHunger(int maxHunger) {
        int safeMax = Math.max(1, maxHunger);
        int lower = Math.min(20, safeMax);
        int upper = Math.max(lower, Math.min(safeMax, 80));
        if (lower >= upper) {
            return upper;
        }
        return ThreadLocalRandom.current().nextInt(lower, upper + 1);
    }

    private void updateTrackedUuid(AnimalData data, UUID newUuid) {
        UUID oldUuid = data.getUuid();
        if (oldUuid.equals(newUuid) && trackedAnimals.get(newUuid) == data) {
            return;
        }

        trackedAnimals.remove(oldUuid);
        data.setUuid(newUuid);
        trackedAnimals.put(newUuid, data);
    }

    private boolean shouldRestoreInChunk(AnimalData data, Chunk chunk) {
        if (!data.hasOwner() || !data.isEntityMissing() || !data.hasSavedLocation()) {
            return false;
        }
        if (!chunk.getWorld().getName().equals(data.getLastWorld())) {
            return false;
        }
        return chunk.getX() == toChunk(data.getLastX()) && chunk.getZ() == toChunk(data.getLastZ());
    }

    private Entity findExistingEntityInChunk(Chunk chunk, AnimalData data) {
        AnimalConfig config = getConfigFor(data);
        if (config == null) {
            return null;
        }

        Location savedLocation = new Location(chunk.getWorld(), data.getLastX(), data.getLastY(), data.getLastZ());
        for (Entity entity : chunk.getEntities()) {
            if (handleExcludedEntity(entity, "chunk-match")) {
                continue;
            }
            if (!isTrackedType(entity)) {
                continue;
            }
            if (!entity.getType().name().equalsIgnoreCase(config.getType())) {
                continue;
            }
            if (trackedAnimals.containsKey(entity.getUniqueId()) && !entity.getUniqueId().equals(data.getUuid())) {
                continue;
            }
            if (entity.getLocation().distanceSquared(savedLocation) <= 4.0D) {
                return entity;
            }
        }
        return null;
    }

    private int toChunk(double coordinate) {
        return ((int) Math.floor(coordinate)) >> 4;
    }
}
