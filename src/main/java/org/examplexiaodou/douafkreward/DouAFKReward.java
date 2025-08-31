package org.examplexiaodou.douafkreward;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DouAFKReward extends JavaPlugin {
    private ConfigManager configManager;
    private SelectionManager selectionManager;
    private AFKManager afkManager;
    private RewardManager rewardManager;
    private CommandManager commandManager;
    private LeaderboardManager leaderboardManager;
    private String latestVersion = null;
    private boolean updateAvailable = false;
    private Set<UUID> notifiedPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        try {
            // 初始化配置管理器
            configManager = new ConfigManager(this);
            getLogger().info("配置管理器初始化完成");

            // 检查配置更新
            configManager.checkConfigUpdates();

            // 调试：打印配置信息
           // configManager.debugMessages();

            // 检查重要消息是否存在
            checkEssentialMessages();

            // 初始化管理器
            selectionManager = new SelectionManager(this);
            rewardManager = new RewardManager(this);
            afkManager = new AFKManager(this, rewardManager);
            leaderboardManager = new LeaderboardManager(this);
            commandManager = new CommandManager(this, selectionManager, afkManager, rewardManager, leaderboardManager);
            getLogger().info("所有管理器初始化完成");

            // 注册事件监听器
            getServer().getPluginManager().registerEvents(new EventListener(this, selectionManager, afkManager), this);
            getLogger().info("事件监听器注册完成");

            // 启动奖励计时任务
            startRewardTask();
            getLogger().info("奖励任务启动完成");

            // 启动排行榜保存任务
            startLeaderboardSaveTask();
            getLogger().info("排行榜保存任务启动完成");

            // 启动版本检查任务
            startVersionCheckTask();
            getLogger().info("版本检查任务启动完成");

            // 注册PAPI扩展
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                PAPIExpansion expansion = new PAPIExpansion(this, afkManager, rewardManager, leaderboardManager);
                expansion.register();
                getLogger().info("已准备 PlaceholderAPI 扩展");
            }

            // 使用带颜色的控制台消息
            sendColoredConsoleMessage("&aDouAFKReward 插件已启用!");
            sendColoredConsoleMessage("&6版本: &e" + getDescription().getVersion());
            sendColoredConsoleMessage("&a插件作者：小豆同学");
            sendColoredConsoleMessage("&a插件定制QQ:2321388357");

            // 检查依赖
            if (!rewardManager.hasVault()) {
                sendColoredConsoleMessage("&c警告: Vault 未找到，将使用内置经济系统");
            } else {
                sendColoredConsoleMessage("&a已连接到 Vault 经济系统");
            }

            if (Bukkit.getServer().getPluginManager().getPlugin("LuckPerms") == null) {
                sendColoredConsoleMessage("&c警告: LuckPerms 未找到，将使用默认权限组设置");
            } else {
                sendColoredConsoleMessage("&a已连接到 LuckPerms 权限系统");
            }

        } catch (Exception e) {
            sendColoredConsoleMessage("&c插件启动失败: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // 保存排行榜数据
        if (leaderboardManager != null) {
            leaderboardManager.saveData();
        }

        sendColoredConsoleMessage("&cDouAFKReward 插件已禁用!");
    }

    private void checkEssentialMessages() {
        String[] essentialMessages = {
                "no-permission",
                "player-only",
                "usage",
                "player-not-found",
                "invalid-amount"
        };

        for (String message : essentialMessages) {
            if (!configManager.hasMessage(message)) {
                sendColoredConsoleMessage("&c警告: 重要消息未配置: " + message);
            } else {
                sendColoredConsoleMessage("&a消息配置正常: " + message);
            }
        }
    }

    private void startRewardTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    afkManager.processAFKRewards();
                } catch (Exception e) {
                    sendColoredConsoleMessage("&c处理AFK奖励时出错: " + e.getMessage());
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    private void startLeaderboardSaveTask() {
        int interval = getConfig().getInt("leaderboards.save-interval", 5) * 60 * 20;
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    leaderboardManager.saveData();
                } catch (Exception e) {
                    sendColoredConsoleMessage("&c保存排行榜数据时出错: " + e.getMessage());
                }
            }
        }.runTaskTimer(this, interval, interval);
    }

    private void startVersionCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkForUpdates();
            }
        }.runTaskLaterAsynchronously(this, 100L);
    }

    private void checkForUpdates() {
        try {
            URL url = new URL("https://api.github.com/repos/yourname/DouAFKReward/releases/latest");
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "DouAFKReward-Plugin");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("\"tag_name\"")) {
                        latestVersion = line.split(":")[1].replace("\"", "").trim().replace(",", "");
                        break;
                    }
                }

                if (latestVersion != null && !getDescription().getVersion().equals(latestVersion)) {
                    updateAvailable = true;
                    sendColoredConsoleMessage("&e有新版本可用! 当前版本: &c" + getDescription().getVersion() +
                            "&e, 最新版本: &a" + latestVersion);
                    sendColoredConsoleMessage("&e请前往下载更新: &bhttps://github.com/yourname/DouAFKReward/releases");

                    notifyOnlineOPPlayers();
                } else {
                    sendColoredConsoleMessage("&a插件已是最新版本");
                }
            }
        } catch (Exception e) {
            sendColoredConsoleMessage("&c检查更新时出错: " + e.getMessage());
        }
    }

    private void notifyOnlineOPPlayers() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if ((player.isOp() || player.hasPermission("douafkreward.*")) &&
                            !notifiedPlayers.contains(player.getUniqueId())) {
                        player.sendMessage("§e§m==============================================");
                        player.sendMessage("§6[DouAFKReward] §e有新版本可用!");
                        player.sendMessage("§6当前版本: §c" + getDescription().getVersion());
                        player.sendMessage("§6最新版本: §a" + latestVersion);
                        player.sendMessage("§6下载地址: §bhttps://www.spigotmc.org/resources/YOUR_RESOURCE_ID/");
                        player.sendMessage("§e§m==============================================");
                        notifiedPlayers.add(player.getUniqueId());
                    }
                }
            }
        }.runTask(this);
    }

    public void onPlayerJoin(Player player) {
        if (updateAvailable && (player.isOp() || player.hasPermission("douafkreward.*")) &&
                !notifiedPlayers.contains(player.getUniqueId())) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.sendMessage("§e§m==============================================");
                    player.sendMessage("§6[DouAFKReward] §e有新版本可用!");
                    player.sendMessage("§6当前版本: §c" + getDescription().getVersion());
                    player.sendMessage("§6最新版本: §a" + latestVersion);
                    player.sendMessage("§6下载地址: §bhttps://www.spigotmc.org/resources/YOUR_RESOURCE_ID/");
                    player.sendMessage("§e§m==============================================");
                    notifiedPlayers.add(player.getUniqueId());
                }
            }.runTaskLater(this, 40L);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        try {
            return commandManager.onCommand(sender, cmd, label, args);
        } catch (Exception e) {
            sendColoredConsoleMessage("&c执行命令时出错: " + e.getMessage());
            sender.sendMessage("§c命令执行出错，请查看服务器日志");
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return commandManager.onTabComplete(sender, command, alias, args);
    }

    private void sendColoredConsoleMessage(String message) {
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
        getLogger().info(coloredMessage);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public AFKManager getAFKManager() {
        return afkManager;
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public SelectionManager getSelectionManager() {
        return selectionManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}