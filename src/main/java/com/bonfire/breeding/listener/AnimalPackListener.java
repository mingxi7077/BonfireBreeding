package com.bonfire.breeding.listener;

import com.bonfire.breeding.BonfireBreeding;
import com.bonfire.breeding.command.BreedingCommand;
import com.bonfire.breeding.config.ConfigManager;
import com.bonfire.breeding.manager.AnimalManager;
import com.bonfire.breeding.model.AnimalConfig;
import com.bonfire.breeding.model.AnimalData;
import com.bonfire.breeding.model.LiteItem;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.Locale;

public class AnimalPackListener implements Listener {
    private final BonfireBreeding plugin;
    private final ConfigManager configManager;
    private final AnimalManager animalManager;

    public AnimalPackListener(BonfireBreeding plugin, ConfigManager configManager, AnimalManager animalManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.animalManager = animalManager;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = hand.getItemMeta();
        if (meta == null) {
            return;
        }

        NamespacedKey key = new NamespacedKey(plugin, BreedingCommand.PACK_KEY_NAME);
        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return;
        }

        String configName = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (configName == null) {
            return;
        }

        AnimalConfig config = configManager.getAnimalConfig(configName);
        if (config == null) {
            player.sendMessage(LiteItem.colorText("§c无效的生物包，该动物配置已不存在。"));
            return;
        }

        event.setCancelled(true);
        hand.setAmount(hand.getAmount() - 1);
        performSummonEffect(player, config);
    }

    private void performSummonEffect(Player player, AnimalConfig config) {
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 50, 0, false, false, false));
        world.playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);
        world.playSound(location, Sound.BLOCK_PORTAL_TRIGGER, 0.3f, 1.5f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            EntityType entityType;
            try {
                entityType = EntityType.valueOf(config.getType().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                player.sendMessage(LiteItem.colorText("§c生物类型无效: " + config.getType()));
                return;
            }

            Location spawnLocation = player.getLocation();
            Entity entity = world.spawnEntity(spawnLocation, entityType);

            if (entity instanceof Ageable ageable) {
                ageable.setBaby();
            }

            AnimalData data = animalManager.getOrTrack(entity);
            if (data != null) {
                if (config.getTimeGrowth() > 0) {
                    data.startGrowth(config.getTimeGrowth());
                }
                animalManager.claimAnimal(entity, data, player.getUniqueId(), player.getName());
            }

            world.spawnParticle(Particle.END_ROD, spawnLocation.clone().add(0, 0.5, 0), 30, 0.5, 0.8, 0.5, 0.05);
            world.spawnParticle(Particle.ENCHANT, spawnLocation.clone().add(0, 1, 0), 50, 0.8, 0.8, 0.8, 0.5);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                world.playSound(spawnLocation, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
                world.playSound(spawnLocation, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
                world.spawnParticle(Particle.HEART, spawnLocation.clone().add(0, 1.2, 0), 8, 0.4, 0.4, 0.4, 0);

                if (player.isOnline()) {
                    player.showTitle(Title.title(
                            LiteItem.colorText("{#FFDAB9}New Partner"),
                            LiteItem.colorText("§7一只 " + config.getDisplayName() + " §7来到了你的身边。"),
                            Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(500))
                    ));
                }
            }, 10L);
        }, 20L);
    }
}
