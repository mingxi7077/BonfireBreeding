package com.bonfire.breeding.command;

import com.bonfire.breeding.BonfireBreeding;
import com.bonfire.breeding.config.ConfigManager;
import com.bonfire.breeding.gui.GuiManager;
import com.bonfire.breeding.manager.AnimalManager;
import com.bonfire.breeding.manager.LevelManager;
import com.bonfire.breeding.model.AnimalConfig;
import com.bonfire.breeding.model.AnimalData;
import com.bonfire.breeding.model.LiteItem;
import com.bonfire.breeding.task.LookAtTask;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class BreedingCommand implements CommandExecutor, TabCompleter {
    public static final String PACK_KEY_NAME = "bonfire_animal_pack";

    private final BonfireBreeding plugin;
    private final ConfigManager configManager;
    private final AnimalManager animalManager;
    private final GuiManager guiManager;
    private final LevelManager levelManager;
    private final LookAtTask lookAtTask;

    public BreedingCommand(
            BonfireBreeding plugin,
            ConfigManager configManager,
            AnimalManager animalManager,
            GuiManager guiManager,
            LevelManager levelManager,
            LookAtTask lookAtTask
    ) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.animalManager = animalManager;
        this.guiManager = guiManager;
        this.levelManager = levelManager;
        this.lookAtTask = lookAtTask;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "menu" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can use this command.");
                    return true;
                }
                guiManager.openMainMenu(player);
            }
            case "reload" -> {
                if (!sender.hasPermission("bonfire.breeding.admin")) {
                    sendMsg(sender, "§c你没有权限执行此命令。");
                    return true;
                }
                configManager.loadAll();
                animalManager.loadData();
                animalManager.bootstrapLoadedChunks();
                levelManager.load(configManager.getLevelsConfig());
                guiManager.setGuiConfig(configManager.getGuiConfig());
                sendMsg(sender, "§a" + configManager.translate("gui.reload_done"));
            }
            case "give" -> handleGive(sender, args);
            case "info" -> handleInfo(sender);
            case "list" -> handleList(sender);
            case "toggle" -> handleToggle(sender);
            case "transfer" -> handleTransfer(sender, args);
            case "unbind" -> handleUnbind(sender);
            case "help" -> sendHelp(sender);
            case "admin" -> handleAdmin(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    public ItemStack createAnimalPack(AnimalConfig config) {
        return plugin.createAnimalPackItem(config, "{#FFDAB9}Pack " + config.getDisplayName());
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bonfire.breeding.admin")) {
            sendMsg(sender, "§c你没有权限执行此命令。");
            return;
        }
        if (args.length < 2) {
            sendMsg(sender, "§c用法: /bbreeding give <动物配置名> [玩家]");
            return;
        }

        String configName = args[1];
        AnimalConfig config = configManager.getAnimalConfig(configName);
        if (config == null && args.length >= 3) {
            for (int end = args.length - 1; end >= 2; end--) {
                StringBuilder sb = new StringBuilder(args[1]);
                for (int i = 2; i <= end; i++) {
                    sb.append(" ").append(args[i]);
                }
                config = configManager.getAnimalConfig(sb.toString());
                if (config != null) {
                    configName = sb.toString();
                    break;
                }
            }
        }
        if (config == null) {
            configName = joinArgs(args, 1);
            config = configManager.getAnimalConfig(configName);
        }
        if (config == null) {
            sendMsg(sender, "§c找不到动物配置: " + joinArgs(args, 1));
            return;
        }

        Player target;
        if (args.length > 2) {
            Player maybeTarget = Bukkit.getPlayerExact(args[args.length - 1]);
            if (maybeTarget != null) {
                target = maybeTarget;
            } else if (sender instanceof Player player) {
                target = player;
            } else {
                sendMsg(sender, "§c请指定在线玩家。");
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sendMsg(sender, "§c请指定目标玩家。");
            return;
        }

        target.getInventory().addItem(createAnimalPack(config));
        sendMsg(sender, "§a已给予 " + target.getName() + " 一个 " + config.getDisplayName() + " §a生物包。");
    }

    private void handleInfo(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }
        int level = levelManager.getPlayerLevel(player);
        int exp = levelManager.getPlayerExp(player);
        sendMsg(sender, "§7牧场等级: " + levelManager.getLevelName(level) + " §7(经验: §e" + exp + "§7)");
    }

    private void handleList(CommandSender sender) {
        sendMsg(sender, "§7已注册动物配置:");
        for (AnimalConfig config : configManager.getAnimalConfigs().values()) {
            sender.sendMessage(LiteItem.colorText("  §7- " + config.getDisplayName() + " §7(" + config.getType() + ")"));
        }
    }

    private void handleToggle(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }
        boolean enabled = lookAtTask.toggle(player.getUniqueId());
        sendMsg(sender, enabled ? "§a状态提示已开启" : "§c状态提示已关闭");
    }

    private void handleTransfer(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }
        if (args.length < 2) {
            sendMsg(sender, "§c用法: /bbreeding transfer <玩家>");
            return;
        }

        Player targetPlayer = Bukkit.getPlayerExact(args[1]);
        if (targetPlayer == null) {
            sendMsg(sender, "§c找不到在线玩家: " + args[1]);
            return;
        }

        Entity target = getTargetAnimal(player);
        if (target == null) {
            sendMsg(sender, "§c请看向你要转让的动物。");
            return;
        }

        AnimalData data = animalManager.getTracked(target.getUniqueId());
        if (data == null) {
            sendMsg(sender, "§c该动物未被追踪。");
            return;
        }
        if (!data.isOwner(player.getUniqueId()) && !player.hasPermission("bonfire.breeding.admin")) {
            sendMsg(sender, "§c这不是你的动物，无法转让。");
            return;
        }

        animalManager.claimAnimal(target, data, targetPlayer.getUniqueId(), targetPlayer.getName());
        sendMsg(sender, "§a已将动物转让给 " + targetPlayer.getName() + "。");
        sendMsg(targetPlayer, "§a" + player.getName() + " 将一只牧场动物转让给了你。");
    }

    private void handleUnbind(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }

        Entity target = getTargetAnimal(player);
        if (target == null) {
            sendMsg(sender, "§c请看向你要解绑的动物。");
            return;
        }

        AnimalData data = animalManager.getTracked(target.getUniqueId());
        if (data == null) {
            sendMsg(sender, "§c该动物未被追踪。");
            return;
        }
        if (!data.isOwner(player.getUniqueId()) && !player.hasPermission("bonfire.breeding.admin")) {
            sendMsg(sender, "§c这不是你的动物，无法解绑。");
            return;
        }

        animalManager.unclaimAnimal(target, data);
        sendMsg(sender, "§a已解除该动物的归属。");
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bonfire.breeding.admin")) {
            sendMsg(sender, "§c你没有权限执行此命令。");
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }
        if (args.length < 2) {
            sendMsg(sender, "§c用法: /bb admin <gender|mood|hunger|courting|clearpregnancy|clearcooldown|clearcourting|clearmating>");
            return;
        }

        Entity target = getTargetAnimal(player);
        if (target == null) {
            sendMsg(sender, "§c请看向目标动物。");
            return;
        }

        AnimalData data = animalManager.getTracked(target.getUniqueId());
        if (data == null) {
            sendMsg(sender, "§c该动物未被追踪。");
            return;
        }

        AnimalConfig targetConfig = animalManager.getConfigFor(data);
        int maxHunger = targetConfig != null ? targetConfig.getHungerConfig().getMaxHunger() : 200;

        switch (args[1].toLowerCase()) {
            case "gender" -> {
                if (args.length < 3) {
                    sendMsg(sender, "§c用法: /bb admin gender <male|female>");
                    return;
                }
                if (args[2].equalsIgnoreCase("male")) {
                    data.setMale(true);
                    animalManager.syncToPDC(target, data);
                    sendMsg(sender, "§a已将动物性别设置为公。");
                } else if (args[2].equalsIgnoreCase("female")) {
                    data.setMale(false);
                    animalManager.syncToPDC(target, data);
                    sendMsg(sender, "§a已将动物性别设置为母。");
                } else {
                    sendMsg(sender, "§c参数必须是 male 或 female。");
                }
            }
            case "mood" -> {
                if (args.length < 3) {
                    sendMsg(sender, "§c用法: /bb admin mood <0-100>");
                    return;
                }
                try {
                    int value = Integer.parseInt(args[2]);
                    data.setMood(value);
                    animalManager.syncMoodToPDC(target, data);
                    sendMsg(sender, "§a已将动物心情设置为 §e" + data.getMood());
                } catch (NumberFormatException e) {
                    sendMsg(sender, "§c请输入有效数字。");
                }
            }
            case "hunger" -> {
                if (args.length < 3) {
                    sendMsg(sender, "§c用法: /bb admin hunger <0-" + maxHunger + ">");
                    return;
                }
                try {
                    int value = Integer.parseInt(args[2]);
                    if (value < 0 || value > maxHunger) {
                        sendMsg(sender, "§c请输入 0 到 " + maxHunger + " 之间的数值。");
                        return;
                    }
                    data.setHunger(value, maxHunger);
                    data.setLastStarveTick(0);
                    if (targetConfig != null && value >= maxHunger) {
                        data.startSaturation(targetConfig.getHungerConfig().getSaturationDuration());
                        data.setLastHungerTick(0);
                    }
                    animalManager.syncToPDC(target, data);
                    sendMsg(sender, "§a已将动物饱腹度设置为 §e" + data.getHunger() + "/" + maxHunger);
                } catch (NumberFormatException e) {
                    sendMsg(sender, "§c请输入有效数字。");
                }
            }
            case "courting" -> {
                if (args.length < 3) {
                    sendMsg(sender, "§c用法: /bb admin courting <秒数>");
                    return;
                }
                try {
                    int seconds = Integer.parseInt(args[2]);
                    data.startCourting(seconds);
                    sendMsg(sender, "§a已触发动物求偶状态，持续 §e" + seconds + " §a秒。");
                } catch (NumberFormatException e) {
                    sendMsg(sender, "§c请输入有效秒数。");
                }
            }
            case "clearpregnancy" -> {
                data.clearPregnancy();
                sendMsg(sender, "§a已清除动物怀孕状态。");
            }
            case "clearcooldown" -> {
                data.clearBreedCooldown();
                sendMsg(sender, "§a已清除动物繁殖冷却。");
            }
            case "clearcourting" -> {
                data.clearCourting();
                sendMsg(sender, "§a已清除动物求偶状态。");
            }
            case "clearmating" -> {
                data.clearMating();
                sendMsg(sender, "§a已清除动物交配状态。");
            }
            default -> sendMsg(sender, "§c未知管理子命令: " + args[1]);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(List.of("menu", "reload", "give", "info", "list", "transfer", "unbind", "toggle", "help", "admin"));
            completions.removeIf(s -> !s.startsWith(args[0].toLowerCase()));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) {
                for (String name : configManager.getAnimalConfigs().keySet()) {
                    if (name.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(name);
                    }
                }
            } else if (args[0].equalsIgnoreCase("transfer")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            } else if (args[0].equalsIgnoreCase("admin")) {
                for (String sub : List.of("gender", "mood", "hunger", "courting", "clearpregnancy", "clearcooldown", "clearcourting", "clearmating")) {
                    if (sub.startsWith(args[1].toLowerCase())) {
                        completions.add(sub);
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("gender")) {
            for (String gender : List.of("male", "female")) {
                if (gender.startsWith(args[2].toLowerCase())) {
                    completions.add(gender);
                }
            }
        }
        return completions;
    }

    private void sendHelp(CommandSender sender) {
        sendMsg(sender, "§e===== BonfireBreeding Help =====");
        sender.sendMessage(LiteItem.colorText("§6玩家命令"));
        sender.sendMessage(LiteItem.colorText("§7/bb menu §f- 打开牧场菜单"));
        sender.sendMessage(LiteItem.colorText("§7/bb info §f- 查看牧场等级与经验"));
        sender.sendMessage(LiteItem.colorText("§7/bb list §f- 列出已注册动物配置"));
        sender.sendMessage(LiteItem.colorText("§7/bb transfer <玩家> §f- 转让当前看向的动物"));
        sender.sendMessage(LiteItem.colorText("§7/bb unbind §f- 解绑当前看向的动物"));
        sender.sendMessage(LiteItem.colorText("§7/bb toggle §f- 开关准星状态提示"));

        sender.sendMessage(LiteItem.colorText("§6交互说明"));
        sender.sendMessage(LiteItem.colorText("§7空手右键 §f- 仅查看信息"));
        sender.sendMessage(LiteItem.colorText("§7Shift + 空手右键 §f- 抚摸优先，并在可收获时收获"));
        sender.sendMessage(LiteItem.colorText("§7手持食物右键 §f- 喂食并提升饱腹/心情"));
        sender.sendMessage(LiteItem.colorText("§7Shift + 手持食物右键 §f- 触发求偶或繁殖"));
        sender.sendMessage(LiteItem.colorText("§7手持工具右键 §f- 执行采集类交互"));

        if (sender.hasPermission("bonfire.breeding.admin")) {
            sender.sendMessage(LiteItem.colorText("§6管理员命令"));
            sender.sendMessage(LiteItem.colorText("§7/bb reload §f- 重载配置并重新挂载已加载实体"));
            sender.sendMessage(LiteItem.colorText("§7/bb give <动物名> [玩家] §f- 发放生物包"));
            sender.sendMessage(LiteItem.colorText("§7/bb admin gender <male|female> §f- 设置性别"));
            sender.sendMessage(LiteItem.colorText("§7/bb admin mood <0-100> §f- 设置心情"));
            sender.sendMessage(LiteItem.colorText("§7/bb admin hunger <0-当前动物max_hunger> §f- 设置饱腹"));
            sender.sendMessage(LiteItem.colorText("§7/bb admin courting <秒数> §f- 触发求偶"));
            sender.sendMessage(LiteItem.colorText("§7/bb admin clearpregnancy §f- 清除怀孕"));
            sender.sendMessage(LiteItem.colorText("§7/bb admin clearcooldown §f- 清除繁殖冷却"));
            sender.sendMessage(LiteItem.colorText("§7/bb admin clearcourting §f- 清除求偶"));
            sender.sendMessage(LiteItem.colorText("§7/bb admin clearmating §f- 清除交配"));
        }
    }

    private Entity getTargetAnimal(Player player) {
        Entity target = player.getTargetEntity(5);
        if (target == null) {
            return null;
        }
        if (!(target instanceof Animals) && !(target instanceof Bee)) {
            return null;
        }
        return target;
    }

    private void sendMsg(CommandSender sender, String msg) {
        sender.sendMessage(LiteItem.colorText(configManager.getPluginPrefix().replace("%msg%", msg)));
    }

    private String joinArgs(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }
}
