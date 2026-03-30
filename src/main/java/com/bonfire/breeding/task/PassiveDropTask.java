package com.bonfire.breeding.task;

import com.bonfire.breeding.BonfireBreeding;
import com.bonfire.breeding.manager.AnimalManager;
import com.bonfire.breeding.model.AnimalConfig;
import com.bonfire.breeding.model.AnimalData;
import com.bonfire.breeding.model.LiteAnimation;
import com.bonfire.breeding.model.LiteItem;
import com.bonfire.breeding.model.PassiveDropItem;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PassiveDropTask extends BukkitRunnable {
    private final BonfireBreeding plugin;
    private final AnimalManager animalManager;

    public PassiveDropTask(BonfireBreeding plugin, AnimalManager animalManager) {
        this.plugin = plugin;
        this.animalManager = animalManager;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis() / 1000;

        for (Map.Entry<UUID, AnimalData> entry : animalManager.getAllTracked().entrySet()) {
            UUID uuid = entry.getKey();
            AnimalData data = entry.getValue();
            if (!data.isGrown() || data.isInactive()) {
                continue;
            }

            Entity entity = plugin.getServer().getEntity(uuid);
            if (data.hasOwner() && (entity == null || !entity.isValid() || entity.isDead())) {
                data.markInactive(now);
                continue;
            }

            AnimalConfig config = animalManager.getConfigFor(data);
            if (config == null || entity == null || entity.isDead()) {
                continue;
            }

            for (PassiveDropItem passiveDropItem : config.getPassiveDropItems()) {
                if (!data.canPassiveDrop(passiveDropItem.getId(), passiveDropItem.getTime())) {
                    continue;
                }

                data.markPassiveDrop(passiveDropItem.getId(), passiveDropItem.getTime());

                if (passiveDropItem.isFemaleOnly() && data.isMale()) {
                    continue;
                }

                if (ThreadLocalRandom.current().nextDouble(100.0D) >= passiveDropItem.getChance()) {
                    continue;
                }

                LiteItem item = passiveDropItem.getItem();
                if (item == null || !entity.getLocation().isChunkLoaded()) {
                    continue;
                }

                Location location = entity.getLocation().add(0, 0.3, 0);
                ItemStack stack = item.toItemStack();
                entity.getWorld().dropItemNaturally(location, stack);

                LiteAnimation animation = passiveDropItem.getAnimation();
                if (animation != null) {
                    animation.play(entity, plugin);
                }
            }
        }
    }
}
