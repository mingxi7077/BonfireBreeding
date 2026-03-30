package com.bonfire.breeding.listener;

import com.bonfire.breeding.BonfireBreeding;
import com.bonfire.breeding.config.ConfigManager;
import com.bonfire.breeding.manager.AnimalManager;
import com.bonfire.breeding.manager.AnimalStatusFormatter;
import com.bonfire.breeding.manager.LevelManager;
import com.bonfire.breeding.model.*;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AnimalInteractListener implements Listener {
    private final BonfireBreeding plugin;
    private final AnimalManager animalManager;
    private final ConfigManager configManager;
    private final LevelManager levelManager;
    private final AnimalStatusFormatter statusFormatter;

    private static final int PET_COOLDOWN_SECONDS = 5;

    public AnimalInteractListener(BonfireBreeding plugin, AnimalManager animalManager,
                                  ConfigManager configManager, LevelManager levelManager,
                                  AnimalStatusFormatter statusFormatter) {
        this.plugin = plugin;
        this.animalManager = animalManager;
        this.configManager = configManager;
        this.levelManager = levelManager;
        this.statusFormatter = statusFormatter;
    }

    private static final String PERM_INTERACT = "bonfire.breeding.interact";
    private static final String PERM_HARVEST = "bonfire.breeding.harvest";
    private static final String PERM_TOOL = "bonfire.breeding.tool";
    private static final String PERM_BREED = "bonfire.breeding.breed";

    private String typeKey(AnimalConfig config) {
        return config.getType().toLowerCase();
    }

    private boolean hasPermission(Player player, String base, AnimalConfig config) {
        String type = typeKey(config);
        String specific = base + "." + type;
        if (player.isPermissionSet(specific)) {
            return player.hasPermission(specific);
        }
        if (player.isPermissionSet(base)) {
            return player.hasPermission(base);
        }
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Entity entity = event.getRightClicked();
        if (!(entity instanceof Animals) && !(entity instanceof Bee)) return;
        if (animalManager.handleExcludedEntity(entity, "interact")) return;

        AnimalData data = animalManager.getOrTrack(entity);
        if (data == null) return;

        AnimalConfig config = animalManager.getConfigFor(data);
        if (config == null) return;

        Player player = event.getPlayer();

        event.setCancelled(true);

        if (!hasPermission(player, PERM_INTERACT, config)) {
            sendPrefixMsg(player, "§c你没有与该动物互动的权限。");
            return;
        }

        if (data.hasOwner() && !data.isOwner(player.getUniqueId())) {
            String ownerName = data.getOwnerName() != null ? data.getOwnerName() : "???";
            player.showTitle(Title.title(
                    LiteItem.colorText("{#FF6B6B}属于 " + ownerName + " 的牧场动物"),
                    LiteItem.colorText("§7你无法与它互动"),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(400))
            ));
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        final int handSlot = player.getInventory().getHeldItemSlot();
        final ItemStack handSnapshot = hand.clone();
        boolean sneaking = player.isSneaking();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            ItemStack current = player.getInventory().getItem(handSlot);
            if (current != null && !current.isSimilar(handSnapshot) && handSnapshot.getType() != Material.AIR) {
                if (current.getType() == Material.MILK_BUCKET && handSnapshot.getType() == Material.BUCKET) {
                    player.getInventory().setItem(handSlot, handSnapshot);
                }
            }
        }, 1L);

        if (sneaking && (hand.getType() == Material.AIR || hand.getAmount() == 0)) {
            handleSneakEmptyHand(player, entity, data, config);
            return;
        }

        if (hand.getType() != Material.AIR) {
            if (tryToolInteract(player, entity, hand, data, config)) {
                return;
            }

            if (sneaking) {
                // 蹲下 + 食物 = 繁殖求偶
                if (tryBreeding(player, entity, hand, data, config)) {
                    return;
                }
            } else {
                // 不蹲下 + 食物 = 喂食增加饱腹
                if (tryFeed(player, entity, hand, data, config)) {
                    return;
                }
            }
        } else {
            if (!player.isSneaking() && data.isCourting()) {
                if (tryConfirmMating(player, entity, data, config)) {
                    return;
                }
            }
        }

        showAnimalInfo(player, data, config);
    }

    private void handleSneakEmptyHand(Player player, Entity entity, AnimalData data, AnimalConfig config) {
        AnimalConfig.MoodConfig moodCfg = config.getMoodConfig();

        boolean petted = false;
        if (data.canPet()) {
            int oldMood = data.getMood();
            data.addMood(moodCfg.getPetGain());
            int newMood = data.getMood();
            data.markPetted(PET_COOLDOWN_SECONDS);
            petted = true;
            animalManager.syncMoodToPDC(entity, data);

            player.playSound(entity.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);

            entity.getWorld().spawnParticle(Particle.HEART,
                    entity.getLocation().add(0, entity.getHeight() * 0.8, 0),
                    ThreadLocalRandom.current().nextInt(3, 6),
                    0.3, 0.2, 0.3, 0.02);

            player.showTitle(Title.title(
                    LiteItem.colorText("{#FFDAB9}你轻轻抚摸了 " + config.getDisplayName()),
                    LiteItem.colorText(statusFormatter.formatMoodChange(oldMood, newMood)),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofSeconds(2), Duration.ofMillis(300))
            ));
        }

        if (!data.isGrown()) {
            if (!petted) {
                sendPrefixMsg(player, "§c该动物尚未成年，无法收获。");
            }
            return;
        }

        if (data.isPregnant()) {
            long remaining = data.getRemainingPregnancySeconds();
            if (!petted) {
                player.playSound(entity.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);
            }
            sendPrefixMsg(player, "§c该生物正在孕期，不可以收获。§7(剩余 " + configManager.formatTime(remaining) + ")");
            return;
        }

        if (data.isHarvestCooldown()) {
            long remaining = data.getRemainingHarvestSeconds();
            if (!petted) {
                sendPrefixMsg(player, "§c收获冷却中，剩余 " + configManager.formatTime(remaining));
            }
            return;
        }

        if (!hasPermission(player, PERM_HARVEST, config)) {
            sendPrefixMsg(player, "§c你没有收获该动物的权限。");
            return;
        }

        // Shift + empty-hand interaction is treated as "pet-focused":
        // keep harvest available, but don't penalize mood on this path.
        handleHarvest(player, entity, data, config, false);
    }

    private void handleHarvest(Player player, Entity entity, AnimalData data, AnimalConfig config, boolean applyMoodLoss) {
        List<DropItem> dropItems = config.getDropItems();
        if (dropItems.isEmpty()) {
            showAnimalInfo(player, data, config);
            return;
        }

        autoBindIfNeeded(player, entity, data);

        int playerLevel = levelManager.getPlayerLevel(player);
        boolean dropped = false;

        for (DropItem drop : dropItems) {
            if (drop.getLevelRequired() > playerLevel) {
                levelManager.notifyLockedDrop(player, drop, playerLevel);
                continue;
            }
            double roll = ThreadLocalRandom.current().nextDouble(100);
            if (roll < drop.getChance()) {
                LiteItem item = drop.getItem();
                if (item != null) {
                    ItemStack stack = item.toItemStack();
                    entity.getWorld().dropItemNaturally(entity.getLocation(), stack);
                    dropped = true;
                    if (drop.getAnimation() != null) {
                        drop.getAnimation().play(entity, plugin);
                    }
                }
            }
        }

        if (dropped) {
            data.startHarvestCooldown(config.getHarvestCooldown());
            if (applyMoodLoss) {
                data.addMood(-config.getMoodConfig().getHarvestLoss());
                animalManager.syncMoodToPDC(entity, data);
            }
            levelManager.addExp(player, config.getHarvestExp());
        }
    }

    private boolean tryToolInteract(Player player, Entity entity, ItemStack hand,
                                     AnimalData data, AnimalConfig config) {
        if (!data.isGrown()) return false;

        if (!hasPermission(player, PERM_TOOL, config)) {
            sendPrefixMsg(player, "§c你没有对该动物使用工具的权限。");
            return true;
        }

        if (data.isPregnant()) {
            sendPrefixMsg(player, "§c该生物正在孕期，不可以收获。");
            return true;
        }

        for (ToolConfig tool : config.getTools()) {
            if (tool.getItem() == null) continue;
            if (!tool.getItem().matchesMaterial(hand)) continue;

            if (data.isToolCooldown() && data.getEndToolId() == tool.getId()) {
                long remaining = data.getRemainingToolSeconds();
                sendPrefixMsg(player, "§c工具冷却中，剩余 " + configManager.formatTime(remaining));
                return true;
            }

            int playerLevel = levelManager.getPlayerLevel(player);
            boolean dropped = false;

            for (DropItem drop : tool.getDrops()) {
                if (drop.getLevelRequired() > playerLevel) {
                    levelManager.notifyLockedDrop(player, drop, playerLevel);
                    continue;
                }
                double roll = ThreadLocalRandom.current().nextDouble(100);
                if (roll < drop.getChance()) {
                    LiteItem item = drop.getItem();
                    if (item != null) {
                        entity.getWorld().dropItemNaturally(entity.getLocation(), item.toItemStack());
                        dropped = true;
                        if (drop.getAnimation() != null) {
                            drop.getAnimation().play(entity, plugin);
                        }
                    }
                }
            }

            consumeTool(player, hand);

            int cooldownSeconds = configManager.getEffectiveToolTime(config, tool);
            data.startToolCooldown(tool.getId(), cooldownSeconds);

            if (tool.isBaby() && config.getTimeGrowth() > 0) {
                data.startGrowth(config.getTimeGrowth());
                if (entity instanceof Ageable ageable) {
                    ageable.setBaby();
                }
            }

            if (dropped) {
                data.addMood(-config.getMoodConfig().getHarvestLoss());
                animalManager.syncMoodToPDC(entity, data);
                levelManager.addExp(player, 3);
            }
            return true;
        }
        return false;
    }

    private boolean tryFeed(Player player, Entity entity, ItemStack hand,
                             AnimalData data, AnimalConfig config) {
        AnimalConfig.HungerConfig hungerCfg = config.getHungerConfig();
        for (AnimalConfig.FeedItem fi : hungerCfg.getFeedItems()) {
            if (fi.getItem() == null) continue;
            if (!fi.getItem().matchesMaterial(hand)) continue;

            autoBindIfNeeded(player, entity, data);

            if (data.getHunger() >= hungerCfg.getMaxHunger()) {
                sendPrefixMsg(player, "§e这只 " + config.getDisplayName() + " §e已经吃饱了！§7(蹲下右键可触发求偶)");
                return true;
            }

            hand.setAmount(hand.getAmount() - 1);
            int oldHunger = data.getHunger();
            data.addHunger(fi.getValue(), hungerCfg.getMaxHunger());
            data.setLastStarveTick(0);

            // 增加心情
            data.addMood(config.getMoodConfig().getFeedGain());
            animalManager.syncToPDC(entity, data);

            // 如果饱腹值达到满，触发饱腹期
            if (data.getHunger() >= hungerCfg.getMaxHunger()) {
                data.startSaturation(hungerCfg.getSaturationDuration());
                data.setLastHungerTick(0);
            }

            entity.getWorld().spawnParticle(Particle.HEART,
                    entity.getLocation().add(0, entity.getHeight() * 0.8, 0),
                    3, 0.2, 0.2, 0.2, 0.02);
            player.playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EAT, 0.8f, 1.0f);

            sendPrefixMsg(player, "§a喂食成功！§7饱腹度 " + oldHunger + " → " + data.getHunger() + "/" + hungerCfg.getMaxHunger()
                    + " §7好感度 +" + config.getMoodConfig().getFeedGain());
            return true;
        }
        return false;
    }

    private boolean tryBreeding(Player player, Entity entity, ItemStack hand,
                                 AnimalData data, AnimalConfig config) {
        if (!data.isGrown()) return false;

        if (!hasPermission(player, PERM_BREED, config)) {
            sendPrefixMsg(player, "§c你没有繁殖该动物的权限。");
            return true;
        }

        if (data.isMating()) {
            sendPrefixMsg(player, "§c该生物正在交配中，剩余 " + configManager.formatTime(data.getRemainingMatingSeconds()));
            return true;
        }
        if (data.isPregnant()) {
            sendPrefixMsg(player, "§c该生物正在孕期中，剩余 " + configManager.formatTime(data.getRemainingPregnancySeconds()));
            return true;
        }
        if (data.isBreedCooldown()) {
            sendPrefixMsg(player, "§c该生物繁殖冷却中，剩余 " + configManager.formatTime(data.getRemainingBreedCooldownSeconds()));
            return true;
        }
        if (data.isCourting()) {
            sendPrefixMsg(player, "§e该生物正在求偶中！§7空手右键它并确保附近有处于求偶状态的异性配偶以确认配对。");
            return true;
        }

        for (BreedingItem bi : config.getBreedingItems()) {
            if (bi.getItem() == null) continue;
            if (!bi.getItem().matchesMaterial(hand)) continue;

            autoBindIfNeeded(player, entity, data);

            AnimalConfig.MoodConfig moodCfg = config.getMoodConfig();
            if (data.getMood() < moodCfg.getBreedMinMood()) {
                sendPrefixMsg(player, "§c这只动物心情不好（好感度 " + data.getMood() + "/" + moodCfg.getBreedMinMood() + "），不愿意繁殖。");
                data.addMood(moodCfg.getFeedGain());
                animalManager.syncMoodToPDC(entity, data);
                sendPrefixMsg(player, "§7喂食提升了好感度 +" + moodCfg.getFeedGain());
                hand.setAmount(hand.getAmount() - 1);
                return true;
            }

            if (bi.isNeedPartner()) {
                hand.setAmount(hand.getAmount() - 1);
                data.startCourting(bi.getCourtshipDuration());

                entity.getWorld().spawnParticle(Particle.NOTE,
                        entity.getLocation().add(0, entity.getHeight() * 0.9, 0),
                        3, 0.3, 0.2, 0.3, 0.5);

                // 自动检测附近是否有求偶中的异性，有则直接配对
                AnimalData autoPartner = findCourtingPartner(entity, data, config);
                if (autoPartner != null) {
                    Entity partnerEntity = plugin.getServer().getEntity(autoPartner.getUuid());
                    if (partnerEntity != null && partnerEntity.isValid()) {
                        double roll = ThreadLocalRandom.current().nextDouble(100);
                        if (roll < bi.getChance()) {
                            int matingDuration = config.getMatingDuration();
                            data.startMating(autoPartner.getUuid(), matingDuration);
                            autoPartner.startMating(data.getUuid(), matingDuration);

                            sendPrefixMsg(player, "§d♥ 自动配对成功！§f" + config.getDisplayName() + " §7们开始交配...");

                            com.bonfire.breeding.task.MatingNavigateTask task = new com.bonfire.breeding.task.MatingNavigateTask(
                                    plugin, animalManager, entity, partnerEntity, config, bi.getTime());
                            task.runTaskTimer(plugin, 0L, 20L);

                            levelManager.addExp(player, 2);
                            return true;
                        } else {
                            data.clearCourting();
                            autoPartner.clearCourting();
                            sendPrefixMsg(player, "§c自动配对失败了...两只动物都退出了求偶状态。");
                            return true;
                        }
                    }
                }

                // 附近没有求偶异性，提示玩家手动操作
                player.showTitle(Title.title(
                        LiteItem.colorText("{#FFE4B5}" + config.getDisplayName() + " §e进入了求偶状态"),
                        LiteItem.colorText("§7请将异性伴侣也喂食进入求偶，系统将自动配对"),
                        Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(400))
                ));
                sendPrefixMsg(player, "§e求偶持续 " + configManager.formatTime(bi.getCourtshipDuration()) + "，请尽快喂食异性伴侣，喂食后将自动配对。");
                levelManager.addExp(player, 1);
            } else {
                double roll = ThreadLocalRandom.current().nextDouble(100);
                if (roll >= bi.getChance()) {
                    sendPrefixMsg(player, "§c繁殖尝试失败了...");
                    hand.setAmount(hand.getAmount() - 1);
                    return true;
                }

                hand.setAmount(hand.getAmount() - 1);
                data.startBreeding(bi.getTime());

                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (entity.isDead()) return;
                    if (config.getEgg() != null) {
                        entity.getWorld().dropItemNaturally(entity.getLocation(), config.getEgg().toItemStack());
                    }
                    sendPrefixMsg(player, "§a繁殖成功！一只新生命诞生了！");
                    levelManager.addExp(player, 10);
                }, bi.getTime() * 20L);

                sendPrefixMsg(player, "§a繁殖开始！预计 " + configManager.formatTime(bi.getTime()));
                levelManager.addExp(player, 1);
            }
            return true;
        }
        return false;
    }

    private boolean tryConfirmMating(Player player, Entity entity, AnimalData data, AnimalConfig config) {
        BreedingItem bi = null;
        for (BreedingItem b : config.getBreedingItems()) {
            if (b.isNeedPartner()) {
                bi = b;
                break;
            }
        }
        if (bi == null) return false;

        AnimalData partnerData = findCourtingPartner(entity, data, config);
        if (partnerData == null) {
            sendPrefixMsg(player, "§c附近没有处于求偶状态的异性配偶。");
            return true;
        }

        Entity partnerEntity = plugin.getServer().getEntity(partnerData.getUuid());
        if (partnerEntity == null || !partnerEntity.isValid()) {
            partnerData.clearCourting();
            sendPrefixMsg(player, "§c配偶已消失。");
            return true;
        }

        double roll = ThreadLocalRandom.current().nextDouble(100);
        if (roll >= bi.getChance()) {
            data.clearCourting();
            partnerData.clearCourting();
            sendPrefixMsg(player, "§c繁殖尝试失败了...");
            return true;
        }

        int matingDuration = config.getMatingDuration();
        data.startMating(partnerData.getUuid(), matingDuration);
        partnerData.startMating(data.getUuid(), matingDuration);

        String nameA = config.getDisplayName();
        String nameB = config.getDisplayName();
        sendPrefixMsg(player, "§d♥ 配对成功！§f" + nameA + " §7和 §f" + nameB + " §7开始交配...");

        com.bonfire.breeding.task.MatingNavigateTask task = new com.bonfire.breeding.task.MatingNavigateTask(
                plugin, animalManager, entity, partnerEntity, config, bi.getTime());
        task.runTaskTimer(plugin, 0L, 20L);

        levelManager.addExp(player, 1);
        return true;
    }

    private AnimalData findCourtingPartner(Entity entity, AnimalData data, AnimalConfig config) {
        for (Entity nearby : entity.getNearbyEntities(8, 4, 8)) {
            if (nearby.getType() != entity.getType()) continue;
            AnimalData partnerData = animalManager.getTracked(nearby.getUniqueId());
            if (partnerData == null) continue;
            if (!partnerData.getConfigName().equals(data.getConfigName())) continue;
            if (partnerData.isMale() == data.isMale()) continue;
            if (!partnerData.isGrown()) continue;
            if (!partnerData.isCourting()) continue;
            return partnerData;
        }
        return null;
    }

    private void autoBindIfNeeded(Player player, Entity entity, AnimalData data) {
        if (!data.hasOwner()) {
            animalManager.claimAnimal(entity, data, player.getUniqueId(), player.getName());
            sendPrefixMsg(player, "§a这只动物已成为你的牧场动物！");
        }
    }

    private AnimalData findPartner(Entity entity, AnimalData data, AnimalConfig config) {
        for (Entity nearby : entity.getNearbyEntities(8, 4, 8)) {
            if (nearby.getType() != entity.getType()) continue;
            AnimalData partnerData = animalManager.getTracked(nearby.getUniqueId());
            if (partnerData == null) continue;
            if (!partnerData.getConfigName().equals(data.getConfigName())) continue;
            if (partnerData.isMale() == data.isMale()) continue;
            if (!partnerData.isGrown()) continue;
            if (partnerData.isBreeding() || partnerData.isPregnant()) continue;
            return partnerData;
        }
        return null;
    }

    private void showAnimalInfo(Player player, AnimalData data, AnimalConfig config) {
        player.showTitle(Title.title(
                LiteItem.colorText(statusFormatter.formatInfoTitle(config, data)),
                LiteItem.colorText(statusFormatter.formatInfoSubtitle(player, config, data)),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(400))
        ));
    }

    private void consumeTool(Player player, ItemStack hand) {
        Material mat = hand.getType();
        if (mat == Material.SHEARS || mat == Material.BRUSH) {
            if (hand.getItemMeta() instanceof Damageable dmg) {
                dmg.setDamage(dmg.getDamage() + 1);
                hand.setItemMeta(dmg);
                if (dmg.getDamage() >= mat.getMaxDurability()) {
                    hand.setAmount(0);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                }
            }
        } else {
            hand.setAmount(hand.getAmount() - 1);
        }
    }

    private void sendPrefixMsg(Player player, String msg) {
        String prefixed = configManager.getPluginPrefix().replace("%msg%", msg);
        player.sendMessage(LiteItem.colorText(prefixed));
    }
}


