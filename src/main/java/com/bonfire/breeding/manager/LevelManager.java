package com.bonfire.breeding.manager;

import com.bonfire.breeding.BonfireBreeding;
import com.bonfire.breeding.model.DropItem;
import com.bonfire.breeding.model.LiteItem;
import com.google.gson.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class LevelManager {
    private final BonfireBreeding plugin;
    private final List<LevelEntry> levels = new ArrayList<>();
    private String newLevelMsg = "§7恭喜！你已晋升为 %level_name%！";
    private final Map<UUID, Integer> playerExp = new ConcurrentHashMap<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public LevelManager(BonfireBreeding plugin) {
        this.plugin = plugin;
    }

    public void load(JsonObject levelsConfig) {
        levels.clear();
        if (levelsConfig != null && levelsConfig.has("levels")) {
            JsonArray arr = levelsConfig.getAsJsonArray("levels");
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String displayName = obj.has("display_name") ? obj.get("display_name").getAsString() : "Unknown";
                int needExp = obj.has("need_exp") ? obj.get("need_exp").getAsInt() : 0;
                levels.add(new LevelEntry(displayName, needExp));
            }
        }
        if (levelsConfig != null && levelsConfig.has("new_level_msg")) {
            newLevelMsg = levelsConfig.get("new_level_msg").getAsString();
        }

        loadExp();
    }

    // --- Persistence ---

    private File getExpFile() {
        return new File(plugin.getDataFolder(), "player_exp.json");
    }

    public void loadExp() {
        playerExp.clear();
        File file = getExpFile();
        if (!file.exists()) return;
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    int exp = entry.getValue().getAsInt();
                    playerExp.put(uuid, exp);
                } catch (Exception ignored) {}
            }
            plugin.getLogger().info("Loaded player exp for " + playerExp.size() + " players.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load player_exp.json", e);
        }
    }

    public void saveExp() {
        File file = getExpFile();
        JsonObject json = new JsonObject();
        for (Map.Entry<UUID, Integer> entry : playerExp.entrySet()) {
            json.addProperty(entry.getKey().toString(), entry.getValue());
        }
        try {
            file.getParentFile().mkdirs();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save player_exp.json", e);
        }
    }

    // --- Level logic ---

    public int getPlayerLevel(Player player) {
        int exp = playerExp.getOrDefault(player.getUniqueId(), 0);
        int level = 0;
        for (int i = levels.size() - 1; i >= 0; i--) {
            if (exp >= levels.get(i).needExp) {
                level = i;
                break;
            }
        }
        return level;
    }

    public String getLevelName(int level) {
        if (level >= 0 && level < levels.size()) {
            return levels.get(level).displayName;
        }
        return "Unknown";
    }

    public int getPlayerExp(Player player) {
        return playerExp.getOrDefault(player.getUniqueId(), 0);
    }

    private int getNextLevelExp(int currentLevel) {
        if (currentLevel + 1 < levels.size()) {
            return levels.get(currentLevel + 1).needExp;
        }
        return -1;
    }

    public void addExp(Player player, int amount) {
        int oldLevel = getPlayerLevel(player);
        playerExp.merge(player.getUniqueId(), amount, Integer::sum);
        int newLevel = getPlayerLevel(player);

        int currentExp = playerExp.getOrDefault(player.getUniqueId(), 0);
        int nextExp = getNextLevelExp(newLevel);
        String progressText;
        if (nextExp > 0) {
            progressText = currentExp + "/" + nextExp;
        } else {
            progressText = currentExp + " §7(已满级)";
        }

        String actionBarText = "§7[牧场] §a+" + amount + "经验 §8| §7" + progressText + " §8| " + getLevelName(newLevel);
        player.sendActionBar(LiteItem.colorText(actionBarText));

        if (newLevel > oldLevel) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            player.showTitle(Title.title(
                    LiteItem.colorText("{#FFD700}§l牧场升级！"),
                    LiteItem.colorText(getLevelName(newLevel) + " §7- 解锁新掉落物！"),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(500))
            ));
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                    player.getLocation().add(0, 1, 0),
                    10, 0.5, 1, 0.5, 0.1);
        }
    }

    public void notifyLockedDrop(Player player, DropItem drop, int currentLevel) {
        int required = drop.getLevelRequired();
        String levelName = getLevelName(required);
        String itemName = "未知物品";
        if (drop.getItem() != null && drop.getItem().getDisplayName() != null) {
            itemName = drop.getItem().getDisplayName();
        }
        String prefix = plugin.getConfigManager().getPluginPrefix()
                .replace("%msg%", "§8你还未解锁 §7" + itemName + " §8(需要 " + levelName + "§8)");
        player.sendMessage(LiteItem.colorText(prefix));
    }

    public List<LevelEntry> getLevels() {
        return levels;
    }

    public static class LevelEntry {
        public final String displayName;
        public final int needExp;

        public LevelEntry(String displayName, int needExp) {
            this.displayName = displayName;
            this.needExp = needExp;
        }
    }
}
