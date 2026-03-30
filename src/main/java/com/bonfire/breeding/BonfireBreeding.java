package com.bonfire.breeding;

import com.bonfire.breeding.command.BreedingCommand;
import com.bonfire.breeding.config.ConfigManager;
import com.bonfire.breeding.gui.GuiManager;
import com.bonfire.breeding.listener.AnimalInteractListener;
import com.bonfire.breeding.listener.AnimalLifecycleListener;
import com.bonfire.breeding.listener.AnimalPackListener;
import com.bonfire.breeding.manager.AnimalManager;
import com.bonfire.breeding.manager.AnimalStatusFormatter;
import com.bonfire.breeding.manager.ExternalEntityFilter;
import com.bonfire.breeding.manager.ItemsAdderHook;
import com.bonfire.breeding.manager.LevelManager;
import com.bonfire.breeding.model.AnimalConfig;
import com.bonfire.breeding.model.LiteItem;
import com.bonfire.breeding.task.AmbientEffectTask;
import com.bonfire.breeding.task.AutoSaveTask;
import com.bonfire.breeding.task.GrowthCheckTask;
import com.bonfire.breeding.task.HungerTask;
import com.bonfire.breeding.task.LookAtTask;
import com.bonfire.breeding.task.PassiveDropTask;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class BonfireBreeding extends JavaPlugin {
    private ConfigManager configManager;
    private AnimalManager animalManager;
    private LevelManager levelManager;
    private GuiManager guiManager;
    private LookAtTask lookAtTask;
    private HungerTask hungerTask;
    private ExternalEntityFilter externalEntityFilter;
    private AnimalStatusFormatter animalStatusFormatter;

    @Override
    public void onEnable() {
        ItemsAdderHook.init(getLogger());

        configManager = new ConfigManager(this);
        configManager.loadAll();

        externalEntityFilter = new ExternalEntityFilter(this, configManager);
        animalStatusFormatter = new AnimalStatusFormatter(configManager);

        animalManager = new AnimalManager(this, configManager, externalEntityFilter);
        animalManager.loadData();

        levelManager = new LevelManager(this);
        levelManager.load(configManager.getLevelsConfig());

        guiManager = new GuiManager(this, configManager, levelManager);
        guiManager.setGuiConfig(configManager.getGuiConfig());

        getServer().getPluginManager().registerEvents(
                new AnimalInteractListener(this, animalManager, configManager, levelManager, animalStatusFormatter),
                this
        );
        getServer().getPluginManager().registerEvents(
                new AnimalLifecycleListener(this, animalManager, levelManager),
                this
        );
        getServer().getPluginManager().registerEvents(
                new AnimalPackListener(this, configManager, animalManager),
                this
        );
        getServer().getPluginManager().registerEvents(guiManager, this);

        new PassiveDropTask(this, animalManager).runTaskTimer(this, 100L, 200L);
        new GrowthCheckTask(this, animalManager, levelManager).runTaskTimer(this, 40L, 40L);
        new AutoSaveTask(this, animalManager, levelManager).runTaskTimer(this, 6000L, 6000L);
        new AmbientEffectTask(this, animalManager).runTaskTimer(this, 60L, 100L);

        hungerTask = new HungerTask(this, animalManager);
        hungerTask.runTaskTimer(this, 200L, 200L);

        lookAtTask = new LookAtTask(this, animalManager, animalStatusFormatter);
        lookAtTask.runTaskTimer(this, 5L, 5L);

        PluginCommand command = getCommand("bbreeding");
        if (command != null) {
            BreedingCommand handler = new BreedingCommand(
                    this,
                    configManager,
                    animalManager,
                    guiManager,
                    levelManager,
                    lookAtTask
            );
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }

        getServer().getScheduler().runTask(this, animalManager::bootstrapLoadedChunks);

        getLogger().info("BonfireBreeding v" + getDescription().getVersion() + " enabled.");
        getLogger().info("Loaded " + configManager.getAnimalConfigs().size() + " animal configs.");
    }

    @Override
    public void onDisable() {
        if (animalManager != null) {
            animalManager.saveData();
        }
        if (levelManager != null) {
            levelManager.saveExp();
        }
        getLogger().info("BonfireBreeding disabled. Data saved.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public AnimalManager getAnimalManager() {
        return animalManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public LookAtTask getLookAtTask() {
        return lookAtTask;
    }

    public HungerTask getHungerTask() {
        return hungerTask;
    }

    public ItemStack createAnimalPackItem(AnimalConfig config, String displayName) {
        Material packMaterial = Material.PAPER;
        if (config.getEgg() != null) {
            try {
                packMaterial = Material.valueOf(config.getEgg().getMaterial().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        ItemStack item = new ItemStack(packMaterial, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LiteItem.colorText(displayName));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(LiteItem.colorText("§7右键使用后会召唤一只"));
            lore.add(LiteItem.colorText(config.getDisplayName() + " §7来到你身边"));
            lore.add(Component.empty());
            lore.add(LiteItem.colorText("§7使用后将自动绑定为你的牧场动物"));
            lore.add(LiteItem.colorText("§7类型: " + config.getType()));
            lore.add(LiteItem.colorText("§7成长时间: " + config.getTimeGrowth() + "s"));
            meta.lore(lore);

            NamespacedKey key = new NamespacedKey(this, BreedingCommand.PACK_KEY_NAME);
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, config.getConfigName());
            meta.setCustomModelData(39001);
            item.setItemMeta(meta);
        }

        return item;
    }
}
