package com.bonfire.breeding.task;

import com.bonfire.breeding.BonfireBreeding;
import com.bonfire.breeding.manager.AnimalManager;
import com.bonfire.breeding.manager.LevelManager;
import com.bonfire.breeding.model.AnimalConfig;
import com.bonfire.breeding.model.AnimalData;
import com.bonfire.breeding.model.LiteItem;
import com.destroystokyo.paper.entity.Pathfinder;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Runs every second during mating: moves both entities toward each other,
 * spawns HEART/VILLAGER_HAPPY when close. On completion applies pregnancy and breed cooldown.
 */
public class MatingNavigateTask extends BukkitRunnable {
    private final BonfireBreeding plugin;
    private final AnimalManager animalManager;
    private final Entity entityA;
    private final Entity entityB;
    private final AnimalConfig config;
    private final int pregnancyTimeSeconds;

    private int ticksRun;

    public MatingNavigateTask(BonfireBreeding plugin, AnimalManager animalManager,
                              Entity entityA, Entity entityB, AnimalConfig config, int pregnancyTimeSeconds) {
        this.plugin = plugin;
        this.animalManager = animalManager;
        this.entityA = entityA;
        this.entityB = entityB;
        this.config = config;
        this.pregnancyTimeSeconds = pregnancyTimeSeconds;
        this.ticksRun = 0;
    }

    @Override
    public void run() {
        if (!entityA.isValid() || entityA.isDead() || !entityB.isValid() || entityB.isDead()) {
            AnimalData dataA = animalManager.getTracked(entityA.getUniqueId());
            AnimalData dataB = animalManager.getTracked(entityB.getUniqueId());
            if (dataA != null) dataA.clearMating();
            if (dataB != null) dataB.clearMating();
            cancel();
            return;
        }

        AnimalData dataA = animalManager.getTracked(entityA.getUniqueId());
        AnimalData dataB = animalManager.getTracked(entityB.getUniqueId());
        if (dataA == null || dataB == null || !dataA.isMating() || !dataB.isMating()) {
            if (dataA != null) dataA.clearMating();
            if (dataB != null) dataB.clearMating();
            cancel();
            return;
        }

        int matingDurationTicks = config.getMatingDuration() * 20;
        if (ticksRun >= matingDurationTicks) {
            finishMating(dataA, dataB);
            cancel();
            return;
        }

        double dist = entityA.getLocation().distance(entityB.getLocation());

        if (entityA instanceof Mob mobA && entityB instanceof LivingEntity livingB) {
            Pathfinder pathA = mobA.getPathfinder();
            pathA.moveTo(livingB, 0.8);
        }
        if (entityB instanceof Mob mobB && entityA instanceof LivingEntity livingA) {
            Pathfinder pathB = mobB.getPathfinder();
            pathB.moveTo(livingA, 0.8);
        }

        if (dist < 2.5) {
            entityA.getWorld().spawnParticle(Particle.HEART,
                    entityA.getLocation().add(0, entityA.getHeight() * 0.5, 0),
                    6, 0.4, 0.3, 0.4, 0.02);
            entityB.getWorld().spawnParticle(Particle.HEART,
                    entityB.getLocation().add(0, entityB.getHeight() * 0.5, 0),
                    6, 0.4, 0.3, 0.4, 0.02);
            entityA.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                    entityA.getLocation().add(0, entityA.getHeight() * 0.6, 0),
                    4, 0.3, 0.2, 0.3, 0.01);
        }

        ticksRun += 20;
    }

    private void finishMating(AnimalData dataA, AnimalData dataB) {
        dataA.clearMating();
        dataB.clearMating();

        AnimalData mother = dataA.isMale() ? dataB : dataA;
        AnimalData father = dataA.isMale() ? dataA : dataB;
        Entity motherEntity = mother.getUuid().equals(entityA.getUniqueId()) ? entityA : entityB;
        Entity fatherEntity = father.getUuid().equals(entityA.getUniqueId()) ? entityA : entityB;

        mother.startPregnancy(pregnancyTimeSeconds);
        int cooldownSeconds = (int) (pregnancyTimeSeconds * config.getBreedCooldownRatio());
        father.startBreedCooldown(cooldownSeconds);

        motherEntity.getWorld().spawnParticle(Particle.HEART,
                motherEntity.getLocation().add(0, motherEntity.getHeight(), 0),
                10, 0.5, 0.4, 0.5, 0.02);
        motherEntity.getWorld().playSound(motherEntity.getLocation(),
                Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.8f);

        LevelManager levelManager = plugin.getLevelManager();
        if (mother.getOwnerUuid() != null) {
            Player owner = plugin.getServer().getPlayer(mother.getOwnerUuid());
            if (owner != null && owner.isOnline()) {
                String prefix = plugin.getConfigManager().getPluginPrefix()
                        .replace("%msg%", "§a交配完成！" + config.getDisplayName() + " 进入孕期。");
                owner.sendMessage(LiteItem.colorText(prefix));
                levelManager.addExp(owner, 5);
            }
        }
    }
}
