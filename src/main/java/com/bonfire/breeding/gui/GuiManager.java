package com.bonfire.breeding.gui;

import com.bonfire.breeding.BonfireBreeding;
import com.bonfire.breeding.config.ConfigManager;
import com.bonfire.breeding.manager.LevelManager;
import com.bonfire.breeding.model.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class GuiManager implements Listener {
    private final BonfireBreeding plugin;
    private final ConfigManager configManager;
    private final LevelManager levelManager;
    private GuiConfig guiConfig;

    private final Map<UUID, GuiSession> sessions = new HashMap<>();

    public GuiManager(BonfireBreeding plugin, ConfigManager configManager, LevelManager levelManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.levelManager = levelManager;
        this.guiConfig = new GuiConfig();
    }

    public void setGuiConfig(GuiConfig guiConfig) {
        this.guiConfig = guiConfig != null ? guiConfig : new GuiConfig();
    }

    private static class BonfireGuiHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() { return null; }
    }

    private static final BonfireGuiHolder GUI_HOLDER = new BonfireGuiHolder();

    private boolean isOurGui(Inventory inv) {
        return inv != null && inv.getHolder() instanceof BonfireGuiHolder;
    }

    public void openMainMenu(Player player) {
        Map<String, AnimalConfig> configs = configManager.getAnimalConfigs();
        int size = Math.max(27, ((configs.size() / 7) + 1) * 9 + 18);
        size = Math.min(size, 54);

        Inventory inv = Bukkit.createInventory(GUI_HOLDER, size,
                LiteItem.colorText("{#FFDAB9}" + guiConfig.resolveMenuTitle()));

        fillBorder(inv, size);

        int slot = 10;
        List<String> configNames = new ArrayList<>(configs.keySet());
        for (int i = 0; i < configNames.size() && slot < size - 9; i++) {
            if (slot % 9 == 0) slot++;
            if (slot % 9 == 8) slot += 2;
            AnimalConfig ac = configs.get(configNames.get(i));
            ItemStack icon = createAnimalIcon(ac);
            inv.setItem(slot, icon);
            slot++;
        }

        player.openInventory(inv);

        GuiSession session = new GuiSession(GuiType.MAIN_MENU);
        session.setAnimalNames(configNames);
        sessions.put(player.getUniqueId(), session);

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 2f);
    }

    public void openAnimalDetail(Player player, AnimalConfig config) {
        Inventory inv = Bukkit.createInventory(GUI_HOLDER, 27,
                LiteItem.colorText(guiConfig.resolveDetailTitle(config.getDisplayName())));

        fillBorder(inv, 27);

        if (config.getEgg() != null) {
            Material eggMat;
            try {
                eggMat = Material.valueOf(config.getEgg().getMaterial().toUpperCase());
            } catch (IllegalArgumentException e) {
                eggMat = Material.STONE;
            }
            ItemStack eggIcon = new ItemStack(eggMat);
            ItemMeta meta = eggIcon.getItemMeta();
            if (meta != null) {
                meta.displayName(LiteItem.colorText("{#C1E1C1}" + config.getDisplayName()));
                List<Component> lore = new ArrayList<>();
                lore.add(LiteItem.colorText("§7类型: {#E0B081}" + config.getType()));
                lore.add(LiteItem.colorText("§7成长: {#E0B081}" + config.getTimeGrowth() + "秒"));
                if (config.getHarvestCooldown() > 0) {
                    lore.add(LiteItem.colorText("§7收获冷却: {#E0B081}" + config.getHarvestCooldown() + "秒"));
                }
                lore.add(Component.empty());
                lore.add(LiteItem.colorText("{#FFDAB9}▶ 点击领取生物包 §7(需管理权限)"));
                meta.lore(lore);
                eggIcon.setItemMeta(meta);
            }
            inv.setItem(11, eggIcon);
        }

        inv.setItem(13, createInfoItem(Material.CLOCK,
                "{#FFDAB9}" + configManager.translate("gui.time") + ": {#E0B081}" + config.getTimeGrowth() + "秒",
                null));

        if (!config.getDropItems().isEmpty()) {
            inv.setItem(14, createInfoItem(Material.CHEST,
                    "{#FFDAB9}" + configManager.translate("gui.drop"),
                    "§7Shift+右键收获"));
        }

        if (!config.getPassiveDropItems().isEmpty()) {
            inv.setItem(15, createInfoItem(Material.HOPPER,
                    "{#FFDAB9}" + configManager.translate("gui.passive_drop"),
                    "§7" + configManager.translate("gui.passive_drop_desk")));
        }

        if (!config.getBreedingItems().isEmpty()) {
            inv.setItem(16, createInfoItem(Material.WHEAT,
                    "{#FFDAB9}" + configManager.translate("gui.breeding_item"),
                    null));
        }

        if (!config.getTools().isEmpty()) {
            inv.setItem(17, createInfoItem(Material.SHEARS,
                    "{#FFDAB9}" + configManager.translate("gui.tools"),
                    null));
        }

        inv.setItem(18, createBackButton());

        player.openInventory(inv);

        GuiSession session = new GuiSession(GuiType.ANIMAL_DETAIL);
        session.setCurrentConfig(config.getConfigName());
        sessions.put(player.getUniqueId(), session);

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 2f);
    }

    public void openDropList(Player player, AnimalConfig config) {
        List<DropItem> drops = config.getDropItems();
        int size = Math.max(27, ((drops.size() / 7) + 1) * 9 + 18);
        size = Math.min(size, 54);

        Inventory inv = Bukkit.createInventory(GUI_HOLDER, size,
                LiteItem.colorText("{#FFDAB9}" + guiConfig.resolveListTitle(configManager.translate("gui.drop"), config.getDisplayName())));

        fillBorder(inv, size);

        int slot = 10;
        for (DropItem drop : drops) {
            if (slot % 9 == 0) slot++;
            if (slot % 9 == 8) slot += 2;
            if (slot >= size - 9) break;
            if (drop.getItem() != null) {
                ItemStack icon = createDisplayOnlyItem(drop.getItem());
                ItemMeta meta = icon.getItemMeta();
                if (meta != null) {
                    List<Component> lore = new ArrayList<>();
                    lore.add(LiteItem.colorText("{#FFDAB9}" + configManager.translate("gui.amount") + ": {#C1E1C1}" + drop.getAmount()));
                    lore.add(LiteItem.colorText("{#FFDAB9}" + configManager.translate("gui.chance") + ": {#C1E1C1}" + drop.getChance() + "%"));
                    if (drop.getLevelRequired() > 0) {
                        lore.add(LiteItem.colorText("{#FFB47E}需要等级: " + drop.getLevelRequired()));
                    }
                    meta.lore(lore);
                    icon.setItemMeta(meta);
                }
                inv.setItem(slot, icon);
            }
            slot++;
        }

        inv.setItem(size - 9, createBackButton());

        player.openInventory(inv);

        GuiSession session = new GuiSession(GuiType.DROP_LIST);
        session.setCurrentConfig(config.getConfigName());
        sessions.put(player.getUniqueId(), session);
    }

    public void openPassiveDropList(Player player, AnimalConfig config) {
        List<PassiveDropItem> drops = config.getPassiveDropItems();
        int size = Math.max(27, ((drops.size() / 7) + 1) * 9 + 18);
        size = Math.min(size, 54);

        Inventory inv = Bukkit.createInventory(GUI_HOLDER, size,
                LiteItem.colorText("{#FFDAB9}" + guiConfig.resolveListTitle(configManager.translate("gui.passive_drop"), config.getDisplayName())));

        fillBorder(inv, size);

        int slot = 10;
        for (PassiveDropItem drop : drops) {
            if (slot % 9 == 0) slot++;
            if (slot % 9 == 8) slot += 2;
            if (slot >= size - 9) break;
            if (drop.getItem() != null) {
                ItemStack icon = createDisplayOnlyItem(drop.getItem());
                ItemMeta meta = icon.getItemMeta();
                if (meta != null) {
                    List<Component> lore = new ArrayList<>();
                    lore.add(LiteItem.colorText("{#FFDAB9}" + configManager.translate("gui.amount") + ": {#C1E1C1}" + drop.getAmount()));
                    lore.add(LiteItem.colorText("{#FFDAB9}" + configManager.translate("gui.chance") + ": {#C1E1C1}" + drop.getChance() + "%"));
                    lore.add(LiteItem.colorText("{#FFDAB9}" + configManager.translate("gui.time") + ": {#C1E1C1}" + drop.getTime() + "秒"));
                    meta.lore(lore);
                    icon.setItemMeta(meta);
                }
                inv.setItem(slot, icon);
            }
            slot++;
        }

        inv.setItem(size - 9, createBackButton());

        player.openInventory(inv);

        GuiSession session = new GuiSession(GuiType.PASSIVE_DROP_LIST);
        session.setCurrentConfig(config.getConfigName());
        sessions.put(player.getUniqueId(), session);
    }

    public void openBreedingList(Player player, AnimalConfig config) {
        List<BreedingItem> items = config.getBreedingItems();
        int size = Math.max(27, ((items.size() / 7) + 1) * 9 + 18);
        size = Math.min(size, 54);

        Inventory inv = Bukkit.createInventory(GUI_HOLDER, size,
                LiteItem.colorText("{#FFDAB9}" + guiConfig.resolveListTitle(configManager.translate("gui.breeding_item"), config.getDisplayName())));

        fillBorder(inv, size);

        int slot = 10;
        for (BreedingItem bi : items) {
            if (slot % 9 == 0) slot++;
            if (slot % 9 == 8) slot += 2;
            if (slot >= size - 9) break;
            if (bi.getItem() != null) {
                ItemStack icon = createDisplayOnlyItem(bi.getItem());
                ItemMeta meta = icon.getItemMeta();
                if (meta != null) {
                    List<Component> lore = new ArrayList<>();
                    lore.add(LiteItem.colorText("{#FFDAB9}" + configManager.translate("gui.time") + ": {#C1E1C1}" + configManager.formatTime(bi.getTime())));
                    lore.add(LiteItem.colorText("{#FFDAB9}" + configManager.translate("gui.chance") + ": {#C1E1C1}" + bi.getChance() + "%"));
                    lore.add(LiteItem.colorText("{#FFDAB9}" + configManager.translate("gui.partner") + ": {#C1E1C1}" + (bi.isNeedPartner() ? "§a需要" : "§c不需要")));
                    meta.lore(lore);
                    icon.setItemMeta(meta);
                }
                inv.setItem(slot, icon);
            }
            slot++;
        }

        inv.setItem(size - 9, createBackButton());

        player.openInventory(inv);

        GuiSession session = new GuiSession(GuiType.BREEDING_LIST);
        session.setCurrentConfig(config.getConfigName());
        sessions.put(player.getUniqueId(), session);
    }

    public void openToolList(Player player, AnimalConfig config) {
        List<ToolConfig> tools = config.getTools();
        int size = Math.max(27, ((tools.size() / 7) + 1) * 9 + 18);
        size = Math.min(size, 54);

        Inventory inv = Bukkit.createInventory(GUI_HOLDER, size,
                LiteItem.colorText("{#FFDAB9}" + guiConfig.resolveListTitle(configManager.translate("gui.tools"), config.getDisplayName())));

        fillBorder(inv, size);

        int slot = 10;
        for (ToolConfig tool : tools) {
            if (slot % 9 == 0) slot++;
            if (slot % 9 == 8) slot += 2;
            if (slot >= size - 9) break;
            if (tool.getItem() != null) {
                ItemStack icon = createDisplayOnlyItem(tool.getItem());
                ItemMeta meta = icon.getItemMeta();
                if (meta != null) {
                    List<Component> lore = new ArrayList<>();
                    lore.add(LiteItem.colorText("{#FFDAB9}" + configManager.translate("gui.time") + ": {#C1E1C1}" + configManager.formatTime(tool.getTime())));
                    lore.add(LiteItem.colorText("{#FFDAB9}" + configManager.translate("gui.baby") + ": {#C1E1C1}" + (tool.isBaby() ? "§a是" : "§c否")));
                    lore.add(Component.empty());
                    lore.add(LiteItem.colorText("{#FFDAB9}掉落物:"));
                    for (DropItem drop : tool.getDrops()) {
                        if (drop.getItem() != null) {
                            String dn = drop.getItem().getDisplayName();
                            if (dn == null) dn = drop.getItem().getMaterial();
                            lore.add(LiteItem.colorText("  §7- {#C1E1C1}" + dn + " §7(" + drop.getChance() + "%)"));
                        }
                    }
                    meta.lore(lore);
                    icon.setItemMeta(meta);
                }
                inv.setItem(slot, icon);
            }
            slot++;
        }

        inv.setItem(size - 9, createBackButton());

        player.openInventory(inv);

        GuiSession session = new GuiSession(GuiType.TOOL_LIST);
        session.setCurrentConfig(config.getConfigName());
        sessions.put(player.getUniqueId(), session);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory topInv = event.getView().getTopInventory();
        if (!isOurGui(topInv)) return;

        event.setCancelled(true);
        event.setResult(org.bukkit.event.Event.Result.DENY);

        GuiSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        if (event.getRawSlot() >= topInv.getSize()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        int slot = event.getRawSlot();

        switch (session.getType()) {
            case MAIN_MENU -> handleMainMenuClick(player, session, slot, clicked);
            case ANIMAL_DETAIL -> handleAnimalDetailClick(player, session, slot, clicked);
            case DROP_LIST, PASSIVE_DROP_LIST, BREEDING_LIST, TOOL_LIST ->
                    handleListClick(player, session, slot, clicked);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory topInv = event.getView().getTopInventory();
        if (isOurGui(topInv)) {
            event.setCancelled(true);
            event.setResult(org.bukkit.event.Event.Result.DENY);
        }
    }

    private void handleMainMenuClick(Player player, GuiSession session, int slot, ItemStack clicked) {
        if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        List<String> names = session.getAnimalNames();
        int index = slotToIndex(slot);
        if (index >= 0 && index < names.size()) {
            AnimalConfig config = configManager.getAnimalConfig(names.get(index));
            if (config != null) {
                openAnimalDetail(player, config);
            }
        }
    }

    private void handleAnimalDetailClick(Player player, GuiSession session, int slot, ItemStack clicked) {
        AnimalConfig config = configManager.getAnimalConfig(session.getCurrentConfig());
        if (config == null) return;

        if (slot == 18 || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) {
            openMainMenu(player);
            return;
        }

        switch (slot) {
            case 11 -> {
                if (player.hasPermission("bonfire.breeding.admin")) {
                    ItemStack pack = createAnimalPack(config);
                    player.getInventory().addItem(pack);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
                    player.sendMessage(LiteItem.colorText("§a已获取 " + config.getDisplayName() + " §a的生物包！"));
                } else {
                    player.sendMessage(LiteItem.colorText("§c你没有权限领取生物包。"));
                }
            }
            case 13 -> {}
            case 14 -> openDropList(player, config);
            case 15 -> openPassiveDropList(player, config);
            case 16 -> openBreedingList(player, config);
            case 17 -> openToolList(player, config);
        }
    }

    private void handleListClick(Player player, GuiSession session, int slot, ItemStack clicked) {
        if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) {
            AnimalConfig config = configManager.getAnimalConfig(session.getCurrentConfig());
            if (config != null) {
                openAnimalDetail(player, config);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Inventory openInv = player.getOpenInventory().getTopInventory();
            if (!isOurGui(openInv)) {
                sessions.remove(player.getUniqueId());
            }
        }, 1L);
    }

    private int slotToIndex(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        if (col == 0 || col == 8) return -1;
        if (row == 0 || row * 9 + col >= 45) return -1;
        int rowIndex = row - 1;
        return rowIndex * 7 + (col - 1);
    }

    private ItemStack createAnimalIcon(AnimalConfig config) {
        Material mat = Material.STONE;
        if (config.getEgg() != null) {
            try {
                mat = Material.valueOf(config.getEgg().getMaterial().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LiteItem.colorText("{#C1E1C1}" + config.getDisplayName()));
            List<Component> lore = new ArrayList<>();
            lore.add(LiteItem.colorText("§7类型: {#E0B081}" + config.getType()));
            lore.add(LiteItem.colorText("§7成长时间: {#E0B081}" + config.getTimeGrowth() + "秒"));
            lore.add(Component.empty());
            lore.add(LiteItem.colorText("{#C1E1C1}• 左键 §7- 查看详情"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createDisplayOnlyItem(LiteItem liteItem) {
        ItemStack stack = liteItem.toDisplayStack();
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (liteItem.getDisplayName() != null && !liteItem.getDisplayName().isEmpty()) {
                meta.displayName(LiteItem.colorText(liteItem.getDisplayName()));
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack createInfoItem(Material mat, String name, String loreText) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LiteItem.colorText(name));
            if (loreText != null) {
                List<Component> lore = new ArrayList<>();
                for (String line : loreText.split("\n")) {
                    lore.add(LiteItem.colorText(line.trim()));
                }
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createAnimalPack(AnimalConfig config) {
        Material packMat = Material.PAPER;
        if (config.getEgg() != null) {
            try {
                packMat = Material.valueOf(config.getEgg().getMaterial().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        ItemStack item = new ItemStack(packMat, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LiteItem.colorText("{#FFDAB9}✦ " + config.getDisplayName() + " §7的生物包"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(LiteItem.colorText("§7右键使用将召唤一只"));
            lore.add(LiteItem.colorText(config.getDisplayName() + " §7来到你身边"));
            lore.add(Component.empty());
            lore.add(LiteItem.colorText("{#E0B081}✧ §7使用后自动绑定为你的牧场动物"));
            lore.add(LiteItem.colorText("{#C1E1C1}• §7类型: {#E0B081}" + config.getType()));
            lore.add(LiteItem.colorText("{#C1E1C1}• §7成长: {#E0B081}" + config.getTimeGrowth() + "秒"));
            meta.lore(lore);

            NamespacedKey key = new NamespacedKey(plugin, "bonfire_animal_pack");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, config.getConfigName());
            meta.setCustomModelData(39001);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LiteItem.colorText("{#FFDAB9}" + configManager.translate("gui.back")));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fillBorder(Inventory inv, int size) {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            border.setItemMeta(meta);
        }

        for (int i = 0; i < 9; i++) inv.setItem(i, border.clone());
        for (int i = size - 9; i < size; i++) inv.setItem(i, border.clone());
        for (int i = 9; i < size - 9; i += 9) {
            inv.setItem(i, border.clone());
            inv.setItem(i + 8, border.clone());
        }
    }

    enum GuiType {
        MAIN_MENU, ANIMAL_DETAIL, DROP_LIST, PASSIVE_DROP_LIST, BREEDING_LIST, TOOL_LIST
    }

    static class GuiSession {
        private final GuiType type;
        private String currentConfig;
        private List<String> animalNames;

        public GuiSession(GuiType type) { this.type = type; }

        public GuiType getType() { return type; }
        public String getCurrentConfig() { return currentConfig; }
        public void setCurrentConfig(String config) { this.currentConfig = config; }
        public List<String> getAnimalNames() { return animalNames; }
        public void setAnimalNames(List<String> names) { this.animalNames = names; }
    }
}
