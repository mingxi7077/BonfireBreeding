package com.bonfire.breeding.model;

import com.bonfire.breeding.manager.ItemsAdderHook;
import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.concurrent.ThreadLocalRandom;

public class LiteItem {
    private String material;
    private String amount;
    private String displayName;

    public LiteItem() {}

    public LiteItem(String material, String amount, String displayName) {
        this.material = material;
        this.amount = amount;
        this.displayName = displayName;
    }

    public static LiteItem fromJson(JsonObject json) {
        LiteItem item = new LiteItem();
        item.material = json.has("material") ? json.get("material").getAsString() : "STONE";
        item.amount = json.has("amount") ? json.get("amount").getAsString() : "1";
        item.displayName = json.has("display_name") ? json.get("display_name").getAsString() : null;
        return item;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("material", material);
        json.addProperty("amount", amount);
        if (displayName != null) json.addProperty("display_name", displayName);
        return json;
    }

    public boolean isItemsAdder() {
        return material != null && material.startsWith("ia:");
    }

    private String getIaId() {
        return material.substring(3);
    }

    public int resolveAmount() {
        if (amount == null) return 1;
        if (amount.contains("-")) {
            String[] parts = amount.split("-");
            int min = Integer.parseInt(parts[0].trim());
            int max = Integer.parseInt(parts[1].trim());
            return ThreadLocalRandom.current().nextInt(min, max + 1);
        }
        return Integer.parseInt(amount.trim());
    }

    public ItemStack toItemStack() {
        int qty = resolveAmount();

        if (isItemsAdder()) {
            ItemStack iaStack = ItemsAdderHook.createItem(getIaId());
            if (iaStack != null) {
                iaStack.setAmount(qty);
                return iaStack;
            }
            // Fallback: IA unavailable or item not found
            ItemStack fallback = new ItemStack(Material.PAPER, qty);
            ItemMeta meta = fallback.getItemMeta();
            if (meta != null) {
                String name = displayName != null ? displayName : getIaId();
                meta.displayName(parseColoredText(name));
                fallback.setItemMeta(meta);
            }
            return fallback;
        }

        Material mat;
        try {
            mat = Material.valueOf(material.toUpperCase());
        } catch (IllegalArgumentException e) {
            mat = Material.STONE;
        }
        ItemStack stack = new ItemStack(mat, qty);

        if (displayName != null && !displayName.isEmpty()) {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(parseColoredText(displayName));
                stack.setItemMeta(meta);
            }
        }
        return stack;
    }

    public ItemStack toDisplayStack() {
        if (isItemsAdder()) {
            ItemStack iaStack = ItemsAdderHook.createItem(getIaId());
            if (iaStack != null) {
                iaStack.setAmount(1);
                return iaStack;
            }
        }

        Material mat;
        try {
            mat = Material.valueOf(material.toUpperCase());
        } catch (IllegalArgumentException e) {
            mat = Material.STONE;
        }
        ItemStack stack = new ItemStack(mat, 1);
        if (displayName != null && !displayName.isEmpty()) {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(parseColoredText(displayName));
                stack.setItemMeta(meta);
            }
        }
        return stack;
    }

    public boolean matchesItem(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return false;

        if (isItemsAdder()) {
            return ItemsAdderHook.matchesIaItem(stack, getIaId());
        }

        try {
            Material mat = Material.valueOf(material.toUpperCase());
            if (stack.getType() != mat) return false;
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (displayName != null && !displayName.isEmpty()) {
            ItemMeta meta = stack.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) return false;
        }
        return true;
    }

    public boolean matchesMaterial(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return false;

        if (isItemsAdder()) {
            return ItemsAdderHook.matchesIaItem(stack, getIaId());
        }

        try {
            return stack.getType() == Material.valueOf(material.toUpperCase());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static Component parseColoredText(String text) {
        String converted = text.replaceAll("\\{#([A-Fa-f0-9]{6})}", "<color:#$1>");
        converted = converted.replace("§0", "<black>").replace("§1", "<dark_blue>")
                .replace("§2", "<dark_green>").replace("§3", "<dark_aqua>")
                .replace("§4", "<dark_red>").replace("§5", "<dark_purple>")
                .replace("§6", "<gold>").replace("§7", "<gray>")
                .replace("§8", "<dark_gray>").replace("§9", "<blue>")
                .replace("§a", "<green>").replace("§b", "<aqua>")
                .replace("§c", "<red>").replace("§d", "<light_purple>")
                .replace("§e", "<yellow>").replace("§f", "<white>")
                .replace("§l", "<bold>").replace("§o", "<italic>")
                .replace("§n", "<underlined>").replace("§m", "<strikethrough>")
                .replace("§k", "<obfuscated>").replace("§r", "<reset>");
        return MiniMessage.miniMessage().deserialize(converted);
    }

    public static Component colorText(String text) {
        return parseColoredText(text);
    }

    public String getMaterial() { return material; }
    public String getAmount() { return amount; }
    public String getDisplayName() { return displayName; }
}
