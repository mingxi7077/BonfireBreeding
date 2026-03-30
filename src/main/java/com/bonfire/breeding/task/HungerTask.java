package com.bonfire.breeding.task;

import com.bonfire.breeding.BonfireBreeding;
import com.bonfire.breeding.manager.AnimalManager;
import com.bonfire.breeding.model.AnimalConfig;
import com.bonfire.breeding.model.AnimalData;
import com.bonfire.breeding.model.LiteItem;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class HungerTask extends BukkitRunnable {
    private final BonfireBreeding plugin;
    private final AnimalManager animalManager;
    private final Map<UUID, Integer> hungerAlertCount = new HashMap<>();

    public HungerTask(BonfireBreeding plugin, AnimalManager animalManager) {
        this.plugin = plugin;
        this.animalManager = animalManager;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis() / 1000;
        List<UUID> starveUuids = new ArrayList<>();

        for (Map.Entry<UUID, AnimalData> entry : animalManager.getAllTracked().entrySet()) {
            UUID uuid = entry.getKey();
            AnimalData data = entry.getValue();
            AnimalConfig config = animalManager.getConfigFor(data);
            if (config == null) {
                continue;
            }

            Entity trackedEntity = null;
            if (data.hasOwner()) {
                trackedEntity = Bukkit.getEntity(uuid);
                if (trackedEntity == null || !trackedEntity.isValid() || trackedEntity.isDead()) {
                    data.markInactive(now);
                    continue;
                }
                if (data.isInactive() || data.isEntityMissing()) {
                    animalManager.attachEntity(trackedEntity, data);
                }
            }

            if (data.isInactive() || !data.isGrown()) {
                continue;
            }

            AnimalConfig.HungerConfig hungerConfig = config.getHungerConfig();

            if (data.isSaturated()) {
                if (!data.isCourting() && !data.isMating() && !data.isPregnant() && !data.isBreedCooldown()) {
                    double chancePerRun = hungerConfig.getAutoHeatChance() / 180.0D;
                    if (ThreadLocalRandom.current().nextDouble(100.0D) < chancePerRun) {
                        for (var breedingItem : config.getBreedingItems()) {
                            if (breedingItem.isNeedPartner()) {
                                data.startCourting(breedingItem.getCourtshipDuration());
                                break;
                            }
                        }
                    }
                }
                continue;
            }

            if (data.getHunger() > 0) {
                if (data.getLastHungerTick() == 0) {
                    data.setLastHungerTick(now);
                }
                if (now - data.getLastHungerTick() >= hungerConfig.getHungerInterval()) {
                    data.setLastHungerTick(now);
                    data.addHunger(-1, hungerConfig.getMaxHunger());
                    if (data.getHunger() > 0) {
                        data.setLastStarveTick(0);
                    }
                    if (trackedEntity != null) {
                        animalManager.syncToPDC(trackedEntity, data);
                    }
                }
                continue;
            }

            if (data.getLastStarveTick() == 0) {
                data.setLastStarveTick(now);
            }
            if (now - data.getLastStarveTick() >= hungerConfig.getStarveInterval()) {
                data.setLastStarveTick(now);
                starveUuids.add(uuid);
            }
        }

        if (!starveUuids.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (UUID uuid : starveUuids) {
                    Entity entity = Bukkit.getEntity(uuid);
                    if (!(entity instanceof LivingEntity living) || entity.isDead()) {
                        continue;
                    }

                    AnimalData data = animalManager.getTracked(uuid);
                    if (data == null || data.isInactive()) {
                        continue;
                    }

                    AnimalConfig config = animalManager.getConfigFor(data);
                    if (config == null) {
                        continue;
                    }

                    double damage = config.getHungerConfig().getStarveDamage();
                    if (living.getHealth() <= damage) {
                        data.setStarvedToDeath(true);
                    }
                    living.damage(damage);
                    entity.getWorld().spawnParticle(
                            Particle.SMOKE,
                            entity.getLocation().add(0, entity.getHeight() * 0.5, 0),
                            5,
                            0.2,
                            0.2,
                            0.2,
                            0.01
                    );
                }
            });
        }

        checkHungerAlerts();
    }

    private void checkHungerAlerts() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            int alertsSent = hungerAlertCount.getOrDefault(playerId, 0);
            if (alertsSent >= 5) {
                continue;
            }

            int hungryCount = 0;
            for (AnimalData data : animalManager.getAllTracked().values()) {
                if (!data.isOwner(playerId) || data.isInactive()) {
                    continue;
                }
                if (data.isStarving()) {
                    hungryCount++;
                }
            }

            if (hungryCount > 0) {
                hungerAlertCount.put(playerId, alertsSent + 1);
                String prefix = plugin.getConfigManager().getPluginPrefix().replace(
                        "%msg%",
                        "§c⚠ 你有 §e" + hungryCount + " §c只牧场动物正在挨饿，请尽快喂食！"
                );
                player.sendMessage(LiteItem.colorText(prefix));
            } else {
                hungerAlertCount.remove(playerId);
            }
        }
    }

    public void resetAlertCount(UUID playerId) {
        hungerAlertCount.remove(playerId);
    }
}
