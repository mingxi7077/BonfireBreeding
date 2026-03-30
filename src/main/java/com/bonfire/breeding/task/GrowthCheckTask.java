package com.bonfire.breeding.task;

import com.bonfire.breeding.BonfireBreeding;
import com.bonfire.breeding.manager.AnimalManager;
import com.bonfire.breeding.manager.LevelManager;
import com.bonfire.breeding.model.AnimalConfig;
import com.bonfire.breeding.model.AnimalData;
import com.bonfire.breeding.model.LiteItem;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class GrowthCheckTask extends BukkitRunnable {
    private final BonfireBreeding plugin;
    private final AnimalManager animalManager;
    private final LevelManager levelManager;

    public GrowthCheckTask(BonfireBreeding plugin, AnimalManager animalManager, LevelManager levelManager) {
        this.plugin = plugin;
        this.animalManager = animalManager;
        this.levelManager = levelManager;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis() / 1000;

        for (Map.Entry<UUID, AnimalData> entry : animalManager.getAllTracked().entrySet()) {
            UUID uuid = entry.getKey();
            AnimalData data = entry.getValue();

            Entity entity = Bukkit.getEntity(uuid);
            if (data.hasOwner()) {
                if (entity == null || !entity.isValid() || entity.isDead()) {
                    data.markInactive(now);
                    continue;
                }
                if (data.isInactive() || data.isEntityMissing()) {
                    animalManager.attachEntity(entity, data);
                }
            }

            if (data.isInactive()) {
                continue;
            }

            if (data.isCourtingExpired()) {
                data.clearCourting();
            }

            if (data.isCourting() && entity != null && entity.isValid()) {
                entity.getWorld().spawnParticle(
                        Particle.NOTE,
                        entity.getLocation().add(0, entity.getHeight() * 0.9, 0),
                        3,
                        0.3,
                        0.2,
                        0.3,
                        0.5
                );
            }

            if (entity instanceof Ageable ageable && data.isGrown() && !ageable.isAdult()) {
                ageable.setAdult();
            }

            if (!data.isPregnancyDone()) {
                continue;
            }

            if (entity == null || entity.isDead()) {
                continue;
            }

            AnimalConfig config = animalManager.getConfigFor(data);
            if (config == null) {
                data.clearPregnancy();
                continue;
            }

            data.clearPregnancy();

            try {
                EntityType babyType = EntityType.valueOf(config.getType().toUpperCase(Locale.ROOT));
                Entity baby = entity.getWorld().spawnEntity(entity.getLocation(), babyType);
                if (baby instanceof Ageable babyAgeable) {
                    babyAgeable.setBaby();
                }

                AnimalData babyData = animalManager.getOrTrack(baby);
                if (babyData != null) {
                    if (config.getTimeGrowth() > 0) {
                        babyData.startGrowth(config.getTimeGrowth());
                    }
                    if (data.getOwnerUuid() != null) {
                        animalManager.claimAnimal(baby, babyData, data.getOwnerUuid(), data.getOwnerName());
                    } else {
                        animalManager.attachEntity(baby, babyData);
                    }
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unable to spawn baby entity for type: " + config.getType());
            }

            entity.getWorld().spawnParticle(
                    Particle.HEART,
                    entity.getLocation().add(0, entity.getHeight(), 0),
                    8,
                    0.5,
                    0.3,
                    0.5,
                    0.02
            );
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.8f);

            if (data.getOwnerUuid() != null) {
                Player owner = Bukkit.getPlayer(data.getOwnerUuid());
                if (owner != null && owner.isOnline()) {
                    String prefix = plugin.getConfigManager().getPluginPrefix()
                            .replace("%msg%", "§a你的 " + config.getDisplayName() + " 分娩成功，诞下了一只幼崽！");
                    owner.sendMessage(LiteItem.colorText(prefix));
                    levelManager.addExp(owner, 10);
                }
            }
        }
    }
}
