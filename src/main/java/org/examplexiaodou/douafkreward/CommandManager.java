package org.examplexiaodou.douafkreward;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CommandManager {
    private DouAFKReward plugin;
    private ConfigManager configManager;
    private SelectionManager selectionManager;
    private AFKManager afkManager;
    private RewardManager rewardManager;
    private LeaderboardManager leaderboardManager;

    public CommandManager(DouAFKReward plugin, SelectionManager selectionManager, AFKManager afkManager, RewardManager rewardManager, LeaderboardManager leaderboardManager) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.selectionManager = selectionManager;
        this.afkManager = afkManager;
        this.rewardManager = rewardManager;
        this.leaderboardManager = leaderboardManager;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (sender.hasPermission("douafkreward.help") || sender.hasPermission("douafkreward.*")) {
                sendHelp(sender);
            } else {
                sender.sendMessage(configManager.getMessage("no-permission"));
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give":
                if (sender.hasPermission("douafkreward.give") || sender.hasPermission("douafkreward.*")) {
                    giveSelectionTool(sender, args);
                } else {
                    sender.sendMessage(configManager.getMessage("no-permission"));
                }
                break;

            case "create":
                if (args.length > 1 && args[1].equalsIgnoreCase("top")) {
                    if (sender.hasPermission("douafkreward.create.top") || sender.hasPermission("douafkreward.*")) {
                        createLeaderboard(sender, args);
                    } else {
                        sender.sendMessage(configManager.getMessage("no-permission"));
                    }
                } else {
                    if (sender.hasPermission("douafkreward.create") || sender.hasPermission("douafkreward.*")) {
                        createAFKArea(sender, args);
                    } else {
                        sender.sendMessage(configManager.getMessage("no-permission"));
                    }
                }
                break;

            case "remove":
                if (sender.hasPermission("douafkreward.remove") || sender.hasPermission("douafkreward.*")) {
                    removeAFKArea(sender, args);
                } else {
                    sender.sendMessage(configManager.getMessage("no-permission"));
                }
                break;

            case "reload":
                if (sender.hasPermission("douafkreward.reload") || sender.hasPermission("douafkreward.*")) {
                    reloadConfig(sender);
                } else {
                    sender.sendMessage(configManager.getMessage("no-permission"));
                }
                break;

            case "list":
                if (sender.hasPermission("douafkreward.list") || sender.hasPermission("douafkreward.*")) {
                    listAFKAreas(sender);
                } else {
                    sender.sendMessage(configManager.getMessage("no-permission"));
                }
                break;

            case "tp":
                if (sender.hasPermission("douafkreward.tp") || sender.hasPermission("douafkreward.*")) {
                    teleportToArea(sender, args);
                } else {
                    sender.sendMessage(configManager.getMessage("no-permission"));
                }
                break;

            case "top":
                if (sender.hasPermission("douafkreward.top") || sender.hasPermission("douafkreward.*")) {
                    showLeaderboard(sender, args);
                } else {
                    sender.sendMessage(configManager.getMessage("no-permission"));
                }
                break;

            case "help":
                if (sender.hasPermission("douafkreward.help") || sender.hasPermission("douafkreward.*")) {
                    sendHelp(sender);
                } else {
                    sender.sendMessage(configManager.getMessage("no-permission"));
                }
                break;

            default:
                if (sender.hasPermission("douafkreward.help") || sender.hasPermission("douafkreward.*")) {
                    sendHelp(sender);
                } else {
                    sender.sendMessage(configManager.getMessage("no-permission"));
                }
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getMessage("help-header")));

        if (sender.hasPermission("douafkreward.give") || sender.hasPermission("douafkreward.*")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getMessage("help-give")));
        }
        if (sender.hasPermission("douafkreward.create") || sender.hasPermission("douafkreward.*")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getMessage("help-create")));
        }
        if (sender.hasPermission("douafkreward.remove") || sender.hasPermission("douafkreward.*")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getMessage("help-remove")));
        }
        if (sender.hasPermission("douafkreward.create.top") || sender.hasPermission("douafkreward.*")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getMessage("help-create-top")));
        }
        if (sender.hasPermission("douafkreward.list") || sender.hasPermission("douafkreward.*")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getMessage("help-list")));
        }
        if (sender.hasPermission("douafkreward.reload") || sender.hasPermission("douafkreward.*")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getMessage("help-reload")));
        }
        if (sender.hasPermission("douafkreward.tp") || sender.hasPermission("douafkreward.*")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getMessage("help-tp")));
        }
        if (sender.hasPermission("douafkreward.top") || sender.hasPermission("douafkreward.*")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getMessage("help-top")));
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getMessage("help-help")));
    }

    private void giveSelectionTool(CommandSender sender, String[] args) {
        Player targetPlayer;

        if (args.length > 1) {
            if (!sender.hasPermission("douafkreward.give.others") && !sender.hasPermission("douafkreward.*")) {
                sender.sendMessage(configManager.getMessage("no-permission"));
                return;
            }

            targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                sender.sendMessage(configManager.getMessage("player-not-found"));
                return;
            }
        } else if (sender instanceof Player) {
            targetPlayer = (Player) sender;
        } else {
            sender.sendMessage(configManager.getMessage("player-only"));
            return;
        }

        ItemStack tool = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = tool.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "挂机区域选择工具");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "左键点击方块设置第一个点",
                ChatColor.GRAY + "右键点击方块设置第二个点",
                ChatColor.GRAY + "然后使用/dfd create <区域名称> 创建区域"
        ));
        tool.setItemMeta(meta);

        targetPlayer.getInventory().addItem(tool);
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                configManager.getMessage("tool-given").replace("%player%", targetPlayer.getName())));
    }

    private void createAFKArea(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.getMessage("player-only"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(configManager.getMessage("usage").replace("%usage%", "/dfd create <区域名称>"));
            return;
        }

        Player player = (Player) sender;
        String areaName = args[1];

        if (plugin.getConfig().contains("afk-areas." + areaName)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    configManager.getMessage("area-exists").replace("%name%", areaName)));
            return;
        }

        Location[] points = selectionManager.getSelection(player);
        if (points == null || points[0] == null || points[1] == null) {
            sender.sendMessage(configManager.getMessage("tool-no-points"));
            return;
        }

        if (!points[0].getWorld().getName().equals(points[1].getWorld().getName())) {
            sender.sendMessage(configManager.getMessage("tool-different-worlds"));
            return;
        }

        if (afkManager.isAreaOverlapping(points[0], points[1], null)) {
            sender.sendMessage(ChatColor.RED + "该区域与现有挂机区域重叠，请选择其他位置!");
            return;
        }

        FileConfiguration config = plugin.getConfig();
        String path = "afk-areas." + areaName;

        config.set(path + ".world", points[0].getWorld().getName());
        config.set(path + ".minX", Math.min(points[0].getBlockX(), points[1].getBlockX()));
        config.set(path + ".minY", Math.min(points[0].getBlockY(), points[1].getBlockY()));
        config.set(path + ".minZ", Math.min(points[0].getBlockZ(), points[1].getBlockZ()));
        config.set(path + ".maxX", Math.max(points[0].getBlockX(), points[1].getBlockX()));
        config.set(path + ".maxY", Math.max(points[0].getBlockY(), points[1].getBlockY()));
        config.set(path + ".maxZ", Math.max(points[0].getBlockZ(), points[1].getBlockZ()));

        // 保存配置但不重载（避免注释丢失）
        try {
            plugin.saveConfig();
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    configManager.getMessage("area-created").replace("%name%", areaName)));
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "保存配置时出错: " + e.getMessage());
            plugin.getLogger().warning("保存配置时出错: " + e.getMessage());
        }

        // 立即停止粒子效果
        selectionManager.clearSelection(player);
    }

    private void removeAFKArea(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(configManager.getMessage("usage").replace("%usage%", "/dfd remove <区域名称>"));
            return;
        }

        String areaName = args[1];
        FileConfiguration config = plugin.getConfig();

        if (!config.contains("afk-areas." + areaName)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    configManager.getMessage("area-not-found").replace("%name%", areaName)));
            return;
        }

        config.set("afk-areas." + areaName, null);

        // 保存配置但不重载（避免注释丢失）
        try {
            plugin.saveConfig();
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    configManager.getMessage("area-removed").replace("%name%", areaName)));
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "保存配置时出错: " + e.getMessage());
            plugin.getLogger().warning("保存配置时出错: " + e.getMessage());
        }
    }

    private void reloadConfig(CommandSender sender) {
        configManager.reloadConfigs();
        sender.sendMessage(configManager.getMessage("config-reloaded"));
    }

    private void listAFKAreas(CommandSender sender) {
        FileConfiguration config = plugin.getConfig();

        // 列出普通挂机区域
        if (config.contains("afk-areas")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getMessage("area-list-header")));
            for (String areaName : config.getConfigurationSection("afk-areas").getKeys(false)) {
                String path = "afk-areas." + areaName;
                String message = configManager.getMessage("area-list-item")
                        .replace("%name%", areaName)
                        .replace("%world%", config.getString(path + ".world"))
                        .replace("%minX%", String.valueOf(config.getInt(path + ".minX")))
                        .replace("%minY%", String.valueOf(config.getInt(path + ".minY")))
                        .replace("%minZ%", String.valueOf(config.getInt(path + ".minZ")))
                        .replace("%maxX%", String.valueOf(config.getInt(path + ".maxX")))
                        .replace("%maxY%", String.valueOf(config.getInt(path + ".maxY")))
                        .replace("%maxZ%", String.valueOf(config.getInt(path + ".maxZ")));

                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            }

            // 添加全局奖励信息
            long interval = config.getLong("reward-interval", 60);
            boolean useRandom = config.getBoolean("use-random-reward", true);

            if (useRandom) {
                ConfigurationSection baseReward = config.getConfigurationSection("base-reward");
                if (baseReward != null) {
                    double min = baseReward.getDouble("min", 5.0);
                    double max = baseReward.getDouble("max", 15.0);
                    sender.sendMessage(ChatColor.GREEN + "全局奖励设置: " +
                            ChatColor.YELLOW + "每 " + interval + " 秒奖励 " +
                            String.format("%.1f", min) + " - " + String.format("%.1f", max) + " 金币");
                }
            } else {
                double reward = config.getDouble("base-reward", 10.0);
                sender.sendMessage(ChatColor.GREEN + "全局奖励设置: " +
                        ChatColor.YELLOW + "每 " + interval + " 秒奖励 " + reward + " 金币");
            }
        }

        if (!config.contains("afk-areas")) {
            sender.sendMessage(configManager.getMessage("no-areas"));
        }
    }

    private void teleportToArea(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.getMessage("player-only"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(configManager.getMessage("usage").replace("%usage%", "/dfd tp <区域名称>"));
            return;
        }

        Player player = (Player) sender;
        String areaName = args[1];

        FileConfiguration config = plugin.getConfig();
        Location teleportLocation = null;

        // 检查普通挂机区域
        if (config.contains("afk-areas." + areaName)) {
            String path = "afk-areas." + areaName;
            String worldName = config.getString(path + ".world");
            int minX = config.getInt(path + ".minX");
            int minY = config.getInt(path + ".minY");
            int minZ = config.getInt(path + ".minZ");
            int maxX = config.getInt(path + ".maxX");
            int maxY = config.getInt(path + ".maxY");
            int maxZ = config.getInt(path + ".maxZ");

            int centerX = (minX + maxX) / 2;
            int centerY = (minY + maxY) / 2;
            int centerZ = (minZ + maxZ) / 2;

            teleportLocation = new Location(Bukkit.getWorld(worldName), centerX + 0.5, centerY, centerZ + 0.5);
        }

        if (teleportLocation == null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    configManager.getMessage("area-not-found").replace("%name%", areaName)));
            return;
        }

        player.teleport(teleportLocation);
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                configManager.getMessage("area-teleported").replace("%name%", areaName)));
    }

    private void showLeaderboard(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(configManager.getMessage("usage").replace("%usage%", "/dfd top <time/money>"));
            return;
        }

        String type = args[1].toLowerCase();
        if (!type.equals("time") && !type.equals("money")) {
            sender.sendMessage(configManager.getMessage("usage").replace("%usage%", "/dfd top <time/money>"));
            return;
        }

        if (type.equals("time")) {
            showTimeLeaderboard(sender);
        } else {
            showMoneyLeaderboard(sender);
        }
    }

    private void showTimeLeaderboard(CommandSender sender) {
        List<Map.Entry<UUID, Long>> topList = leaderboardManager.getTopAFKTime(10);

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                configManager.getTopFormat("time", "header")));

        if (topList.isEmpty()) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    configManager.getTopFormat("time", "no-data")));
            return;
        }

        int rank = 1;
        for (Map.Entry<UUID, Long> entry : topList) {
            String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (playerName == null) continue;

            long totalSeconds = entry.getValue();
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;

            String line = configManager.getTopFormat("time", "line")
                    .replace("%rank%", String.valueOf(rank))
                    .replace("%player%", playerName)
                    .replace("%hours%", String.valueOf(hours))
                    .replace("%minutes%", String.valueOf(minutes))
                    .replace("%time%", String.format(configManager.getTopFormat("time", "time-format"), hours, minutes));

            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
            rank++;
        }
    }

    private void showMoneyLeaderboard(CommandSender sender) {
        List<Map.Entry<UUID, Double>> topList = leaderboardManager.getTopMoney(10);

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                configManager.getTopFormat("money", "header")));

        if (topList.isEmpty()) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    configManager.getTopFormat("money", "no-data")));
            return;
        }

        int rank = 1;
        for (Map.Entry<UUID, Double> entry : topList) {
            String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (playerName == null) continue;

            double money = entry.getValue();
            String moneyFormat = configManager.getTopFormat("money", "money-format");
            String formattedMoney = String.format(moneyFormat, money);

            String line = configManager.getTopFormat("money", "line")
                    .replace("%rank%", String.valueOf(rank))
                    .replace("%player%", playerName)
                    .replace("%money%", formattedMoney);

            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
            rank++;
        }
    }

    private void createLeaderboard(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.getMessage("player-only"));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(configManager.getMessage("usage").replace("%usage%", "/dfd create top <time/money>"));
            return;
        }

        Player player = (Player) sender;
        String type = args[2].toLowerCase();

        if (!type.equals("time") && !type.equals("money")) {
            sender.sendMessage(configManager.getMessage("usage").replace("%usage%", "/dfd create top <time/money>"));
            return;
        }

        leaderboardManager.setLeaderboardLocation(type, player.getLocation());
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                configManager.getMessage("top-created").replace("%type%", type)));
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("douafkreward.give") || sender.hasPermission("douafkreward.*")) {
                completions.add("give");
            }
            if (sender.hasPermission("douafkreward.create") || sender.hasPermission("douafkreward.*")) {
                completions.add("create");
            }
            if (sender.hasPermission("douafkreward.remove") || sender.hasPermission("douafkreward.*")) {
                completions.add("remove");
            }
            if (sender.hasPermission("douafkreward.list") || sender.hasPermission("douafkreward.*")) {
                completions.add("list");
            }
            if (sender.hasPermission("douafkreward.reload") || sender.hasPermission("douafkreward.*")) {
                completions.add("reload");
            }
            if (sender.hasPermission("douafkreward.tp") || sender.hasPermission("douafkreward.*")) {
                completions.add("tp");
            }
            if (sender.hasPermission("douafkreward.top") || sender.hasPermission("douafkreward.*")) {
                completions.add("top");
            }
            completions.add("help");
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "give":
                    if (sender.hasPermission("douafkreward.give.others") || sender.hasPermission("douafkreward.*")) {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            completions.add(player.getName());
                        }
                    }
                    break;

                case "create":
                    completions.add("<区域名称>");
                    break;

                case "remove":
                case "tp":
                    if (plugin.getConfig().contains("afk-areas")) {
                        completions.addAll(plugin.getConfig().getConfigurationSection("afk-areas").getKeys(false));
                    }
                    break;

                case "top":
                    completions.add("time");
                    completions.add("money");
                    break;
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("create") && args[1].equalsIgnoreCase("top")) {
                completions.add("time");
                completions.add("money");
            }
        }

        String partial = args[args.length - 1].toLowerCase();
        List<String> filtered = new ArrayList<>();
        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(partial)) {
                filtered.add(completion);
            }
        }

        return filtered.isEmpty() ? completions : filtered;
    }
}