package org.examplexiaodou.douafkreward;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.lang.reflect.Method;
import java.util.concurrent.ThreadLocalRandom;

public class RewardManager {
    private DouAFKReward plugin;
    private Object economy;
    private boolean hasVault = false;
    private Map<UUID, Double> playerBalances = new HashMap<>();
    private ThreadLocalRandom random = ThreadLocalRandom.current();

    public RewardManager(DouAFKReward plugin) {
        this.plugin = plugin;

        // 检查 Vault 是否存在
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") != null) {
            try {
                Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
                economy = Bukkit.getServicesManager().getRegistration(economyClass).getProvider();
                hasVault = true;
                plugin.getLogger().info("已连接到 Vault 经济系统");
            } catch (Exception e) {
                plugin.getLogger().warning("无法连接到 Vault 经济系统: " + e.getMessage());
                hasVault = false;
            }
        } else {
            plugin.getLogger().warning("Vault 插件未找到，将使用内置经济系统");
            hasVault = false;
        }
    }

    /**
     * 计算玩家应得的奖励金额（基础奖励 + 权限组额外奖励）
     */
    public double calculateReward(Player player) {
        double baseReward = getBaseReward();
        double groupBonus = getGroupBonus(player);

        return baseReward + groupBonus;
    }

    /**
     * 获取基础随机奖励
     */
    private double getBaseReward() {
        ConfigurationSection baseRewardSection = plugin.getConfigManager().getMainConfig().getConfigurationSection("base-reward");
        boolean useRandom = plugin.getConfigManager().getMainConfig().getBoolean("use-random-reward", true);

        if (useRandom && baseRewardSection != null) {
            double min = baseRewardSection.getDouble("min", 5.0);
            double max = baseRewardSection.getDouble("max", 15.0);
            return min + (max - min) * random.nextDouble();
        } else {
            // 兼容旧配置
            return plugin.getConfigManager().getMainConfig().getDouble("base-reward", 10.0);
        }
    }

    /**
     * 获取权限组额外随机奖励
     */
    private double getGroupBonus(Player player) {
        ConfigurationSection groupSection = plugin.getConfigManager().getMainConfig().getConfigurationSection("group-bonus");

        if (groupSection == null) return 0.0;

        // 获取玩家所有的权限组
        Set<String> playerGroups = getAllPlayerGroups(player);

        // 查找最高额外奖励的权限组
        double highestBonus = 0.0;

        for (String group : playerGroups) {
            if (groupSection.contains(group)) {
                ConfigurationSection groupBonusSection = groupSection.getConfigurationSection(group);
                if (groupBonusSection != null) {
                    double min = groupBonusSection.getDouble("min", 0.0);
                    double max = groupBonusSection.getDouble("max", 0.0);
                    double bonus = min + (max - min) * random.nextDouble();
                    if (bonus > highestBonus) {
                        highestBonus = bonus;
                    }
                }
            }
        }

        return highestBonus;
    }

    /**
     * 给予玩家挂机奖励
     */
    public void giveAFKReward(Player player, String areaName) {
        double finalReward = calculateReward(player);

        // 给予奖励
        giveReward(player, finalReward);

        // 发送奖励消息，使用 areaName
        String rewardMessage = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfigManager().getMessage("reward-received")
                        .replace("%amount%", String.format("%.2f", finalReward))
                        .replace("%area%", areaName)); // 添加区域名称替换

        player.sendMessage(rewardMessage);
    }

    /**
     * 获取玩家所有的权限组
     */
    private Set<String> getAllPlayerGroups(Player player) {
        Set<String> groups = new HashSet<>();
        groups.add("default"); // 默认组

        // 检查 LuckPerms 是否存在
        if (Bukkit.getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                Class<?> luckPermsClass = Class.forName("net.luckperms.api.LuckPerms");
                Class<?> userManagerClass = Class.forName("net.luckperms.api.model.user.UserManager");
                Class<?> userClass = Class.forName("net.luckperms.api.model.user.User");

                Object luckPerms = Bukkit.getServicesManager().getRegistration(luckPermsClass).getProvider();
                Object userManager = userManagerClass.cast(luckPermsClass.getMethod("getUserManager").invoke(luckPerms));
                Object user = userClass.cast(userManagerClass.getMethod("getUser", UUID.class).invoke(userManager, player.getUniqueId()));

                if (user != null) {
                    // 获取主要权限组
                    String primaryGroup = (String) userClass.getMethod("getPrimaryGroup").invoke(user);
                    groups.add(primaryGroup);

                    // 获取所有权限组节点
                    Collection<?> nodes = (Collection<?>) userClass.getMethod("getNodes").invoke(user);

                    for (Object node : nodes) {
                        String permission = (String) node.getClass().getMethod("getKey").invoke(node);
                        if (permission.startsWith("group.")) {
                            String groupName = permission.substring(6); // 移除 "group." 前缀
                            groups.add(groupName);
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("获取玩家权限组时出错: " + e.getMessage());
                // 回退到权限检测方式
                addGroupsFromPermissions(player, groups);
            }
        } else {
            // 如果没有 LuckPerms，使用权限检测方式
            addGroupsFromPermissions(player, groups);
        }

        return groups;
    }

    /**
     * 通过权限节点检测玩家权限组（备用方法）
     */
    private void addGroupsFromPermissions(Player player, Set<String> groups) {
        // 检测常见权限组
        String[] commonGroups = {"vip", "mvip", "svip", "admin", "owner", "mod", "helper", "builder"};

        for (String group : commonGroups) {
            if (player.hasPermission("group." + group) || player.hasPermission(group)) {
                groups.add(group);
            }
        }
    }

    /**
     * 给予玩家金币奖励
     */
    public void giveReward(Player player, double amount) {
        UUID playerId = player.getUniqueId();

        if (hasVault) {
            try {
                Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
                // 使用反射调用存款方法
                try {
                    economyClass.getMethod("depositPlayer", Player.class, double.class).invoke(economy, player, amount);
                } catch (NoSuchMethodException e) {
                    try {
                        economyClass.getMethod("depositPlayer", OfflinePlayer.class, double.class).invoke(economy, player, amount);
                    } catch (NoSuchMethodException e2) {
                        economyClass.getMethod("depositPlayer", String.class, double.class).invoke(economy, player.getName(), amount);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("发放奖励时出错: " + e.getMessage());
                // 回退到内置经济系统
                double currentBalance = playerBalances.getOrDefault(playerId, 0.0);
                playerBalances.put(playerId, currentBalance + amount);
            }
        } else {
            // 使用内置经济系统
            double currentBalance = playerBalances.getOrDefault(playerId, 0.0);
            playerBalances.put(playerId, currentBalance + amount);
        }

        // 添加到排行榜
        plugin.getLeaderboardManager().addAFKMoney(playerId, amount);
    }

    /**
     * 获取玩家余额
     */
    public double getBalance(Player player) {
        if (hasVault) {
            try {
                Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
                try {
                    return (Double) economyClass.getMethod("getBalance", Player.class).invoke(economy, player);
                } catch (NoSuchMethodException e) {
                    try {
                        return (Double) economyClass.getMethod("getBalance", OfflinePlayer.class).invoke(economy, player);
                    } catch (NoSuchMethodException e2) {
                        return (Double) economyClass.getMethod("getBalance", String.class).invoke(economy, player.getName());
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("获取玩家余额时出错: " + e.getMessage());
                return 0.0;
            }
        } else {
            return playerBalances.getOrDefault(player.getUniqueId(), 0.0);
        }
    }

    /**
     * 设置玩家余额
     */
    public boolean setBalance(Player player, double amount) {
        if (amount < 0) return false;

        if (hasVault) {
            try {
                Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");

                double currentBalance;
                try {
                    currentBalance = (Double) economyClass.getMethod("getBalance", Player.class).invoke(economy, player);
                } catch (NoSuchMethodException e) {
                    try {
                        currentBalance = (Double) economyClass.getMethod("getBalance", OfflinePlayer.class).invoke(economy, player);
                    } catch (NoSuchMethodException e2) {
                        currentBalance = (Double) economyClass.getMethod("getBalance", String.class).invoke(economy, player.getName());
                    }
                }

                double difference = amount - currentBalance;

                if (difference > 0) {
                    // 存款
                    try {
                        economyClass.getMethod("depositPlayer", Player.class, double.class).invoke(economy, player, difference);
                    } catch (NoSuchMethodException e) {
                        try {
                            economyClass.getMethod("depositPlayer", OfflinePlayer.class, double.class).invoke(economy, player, difference);
                        } catch (NoSuchMethodException e2) {
                            economyClass.getMethod("depositPlayer", String.class, double.class).invoke(economy, player.getName(), difference);
                        }
                    }
                } else if (difference < 0) {
                    // 取款
                    try {
                        economyClass.getMethod("withdrawPlayer", Player.class, double.class).invoke(economy, player, -difference);
                    } catch (NoSuchMethodException e) {
                        try {
                            economyClass.getMethod("withdrawPlayer", OfflinePlayer.class, double.class).invoke(economy, player, -difference);
                        } catch (NoSuchMethodException e2) {
                            economyClass.getMethod("withdrawPlayer", String.class, double.class).invoke(economy, player.getName(), -difference);
                        }
                    }
                }
                return true;
            } catch (Exception e) {
                plugin.getLogger().warning("设置玩家余额时出错: " + e.getMessage());
                return false;
            }
        } else {
            playerBalances.put(player.getUniqueId(), amount);
            return true;
        }
    }

    public boolean hasVault() {
        return hasVault;
    }
    public boolean hasLuckPerms() {
        return Bukkit.getServer().getPluginManager().getPlugin("LuckPerms") != null;
    }
}