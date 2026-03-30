package com.bonfire.breeding.task;

import com.bonfire.breeding.BonfireBreeding;
import com.bonfire.breeding.manager.AnimalManager;
import com.bonfire.breeding.manager.LevelManager;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class AutoSaveTask extends BukkitRunnable {
    private final BonfireBreeding plugin;
    private final AnimalManager animalManager;
    private final LevelManager levelManager;

    public AutoSaveTask(BonfireBreeding plugin, AnimalManager animalManager, LevelManager levelManager) {
        this.plugin = plugin;
        this.animalManager = animalManager;
        this.levelManager = levelManager;
    }

    @Override
    public void run() {
        animalManager.snapshotTrackedEntities();
        String serialized = animalManager.serializeData();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            animalManager.writeDataToFile(serialized);
            levelManager.saveExp();
        });
    }
}
