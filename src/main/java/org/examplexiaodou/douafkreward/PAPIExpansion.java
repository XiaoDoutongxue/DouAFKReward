package org.examplexiaodou.douafkreward;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

public class PAPIExpansion {
    private DouAFKReward plugin;
    private AFKManager afkManager;
    private RewardManager rewardManager;
    private LeaderboardManager leaderboardManager;

    public PAPIExpansion(DouAFKReward plugin, AFKManager afkManager, RewardManager rewardManager, LeaderboardManager leaderboardManager) {
        this.plugin = plugin;
        this.afkManager = afkManager;
        this.rewardManager = rewardManager;
        this.leaderboardManager = leaderboardManager;
    }

    public String getIdentifier() {
        return "douafk";
    }

    public String getAuthor() {
        return "ExampleXiaoDou";
    }

    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";

        UUID playerId = player.getUniqueId();
        String[] args = params.split("_");

        switch (args[0].toLowerCase()) {
            case "time":
                long seconds = leaderboardManager.getTotalAFKTime(playerId);
                return formatTime(seconds);

            case "money":
                double money = leaderboardManager.getTotalMoney(playerId);
                return String.format("%.2f", money);

            case "rank_time":
                return getRankString(playerId, "time");

            case "rank_money":
                return getRankString(playerId, "money");

            case "is_afk":
                if (player.isOnline()) {
                    return afkManager.isPlayerAFK(player.getPlayer()) ? "是" : "否";
                }
                return "否";

            default:
                return "无效参数";
        }
    }

    private String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return String.format("%d小时%02d分钟", hours, minutes);
    }

    private String getRankString(UUID playerId, String type) {
        int rank = 1;
        List<?> topList;

        if (type.equals("time")) {
            topList = new ArrayList<>(leaderboardManager.getTopAFKTime(100));
            for (Object entry : topList) {
                @SuppressWarnings("unchecked")
                Map.Entry<UUID, Long> e = (Map.Entry<UUID, Long>) entry;
                if (e.getKey().equals(playerId)) {
                    return String.valueOf(rank);
                }
                rank++;
            }
        } else {
            topList = new ArrayList<>(leaderboardManager.getTopMoney(100));
            for (Object entry : topList) {
                @SuppressWarnings("unchecked")
                Map.Entry<UUID, Double> e = (Map.Entry<UUID, Double>) entry;
                if (e.getKey().equals(playerId)) {
                    return String.valueOf(rank);
                }
                rank++;
            }
        }

        return "未上榜";
    }

    public void register() {
        // 这里需要根据PAPI的API进行注册
        // 在实际使用时，需要根据PAPI的版本进行调整
        plugin.getLogger().info("PAPI扩展已准备就绪，需要在服务器中手动注册");
    }
}