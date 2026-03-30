package com.bonfire.breeding.manager;

import com.bonfire.breeding.config.ConfigManager;
import com.bonfire.breeding.model.AnimalConfig;
import com.bonfire.breeding.model.AnimalData;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class AnimalStatusFormatter {
    private final ConfigManager configManager;

    public AnimalStatusFormatter(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public String formatActionBar(Player viewer, AnimalConfig config, AnimalData data) {
        List<String> parts = new ArrayList<>();
        parts.add(config.getDisplayName());
        parts.add(getGenderText(data));
        parts.add(formatOwner(viewer, data));
        parts.add(formatMood(data));
        parts.add(formatHunger(data, config));
        parts.add(formatState(data, config));
        return String.join(" §7| ", parts);
    }

    public String formatInfoTitle(AnimalConfig config, AnimalData data) {
        return config.getDisplayName() + " " + getGenderText(data);
    }

    public String formatInfoSubtitle(Player viewer, AnimalConfig config, AnimalData data) {
        List<String> parts = new ArrayList<>();
        parts.add(formatOwner(viewer, data));
        parts.add(formatMood(data));
        parts.add(formatHunger(data, config));
        parts.add(formatState(data, config));
        return String.join(" §7| ", parts);
    }

    public String formatMoodChange(int oldMood, int newMood) {
        return translate("status.label_mood", "{#FF8FA3}♥ 心情")
                + "{#E6EDF3}: {#FFFFFF}"
                + oldMood
                + " → "
                + newMood
                + "{#C9D1D9}（"
                + getMoodLevelText(newMood)
                + "{#C9D1D9}）";
    }

    private String formatOwner(Player viewer, AnimalData data) {
        String ownerValue;
        if (!data.hasOwner()) {
            ownerValue = translate("status.owner_wild", "{#F2CC60}野生");
        } else if (viewer != null && data.isOwner(viewer.getUniqueId())) {
            ownerValue = translate("status.owner_self", "{#7EE787}你的");
        } else if (data.getOwnerName() != null && !data.getOwnerName().isBlank()) {
            ownerValue = "{#79C0FF}" + data.getOwnerName();
        } else {
            ownerValue = "{#79C0FF}已认领";
        }
        return translate("status.label_owner", "{#FFDAB9}归属") + "{#E6EDF3}: " + ownerValue;
    }

    private String formatMood(AnimalData data) {
        return translate("status.label_mood", "{#FF8FA3}♥ 心情")
                + "{#E6EDF3}: {#FFFFFF}"
                + data.getMood()
                + "/100"
                + "{#C9D1D9}（"
                + getMoodLevelText(data.getMood())
                + "{#C9D1D9}）";
    }

    private String formatHunger(AnimalData data, AnimalConfig config) {
        int maxHunger = config.getHungerConfig().getMaxHunger();
        return translate("status.label_hunger", "{#FFDAB9}饱腹")
                + "{#E6EDF3}: {#FFFFFF}"
                + data.getHunger()
                + "/"
                + maxHunger
                + "{#C9D1D9}（"
                + getHungerLevelText(data.getHunger(), maxHunger)
                + "{#C9D1D9}）";
    }

    private String formatState(AnimalData data, AnimalConfig config) {
        List<String> states = new ArrayList<>();

        if (!data.isGrown()) {
            states.add(translate("status.state_growing", "{#7CC7FF}成长期")
                    + " {#E6EDF3}"
                    + configManager.formatTime(data.getRemainingGrowthSeconds()));
        } else {
            states.add(translate("game.grown", "{#7EE787}已成年"));
        }

        if (data.isSaturated()) {
            states.add(translate("status.state_saturated", "{#7EE787}饱和"));
        } else if (data.isStarving()) {
            states.add(translate("status.state_starving", "{#FF6B6B}饥饿中"));
        }

        if (data.isCourting()) {
            states.add(translate("status.state_courting", "{#F2CC60}求偶")
                    + " {#E6EDF3}"
                    + configManager.formatTime(data.getRemainingCourtingSeconds()));
        }
        if (data.isMating()) {
            states.add(translate("status.state_mating", "{#FF6B9A}交配中"));
        }
        if (data.isBreedCooldown()) {
            states.add(translate("status.state_breed_cooldown", "{#C9D1D9}繁殖冷却")
                    + " {#E6EDF3}"
                    + configManager.formatTime(data.getRemainingBreedCooldownSeconds()));
        }
        if (data.isPregnant()) {
            states.add(translate("status.state_pregnant", "{#FF79C6}孕期")
                    + " {#E6EDF3}"
                    + configManager.formatTime(data.getRemainingPregnancySeconds()));
        }
        if (data.isToolCooldown()) {
            states.add(translate("status.state_tool_cooldown", "{#C9D1D9}工具冷却")
                    + " {#E6EDF3}"
                    + configManager.formatTime(data.getRemainingToolSeconds()));
        }

        return translate("status.label_status", "{#FFDAB9}状态")
                + "{#E6EDF3}: "
                + String.join("{#C9D1D9}、", states);
    }

    private String getGenderText(AnimalData data) {
        return data.isMale()
                ? translate("game.male", "{#58BCFF}♂ 公")
                : translate("game.female", "{#FF79F3}♀ 母");
    }

    private String getMoodLevelText(int mood) {
        if (mood >= 80) {
            return translate("status.mood_happy", "{#7EE787}愉悦");
        }
        if (mood >= 50) {
            return translate("status.mood_normal", "{#F2CC60}普通");
        }
        if (mood >= 20) {
            return translate("status.mood_low", "{#FF8F5A}低落");
        }
        return translate("status.mood_bad", "{#FF6B6B}烦躁");
    }

    private String getHungerLevelText(int hunger, int maxHunger) {
        int safeMax = Math.max(1, maxHunger);
        double ratio = hunger / (double) safeMax;
        if (ratio >= 0.8D) {
            return translate("status.hunger_full", "{#7EE787}充足");
        }
        if (ratio >= 0.5D) {
            return translate("status.hunger_normal", "{#F2CC60}正常");
        }
        if (ratio >= 0.2D) {
            return translate("status.hunger_low", "{#FF8F5A}偏低");
        }
        return translate("status.hunger_starving", "{#FF6B6B}饥饿");
    }

    private String translate(String key, String fallback) {
        return configManager.translateOrDefault(key, fallback);
    }
}
