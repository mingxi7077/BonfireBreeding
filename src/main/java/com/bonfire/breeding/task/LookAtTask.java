package com.bonfire.breeding.task;

import com.bonfire.breeding.BonfireBreeding;
import com.bonfire.breeding.manager.AnimalManager;
import com.bonfire.breeding.manager.AnimalStatusFormatter;
import com.bonfire.breeding.model.AnimalConfig;
import com.bonfire.breeding.model.AnimalData;
import com.bonfire.breeding.model.LiteItem;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LookAtTask extends BukkitRunnable {
    private final BonfireBreeding plugin;
    private final AnimalManager animalManager;
    private final AnimalStatusFormatter statusFormatter;
    private final Set<UUID> disabledPlayers = new HashSet<>();
    private final Set<String> matingBlindTriggered = new HashSet<>();

    public LookAtTask(BonfireBreeding plugin, AnimalManager animalManager, AnimalStatusFormatter statusFormatter) {
        this.plugin = plugin;
        this.animalManager = animalManager;
        this.statusFormatter = statusFormatter;
    }

    public boolean isDisabled(UUID playerId) {
        return disabledPlayers.contains(playerId);
    }

    public boolean toggle(UUID playerId) {
        if (disabledPlayers.contains(playerId)) {
            disabledPlayers.remove(playerId);
            return true;
        }
        disabledPlayers.add(playerId);
        return false;
    }

    @Override
    public void run() {
        Set<String> currentMatingPairs = new HashSet<>();
        for (AnimalData data : animalManager.getAllTracked().values()) {
            if (data.isMating() && data.getMatingPartner() != null) {
                currentMatingPairs.add(matingPairKey(data.getUuid(), data.getMatingPartner()));
            }
        }
        matingBlindTriggered.removeIf(key -> {
            int index = key.indexOf("##");
            if (index < 0) {
                return true;
            }
            String pairKey = key.substring(index + 2);
            return !currentMatingPairs.contains(pairKey);
        });

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (disabledPlayers.contains(player.getUniqueId())) {
                continue;
            }

            Entity target = player.getTargetEntity(5);
            if (target == null || (!(target instanceof Animals) && !(target instanceof Bee))) {
                continue;
            }
            if (animalManager.handleExcludedEntity(target, "look-at")) {
                continue;
            }

            AnimalData data = animalManager.getTracked(target.getUniqueId());
            if (data == null || data.isInactive()) {
                continue;
            }

            AnimalConfig config = animalManager.getConfigFor(data);
            if (config == null) {
                continue;
            }

            if (data.isMating()) {
                String pairKey = matingPairKey(data.getUuid(), data.getMatingPartner());
                String triggerKey = player.getUniqueId() + "##" + pairKey;
                if (!matingBlindTriggered.contains(triggerKey)) {
                    matingBlindTriggered.add(triggerKey);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0, false, false));
                    player.showTitle(Title.title(
                            LiteItem.colorText("§c注意"),
                            LiteItem.colorText("§7动物正在交配中"),
                            Title.Times.times(Duration.ofMillis(100), Duration.ofSeconds(2), Duration.ofMillis(300))
                    ));
                }
            }

            player.sendActionBar(LiteItem.colorText(statusFormatter.formatActionBar(player, config, data)));
        }
    }

    private static String matingPairKey(UUID a, UUID b) {
        if (a == null || b == null) {
            return "";
        }
        return a.compareTo(b) <= 0 ? a + "::" + b : b + "::" + a;
    }
}
