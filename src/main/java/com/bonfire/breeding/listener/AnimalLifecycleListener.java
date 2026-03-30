package com.bonfire.breeding.listener;

import com.bonfire.breeding.BonfireBreeding;
import com.bonfire.breeding.manager.AnimalManager;
import com.bonfire.breeding.manager.LevelManager;
import com.bonfire.breeding.model.AnimalConfig;
import com.bonfire.breeding.model.AnimalData;
import com.bonfire.breeding.model.DropItem;
import com.bonfire.breeding.model.LiteItem;
import org.bukkit.Material;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityEnterLoveModeEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class AnimalLifecycleListener implements Listener {
    private final BonfireBreeding plugin;
    private final AnimalManager animalManager;
    private final LevelManager levelManager;

    public AnimalLifecycleListener(BonfireBreeding plugin, AnimalManager animalManager, LevelManager levelManager) {
        this.plugin = plugin;
        this.animalManager = animalManager;
        this.levelManager = levelManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (animalManager.handleExcludedEntity(entity, "death")) {
            return;
        }
        if (!animalManager.isTrackedType(entity)) {
            return;
        }

        AnimalData data = animalManager.getTracked(entity.getUniqueId());
        if (data == null) {
            return;
        }

        event.getDrops().clear();
        event.setDroppedExp(0);

        AnimalConfig config = animalManager.getConfigFor(data);
        if (config != null && config.getDeathDrops() != null && !config.getDeathDrops().isEmpty()) {
            boolean isBaby = !data.isGrown();
            boolean isStarved = data.isStarvedToDeath();
            Player killer = event.getEntity().getKiller();
            int playerLevel = killer != null ? levelManager.getPlayerLevel(killer) : 0;

            for (DropItem drop : config.getDeathDrops()) {
                if (drop.getLevelRequired() > playerLevel) {
                    if (killer != null) {
                        levelManager.notifyLockedDrop(killer, drop, playerLevel);
                    }
                    continue;
                }
                if (drop.isFemaleOnly() && data.isMale()) {
                    continue;
                }
                if (ThreadLocalRandom.current().nextDouble(100.0D) >= drop.getChance()) {
                    continue;
                }

                LiteItem item = drop.getItem();
                if (item == null) {
                    continue;
                }

                ItemStack stack = item.toItemStack();
                if (isBaby || isStarved) {
                    stack.setAmount(1);
                }
                entity.getWorld().dropItemNaturally(entity.getLocation(), stack);
                if (drop.getAnimation() != null) {
                    drop.getAnimation().play(entity, plugin);
                }
            }

            if (killer != null) {
                levelManager.addExp(killer, config.getHarvestExp());
            }
        }

        animalManager.removeTracked(entity.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (animalManager.handleExcludedEntity(entity, "damage")) {
            return;
        }
        AnimalData data = animalManager.getTracked(entity.getUniqueId());
        if (data == null) {
            return;
        }
        AnimalConfig config = animalManager.getConfigFor(data);
        if (config == null) {
            return;
        }

        data.addMood(-config.getMoodConfig().getHurtLoss());
        animalManager.syncMoodToPDC(entity, data);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityRemove(EntityRemoveEvent event) {
        Entity entity = event.getEntity();
        if (animalManager.handleExcludedEntity(entity, "remove")) {
            return;
        }
        if (!animalManager.isTrackedType(entity)) {
            return;
        }

        EntityRemoveEvent.Cause cause = event.getCause();
        if (cause != EntityRemoveEvent.Cause.DESPAWN && cause != EntityRemoveEvent.Cause.DISCARD) {
            return;
        }

        AnimalData data = animalManager.getTracked(entity.getUniqueId());
        if (data == null) {
            return;
        }

        if (data.hasOwner()) {
            animalManager.markEntityMissing(entity, data);
            return;
        }

        animalManager.removeTracked(entity.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (animalManager.handleExcludedEntity(entity, "chunk-load")) {
                continue;
            }
            if (!animalManager.isTrackedType(entity)) {
                continue;
            }
            AnimalData data = animalManager.getOrTrack(entity);
            if (data != null) {
                animalManager.attachEntity(entity, data);
            }
        }

        animalManager.restoreMissingClaimedAnimalsInChunk(event.getChunk());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreed(EntityBreedEvent event) {
        if (animalManager.handleExcludedEntity(event.getEntity(), "breed")) {
            return;
        }
        if (animalManager.isTrackedType(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEnterLoveMode(EntityEnterLoveModeEvent event) {
        if (animalManager.handleExcludedEntity(event.getEntity(), "love-mode")) {
            return;
        }
        if (animalManager.getTracked(event.getEntity().getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDrop(EntityDropItemEvent event) {
        if (!(event.getEntity() instanceof Chicken chicken)) {
            return;
        }
        if (animalManager.handleExcludedEntity(chicken, "item-drop")) {
            return;
        }
        if (event.getItemDrop().getItemStack().getType() != Material.EGG) {
            return;
        }
        if (animalManager.getTracked(chicken.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getHungerTask().resetAlertCount(event.getPlayer().getUniqueId());
    }
}
