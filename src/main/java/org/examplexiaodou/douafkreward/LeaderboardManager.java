package org.examplexiaodou.douafkreward;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class LeaderboardManager {
    private DouAFKReward plugin;
    private Map<UUID, PlayerStats> playerStats = new HashMap<>();
    private Map<String, Location> leaderboardLocations = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    public LeaderboardManager(DouAFKReward plugin) {
        this.plugin = plugin;
        loadData();
    }

    public void addAFKTime(UUID playerId, long seconds) {
        PlayerStats stats = playerStats.computeIfAbsent(playerId, k -> new PlayerStats());
        stats.totalAFKTime += seconds;
        stats.dailyAFKTime += seconds;
    }

    public void addAFKMoney(UUID playerId, double amount) {
        PlayerStats stats = playerStats.computeIfAbsent(playerId, k -> new PlayerStats());
        stats.totalMoney += amount;
        stats.dailyMoney += amount;
    }

    public long getTotalAFKTime(UUID playerId) {
        PlayerStats stats = playerStats.get(playerId);
        return stats != null ? stats.totalAFKTime : 0;
    }

    public double getTotalMoney(UUID playerId) {
        PlayerStats stats = playerStats.get(playerId);
        return stats != null ? stats.totalMoney : 0;
    }

    public double getDailyMoney(UUID playerId) {
        PlayerStats stats = playerStats.get(playerId);
        return stats != null ? stats.dailyMoney : 0;
    }

    public void resetDailyStats() {
        for (PlayerStats stats : playerStats.values()) {
            stats.dailyAFKTime = 0;
            stats.dailyMoney = 0;
        }
        saveData();
    }

    public List<Map.Entry<UUID, Long>> getTopAFKTime(int limit) {
        List<Map.Entry<UUID, Long>> result = new ArrayList<>();
        for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
            result.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().totalAFKTime));
        }
        result.sort((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()));
        return result.stream().limit(limit).collect(Collectors.toList());
    }

    public List<Map.Entry<UUID, Double>> getTopMoney(int limit) {
        List<Map.Entry<UUID, Double>> result = new ArrayList<>();
        for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
            result.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().totalMoney));
        }
        result.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
        return result.stream().limit(limit).collect(Collectors.toList());
    }

    public void setLeaderboardLocation(String type, Location location) {
        leaderboardLocations.put(type, location);
        saveData();
    }

    public Location getLeaderboardLocation(String type) {
        return leaderboardLocations.get(type);
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("创建数据文件时出错: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // 加载玩家数据
        if (dataConfig.contains("players")) {
            for (String key : dataConfig.getConfigurationSection("players").getKeys(false)) {
                UUID playerId = UUID.fromString(key);
                PlayerStats stats = new PlayerStats();
                stats.totalAFKTime = dataConfig.getLong("players." + key + ".totalAFKTime");
                stats.totalMoney = dataConfig.getDouble("players." + key + ".totalMoney");
                playerStats.put(playerId, stats);
            }
        }

        // 加载排行榜位置
        if (dataConfig.contains("leaderboards")) {
            for (String type : dataConfig.getConfigurationSection("leaderboards").getKeys(false)) {
                Location loc = (Location) dataConfig.get("leaderboards." + type);
                leaderboardLocations.put(type, loc);
            }
        }
    }

    public void saveData() {
        try {
            // 保存玩家数据
            for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
                String path = "players." + entry.getKey().toString();
                dataConfig.set(path + ".totalAFKTime", entry.getValue().totalAFKTime);
                dataConfig.set(path + ".totalMoney", entry.getValue().totalMoney);
            }

            // 保存排行榜位置
            for (Map.Entry<String, Location> entry : leaderboardLocations.entrySet()) {
                dataConfig.set("leaderboards." + entry.getKey(), entry.getValue());
            }

            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("保存数据文件时出错: " + e.getMessage());
        }
    }

    private static class PlayerStats {
        public long totalAFKTime = 0;
        public long dailyAFKTime = 0;
        public double totalMoney = 0;
        public double dailyMoney = 0;
    }
    public void updatePlayerAFKTime(UUID playerId, long seconds) {
        addAFKTime(playerId, seconds);
    }

    public void updatePlayerMoney(UUID playerId, double amount) {
        addAFKMoney(playerId, amount);
    }

}
