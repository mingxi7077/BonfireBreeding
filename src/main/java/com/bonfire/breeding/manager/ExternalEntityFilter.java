package com.bonfire.breeding.manager;

import com.bonfire.breeding.BonfireBreeding;
import com.bonfire.breeding.config.ConfigManager;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ExternalEntityFilter {
    public enum ExclusionType {
        NONE,
        MCPETS,
        MYTHIC_MOBS
    }

    private final BonfireBreeding plugin;
    private final ConfigManager configManager;
    private final Set<String> debugLogKeys = ConcurrentHashMap.newKeySet();

    private Method mythicIsMythicMobMethod;
    private Object mythicApiHelper;

    private Method mcPetsGetFromEntityMethod;

    public ExternalEntityFilter(BonfireBreeding plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        reload();
    }

    public void reload() {
        debugLogKeys.clear();
        hookMythicMobs();
        hookMCPets();
    }

    public ExclusionType detect(Entity entity) {
        if (entity == null) {
            return ExclusionType.NONE;
        }
        if (isMCPetsEntity(entity)) {
            return ExclusionType.MCPETS;
        }
        if (isMythicMob(entity)) {
            return ExclusionType.MYTHIC_MOBS;
        }
        return ExclusionType.NONE;
    }

    public void logSkip(Entity entity, ExclusionType type, String context) {
        debugLog(entity, type, context, "skip");
    }

    public void logCleanup(Entity entity, ExclusionType type, String context) {
        debugLog(entity, type, context, "cleanup");
    }

    private void hookMythicMobs() {
        mythicApiHelper = null;
        mythicIsMythicMobMethod = null;

        Plugin mythicPlugin = plugin.getServer().getPluginManager().getPlugin("MythicMobs");
        if (mythicPlugin == null || !mythicPlugin.isEnabled()) {
            return;
        }

        try {
            ClassLoader classLoader = mythicPlugin.getClass().getClassLoader();
            Class<?> mythicBukkitClass = classLoader.loadClass("io.lumine.mythic.bukkit.MythicBukkit");
            Object mythicInstance = mythicBukkitClass.getMethod("inst").invoke(null);
            if (mythicInstance == null) {
                mythicInstance = mythicPlugin;
            }
            mythicApiHelper = mythicBukkitClass.getMethod("getAPIHelper").invoke(mythicInstance);
            mythicIsMythicMobMethod = mythicApiHelper.getClass().getMethod("isMythicMob", Entity.class);
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to initialize MythicMobs entity filter hook.", e);
            mythicApiHelper = null;
            mythicIsMythicMobMethod = null;
        }
    }

    private void hookMCPets() {
        mcPetsGetFromEntityMethod = null;

        Plugin mcPetsPlugin = plugin.getServer().getPluginManager().getPlugin("MCPets");
        if (mcPetsPlugin == null || !mcPetsPlugin.isEnabled()) {
            return;
        }

        try {
            ClassLoader classLoader = mcPetsPlugin.getClass().getClassLoader();
            Class<?> petClass = classLoader.loadClass("fr.nocsy.mcpets.data.Pet");
            mcPetsGetFromEntityMethod = petClass.getMethod("getFromEntity", Entity.class);
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to initialize MCPets entity filter hook.", e);
            mcPetsGetFromEntityMethod = null;
        }
    }

    private boolean isMythicMob(Entity entity) {
        if (mythicApiHelper == null || mythicIsMythicMobMethod == null) {
            return false;
        }

        try {
            Object result = mythicIsMythicMobMethod.invoke(mythicApiHelper, entity);
            return result instanceof Boolean bool && bool;
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to query MythicMobs entity filter hook.", e);
            mythicApiHelper = null;
            mythicIsMythicMobMethod = null;
            return false;
        }
    }

    private boolean isMCPetsEntity(Entity entity) {
        if (mcPetsGetFromEntityMethod == null) {
            return false;
        }

        try {
            return mcPetsGetFromEntityMethod.invoke(null, entity) != null;
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to query MCPets entity filter hook.", e);
            mcPetsGetFromEntityMethod = null;
            return false;
        }
    }

    private void debugLog(Entity entity, ExclusionType type, String context, String action) {
        if (!configManager.isDebugExternalEntityFilter() || entity == null || type == ExclusionType.NONE) {
            return;
        }

        UUID uuid = entity.getUniqueId();
        String key = action + ":" + context + ":" + uuid;
        if (!debugLogKeys.add(key)) {
            return;
        }

        plugin.getLogger().info("[BonfireBreeding] External entity filter " + action
                + " [" + describe(type) + "] "
                + entity.getType() + " "
                + uuid
                + " @ "
                + context);
    }

    private String describe(ExclusionType type) {
        return switch (type) {
            case MCPETS -> "MCPets";
            case MYTHIC_MOBS -> "MythicMobs";
            default -> "None";
        };
    }
}
