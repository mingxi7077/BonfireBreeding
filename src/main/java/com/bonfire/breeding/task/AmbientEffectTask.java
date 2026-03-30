package com.bonfire.breeding.task;

import com.bonfire.breeding.BonfireBreeding;
import com.bonfire.breeding.manager.AnimalManager;
import com.bonfire.breeding.model.AnimalConfig;
import com.bonfire.breeding.model.AnimalData;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;

/**
 * Plays ambient particle effects around adult tracked animals.
 * Configured per-animal via "ambient_effects" in animal config.
 */
public class AmbientEffectTask extends BukkitRunnable {
    private final BonfireBreeding plugin;
    private final AnimalManager animalManager;

    public AmbientEffectTask(BonfireBreeding plugin, AnimalManager animalManager) {
        this.plugin = plugin;
        this.animalManager = animalManager;
    }

    @Override
    public void run() {
        for (Map.Entry<UUID, AnimalData> entry : animalManager.getAllTracked().entrySet()) {
            AnimalData data = entry.getValue();
            if (!data.isGrown()) continue;

            AnimalConfig config = animalManager.getConfigFor(data);
            if (config == null || config.getAmbientParticle() == null) continue;

            Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity == null || entity.isDead()) continue;
            if (!entity.getLocation().isChunkLoaded()) continue;

            playAmbient(entity, config.getAmbientParticle());
        }
    }

    private void playAmbient(Entity entity, String particleSpec) {
        // Format: "PARTICLE_NAME:count:dx:dy:dz"
        String[] parts = particleSpec.split(":");
        try {
            Particle p = Particle.valueOf(parts[0]);
            int count = parts.length >= 2 ? Integer.parseInt(parts[1]) : 3;
            double dx = parts.length >= 3 ? Double.parseDouble(parts[2]) : 0.5;
            double dy = parts.length >= 4 ? Double.parseDouble(parts[3]) : 0.5;
            double dz = parts.length >= 5 ? Double.parseDouble(parts[4]) : 0.5;

            Location loc = entity.getLocation().add(0, 0.8, 0);
            entity.getWorld().spawnParticle(p, loc, count, dx, dy, dz);
        } catch (IllegalArgumentException ignored) {}
    }
}
