package org.examplexiaodou.douafkreward;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AFKManager {
    private DouAFKReward plugin;
    private RewardManager rewardManager;
    private Map<UUID, Long> afkPlayers = new HashMap<>();
    private Map<UUID, String> playerAFKAreas = new HashMap<>();
    private Map<UUID, Long> playerAFKTime = new HashMap<>();
    private Map<UUID, Double> playerAFKMoney = new HashMap<>();
    private Map<UUID, Long> lastRewardTime = new HashMap<>();

    public AFKManager(DouAFKReward plugin, RewardManager rewardManager) {
        this.plugin = plugin;
        this.rewardManager = rewardManager;
    }

    public void processAFKRewards() {
        for (UUID playerId : afkPlayers.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }

            String areaName = playerAFKAreas.get(playerId);
            if (areaName == null) {
                continue;
            }

            long currentTime = System.currentTimeMillis();
            long lastTime = lastRewardTime.getOrDefault(playerId, currentTime);
            long timeDiff = currentTime - lastTime;

            if (timeDiff >= getRewardInterval() * 1000L) {
                double rewardAmount = getRewardAmount();
                giveReward(player, rewardAmount, areaName);
                lastRewardTime.put(playerId, currentTime);

                // 更新玩家挂机时间
                playerAFKTime.put(playerId, playerAFKTime.getOrDefault(playerId, 0L) + (timeDiff / 1000));
                playerAFKMoney.put(playerId, playerAFKMoney.getOrDefault(playerId, 0.0) + rewardAmount);

                // 更新排行榜
                plugin.getLeaderboardManager().updatePlayerAFKTime(playerId, timeDiff / 1000);
                plugin.getLeaderboardManager().updatePlayerMoney(playerId, rewardAmount);
            }
        }
    }

    /**
     * 获取全局奖励间隔（秒）
     */
    private long getRewardInterval() {
        return plugin.getConfig().getLong("reward-interval", 60);
    }

    /**
     * 获取全局基础奖励金额
     */
    private double getRewardAmount() {
        return 0.0;
    }

    private void giveReward(Player player, double amount, String areaName) {
        plugin.getRewardManager().giveAFKReward(player, areaName);
        playRewardSound(player);

        // 发送奖励消息
        String rewardMessage = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfigManager().getMessage("reward-received")
                        .replace("%amount%", String.format("%.2f", amount))
                        .replace("%area%", areaName));
        sendRewardMessage(player, rewardMessage);
    }

    private void sendRewardMessage(Player player, String message) {
        if (plugin.getConfig().getBoolean("message-settings.chat-enabled", true)) {
            player.sendMessage(message);
        }

        if (plugin.getConfig().getBoolean("message-settings.actionbar-enabled", false)) {
            try {
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
            } catch (Exception e) {
                player.sendMessage(message);
            }
        }
    }

    private void playRewardSound(Player player) {
        if (plugin.getConfig().getBoolean("sound-enabled", true)) {
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            } catch (Exception e) {
                plugin.getLogger().warning("无法播放奖励音效: " + e.getMessage());
            }
        }
    }

    public boolean isInAFKArea(Player player) {
        Location location = player.getLocation();
        ConfigurationSection areas = plugin.getConfig().getConfigurationSection("afk-areas");

        if (areas == null) {
            return false;
        }

        for (String areaName : areas.getKeys(false)) {
            if (isLocationInArea(location, areaName)) {
                return true;
            }
        }

        return false;
    }

    public String getPlayerAFKAreaName(Player player) {
        Location location = player.getLocation();
        ConfigurationSection areas = plugin.getConfig().getConfigurationSection("afk-areas");

        if (areas == null) {
            return null;
        }

        for (String areaName : areas.getKeys(false)) {
            if (isLocationInArea(location, areaName)) {
                return areaName;
            }
        }

        return null;
    }

    private boolean isLocationInArea(Location location, String areaName) {
        String path = "afk-areas." + areaName;
        String worldName = plugin.getConfig().getString(path + ".world");
        if (worldName == null || !worldName.equals(location.getWorld().getName())) {
            return false;
        }

        int minX = plugin.getConfig().getInt(path + ".minX");
        int minY = plugin.getConfig().getInt(path + ".minY");
        int minZ = plugin.getConfig().getInt(path + ".minZ");
        int maxX = plugin.getConfig().getInt(path + ".maxX");
        int maxY = plugin.getConfig().getInt(path + ".maxY");
        int maxZ = plugin.getConfig().getInt(path + ".maxZ");

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        return x >= minX && x <= maxX &&
                y >= minY && y <= maxY &&
                z >= minZ && z <= maxZ;
    }

    public boolean isAreaOverlapping(Location point1, Location point2, String excludeArea) {
        ConfigurationSection areas = plugin.getConfig().getConfigurationSection("afk-areas");
        if (areas == null) {
            return false;
        }

        int minX = Math.min(point1.getBlockX(), point2.getBlockX());
        int minY = Math.min(point1.getBlockY(), point2.getBlockY());
        int minZ = Math.min(point1.getBlockZ(), point2.getBlockZ());
        int maxX = Math.max(point1.getBlockX(), point2.getBlockX());
        int maxY = Math.max(point1.getBlockY(), point2.getBlockY());
        int maxZ = Math.max(point1.getBlockZ(), point2.getBlockZ());

        for (String areaName : areas.getKeys(false)) {
            if (areaName.equals(excludeArea)) {
                continue;
            }

            String path = "afk-areas." + areaName;
            int areaMinX = plugin.getConfig().getInt(path + ".minX");
            int areaMinY = plugin.getConfig().getInt(path + ".minY");
            int areaMinZ = plugin.getConfig().getInt(path + ".minZ");
            int areaMaxX = plugin.getConfig().getInt(path + ".maxX");
            int areaMaxY = plugin.getConfig().getInt(path + ".maxY");
            int areaMaxZ = plugin.getConfig().getInt(path + ".maxZ");

            if (minX <= areaMaxX && maxX >= areaMinX &&
                    minY <= areaMaxY && maxY >= areaMinY &&
                    minZ <= areaMaxZ && maxZ >= areaMinZ) {
                return true;
            }
        }

        return false;
    }

    public void addAFKPlayer(Player player, String areaName) {
        UUID playerId = player.getUniqueId();
        afkPlayers.put(playerId, System.currentTimeMillis());
        playerAFKAreas.put(playerId, areaName);
        lastRewardTime.put(playerId, System.currentTimeMillis());
    }

    public void removeAFKPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        afkPlayers.remove(playerId);
        playerAFKAreas.remove(playerId);
        lastRewardTime.remove(playerId);
    }

    public boolean isPlayerAFK(Player player) {
        return afkPlayers.containsKey(player.getUniqueId());
    }

    public long getPlayerAFKTime(UUID playerId) {
        return playerAFKTime.getOrDefault(playerId, 0L);
    }

    public double getPlayerAFKMoney(UUID playerId) {
        return playerAFKMoney.getOrDefault(playerId, 0.0);
    }

    public Map<UUID, Long> getAFKPlayers() {
        return new HashMap<>(afkPlayers);
    }

    /**
     * 获取全局奖励间隔（公开方法）
     */
    public long getGlobalRewardInterval() {
        return getRewardInterval();
    }

    /**
     * 获取全局基础奖励金额（公开方法）
     */
    public double getGlobalBaseReward() {
        return getRewardAmount();
    }
}