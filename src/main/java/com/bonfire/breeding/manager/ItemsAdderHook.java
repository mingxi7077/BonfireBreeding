package com.bonfire.breeding.manager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Soft dependency adapter for ItemsAdder.
 * Uses reflection so the plugin compiles and runs without IA on the classpath.
 */
public class ItemsAdderHook {
    private static boolean available = false;
    private static Method getInstanceMethod;
    private static Method getItemStackMethod;
    private static Method getIdMethod;

    public static void init(Logger logger) {
        if (!Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) {
            logger.info("ItemsAdder not found - custom items will use fallback materials.");
            return;
        }
        try {
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            getInstanceMethod = customStackClass.getMethod("getInstance", String.class);
            getItemStackMethod = customStackClass.getMethod("getItemStack");
            getIdMethod = customStackClass.getMethod("getId");
            available = true;
            logger.info("ItemsAdder hooked successfully.");
        } catch (Exception e) {
            logger.log(Level.WARNING, "ItemsAdder found but failed to hook API", e);
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    /**
     * Create an ItemStack from an ItemsAdder namespaced ID (e.g. "redstone_labs:fox_pelt").
     * Returns null if IA is unavailable or the item doesn't exist.
     */
    public static ItemStack createItem(String namespacedId) {
        if (!available) return null;
        try {
            Object customStack = getInstanceMethod.invoke(null, namespacedId);
            if (customStack == null) return null;
            return (ItemStack) getItemStackMethod.invoke(customStack);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if an ItemStack is a specific ItemsAdder item.
     */
    public static boolean matchesIaItem(ItemStack stack, String namespacedId) {
        if (!available || stack == null) return false;
        try {
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Method byStack = customStackClass.getMethod("byItemStack", ItemStack.class);
            Object customStack = byStack.invoke(null, stack);
            if (customStack == null) return false;
            String id = (String) getIdMethod.invoke(customStack);
            return namespacedId.equals(id);
        } catch (Exception e) {
            return false;
        }
    }
}
