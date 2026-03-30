package com.bonfire.breeding.config;

import com.bonfire.breeding.BonfireBreeding;
import com.bonfire.breeding.gui.GuiConfig;
import com.bonfire.breeding.model.AnimalConfig;
import com.bonfire.breeding.model.ToolConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {
    private final BonfireBreeding plugin;
    private final File dataFolder;
    private final File animalsDir;

    private JsonObject mainConfig;
    private JsonObject customization;
    private JsonObject levelsConfig;
    private final Map<String, AnimalConfig> animalConfigs = new LinkedHashMap<>();
    private final Map<String, String> translations = new LinkedHashMap<>();

    private String pluginPrefix = "{#FFDAB9}[牧场]§7 %msg%";
    private String timeFormat = "{#FFDAB9}%days_{#E0B081}天% {#FFDAB9}%hours_{#E0B081}时% {#FFDAB9}%minutes_{#E0B081}分% {#FFDAB9}%seconds_{#E0B081}秒%";
    private boolean debugExternalEntityFilter = false;
    private GuiConfig guiConfig = new GuiConfig();
    private final Map<String, Integer> toolTimeTiers = new LinkedHashMap<>();
    private String defaultRarity = "normal";
    private final Map<String, String> animalRarityOverrides = new LinkedHashMap<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public ConfigManager(BonfireBreeding plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        this.animalsDir = new File(dataFolder, "animals_config");
    }

    public void loadAll() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        if (!animalsDir.exists()) {
            animalsDir.mkdirs();
            extractDefaultAnimals();
        }

        loadMainConfig();
        loadCustomization();
        loadLevels();
        loadAnimalConfigs();
    }

    private void extractDefaultAnimals() {
        String[] defaults = {
                "example_cow.json",
                "basic_sheep.json",
                "basic_pig.json",
                "basic_chicken.json",
                "basic_fox.json",
                "basic_bee.json"
        };
        for (String name : defaults) {
            extractResource("data/animals_config/" + name, new File(animalsDir, name));
        }
    }

    private void extractResource(String resourcePath, File target) {
        if (target.exists()) {
            return;
        }
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in != null) {
                target.getParentFile().mkdirs();
                Files.copy(in, target.toPath());
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to extract " + resourcePath, e);
        }
    }

    private void loadMainConfig() {
        File file = new File(dataFolder, "config.json");
        if (!file.exists()) {
            extractResource("data/config.json", file);
        }
        mainConfig = readJsonFile(file);
        if (mainConfig == null) {
            mainConfig = new JsonObject();
        }

        debugExternalEntityFilter = false;
        if (mainConfig.has("settings")) {
            JsonObject settings = mainConfig.getAsJsonObject("settings");
            pluginPrefix = settings.has("plugin_prefix") ? settings.get("plugin_prefix").getAsString() : pluginPrefix;
            timeFormat = settings.has("time_format") ? settings.get("time_format").getAsString() : timeFormat;
            debugExternalEntityFilter = settings.has("debug_external_entity_filter")
                    && settings.get("debug_external_entity_filter").getAsBoolean();
        }

        if (mainConfig.has("gui")) {
            guiConfig = GuiConfig.fromJson(mainConfig.getAsJsonObject("gui"));
        } else {
            guiConfig = new GuiConfig();
        }

        toolTimeTiers.clear();
        if (mainConfig.has("tool_time_tiers")) {
            JsonObject tiers = mainConfig.getAsJsonObject("tool_time_tiers");
            for (Map.Entry<String, JsonElement> entry : tiers.entrySet()) {
                toolTimeTiers.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue().getAsInt());
            }
        }
        if (mainConfig.has("default_rarity")) {
            defaultRarity = mainConfig.get("default_rarity").getAsString().toLowerCase(Locale.ROOT);
        }
        animalRarityOverrides.clear();
        if (mainConfig.has("animal_rarity_map")) {
            JsonObject map = mainConfig.getAsJsonObject("animal_rarity_map");
            for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
                animalRarityOverrides.put(
                        entry.getKey().toLowerCase(Locale.ROOT),
                        entry.getValue().getAsString().toLowerCase(Locale.ROOT)
                );
            }
        }
        if (toolTimeTiers.isEmpty()) {
            toolTimeTiers.put("normal", 1000);
            toolTimeTiers.put("rare", 1500);
            toolTimeTiers.put("epic", 2000);
            toolTimeTiers.put("legendary", 2500);
        }
    }

    private void loadCustomization() {
        File file = new File(dataFolder, "customization.json");
        if (!file.exists()) {
            extractResource("data/customization.json", file);
        }
        customization = readJsonFile(file);
        if (customization == null) {
            customization = new JsonObject();
        }

        translations.clear();
        if (customization.has("translation")) {
            JsonObject trans = customization.getAsJsonObject("translation");
            for (Map.Entry<String, JsonElement> entry : trans.entrySet()) {
                translations.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
    }

    private void loadLevels() {
        File file = new File(dataFolder, "levels.json");
        if (!file.exists()) {
            extractResource("data/levels.json", file);
        }
        levelsConfig = readJsonFile(file);
        if (levelsConfig == null) {
            levelsConfig = new JsonObject();
        }
    }

    private void loadAnimalConfigs() {
        animalConfigs.clear();
        File[] files = animalsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            try {
                JsonObject json = readJsonFile(file);
                if (json != null) {
                    AnimalConfig config = AnimalConfig.fromJson(json, file.getName());
                    animalConfigs.put(config.getConfigName(), config);
                    plugin.getLogger().info("Loaded animal config: " + config.getConfigName()
                            + " (" + config.getType() + ")");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load animal config: " + file.getName(), e);
            }
        }
    }

    public void saveEntityData(JsonObject entityData) {
        File file = new File(dataFolder, "animals_entity.json");
        writeJsonFile(file, entityData);
    }

    public void writeEntityDataRaw(String jsonString) {
        File file = new File(dataFolder, "animals_entity.json");
        try {
            file.getParentFile().mkdirs();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                writer.write(jsonString);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to write entity data async", e);
        }
    }

    public JsonObject loadEntityData() {
        File file = new File(dataFolder, "animals_entity.json");
        if (!file.exists()) {
            JsonObject empty = new JsonObject();
            empty.add("map", new JsonObject());
            return empty;
        }
        JsonObject data = readJsonFile(file);
        return data != null ? data : new JsonObject();
    }

    private JsonObject readJsonFile(File file) {
        if (!file.exists()) {
            return null;
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to read JSON: " + file.getName(), e);
            return null;
        }
    }

    private void writeJsonFile(File file, JsonObject data) {
        try {
            file.getParentFile().mkdirs();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to write JSON: " + file.getName(), e);
        }
    }

    public String translate(String key) {
        return translations.getOrDefault(key, key);
    }

    public String translateOrDefault(String key, String fallback) {
        return translations.getOrDefault(key, fallback);
    }

    public String formatTime(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        String result = timeFormat;
        result = replaceTimeToken(result, "days", days, false);
        result = replaceTimeToken(result, "hours", hours, false);
        result = replaceTimeToken(result, "minutes", minutes, false);
        result = replaceTimeToken(result, "seconds", seconds, true);
        return result.replaceAll("\\s+", " ").trim();
    }

    private String replaceTimeToken(String format, String token, long value, boolean alwaysShow) {
        Pattern pattern = Pattern.compile("%" + token + "_(.*?)%");
        Matcher matcher = pattern.matcher(format);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String replacement = (alwaysShow || value > 0)
                    ? value + matcher.group(1)
                    : "";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    public String getPluginPrefix() { return pluginPrefix; }

    public boolean isDebugExternalEntityFilter() { return debugExternalEntityFilter; }

    public Map<String, AnimalConfig> getAnimalConfigs() { return animalConfigs; }

    public AnimalConfig getAnimalConfig(String configName) { return animalConfigs.get(configName); }

    public JsonObject getLevelsConfig() { return levelsConfig; }

    public JsonObject getCustomization() { return customization; }

    public GuiConfig getGuiConfig() { return guiConfig; }

    public int getEffectiveToolTime(AnimalConfig config, ToolConfig tool) {
        int base = tool != null ? tool.getTime() : 0;
        String rarity = config != null ? config.getRarity() : null;
        if (config != null) {
            String override = animalRarityOverrides.get(config.getConfigName().toLowerCase(Locale.ROOT));
            if (override != null && !override.isBlank()) {
                rarity = override;
            }
        }
        String key = rarity != null && !rarity.isBlank() ? rarity.toLowerCase(Locale.ROOT) : defaultRarity;
        int tier = toolTimeTiers.getOrDefault(key, toolTimeTiers.getOrDefault(defaultRarity, 1000));
        int min = Math.max(1000, tier);
        return Math.max(base, min);
    }

    public AnimalConfig findDefaultConfig(String entityType) {
        for (AnimalConfig config : animalConfigs.values()) {
            if (config.isDefaultEntity() && config.getType().equalsIgnoreCase(entityType)) {
                return config;
            }
        }
        return null;
    }
}
