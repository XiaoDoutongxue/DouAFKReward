package org.examplexiaodou.douafkreward;

import org.bukkit.Bukkit;
import org.bukkit.Sound;

public class VersionAdapter {

    public static Sound getSound(String soundName, Sound defaultSound) {
        try {
            // 尝试获取指定版本的声音
            return Sound.valueOf(soundName);
        } catch (IllegalArgumentException e) {
            // 如果声音不存在，尝试使用兼容的声音
            return getCompatibleSound(soundName, defaultSound);
        }
    }

    private static Sound getCompatibleSound(String soundName, Sound defaultSound) {
        // 这里添加不同版本的声音映射
        switch (soundName) {
            case "ENTITY_PLAYER_LEVELUP":
                try {
                    return Sound.valueOf("ENTITY_PLAYER_LEVELUP");
                } catch (IllegalArgumentException e) {
                    return Sound.ENTITY_PLAYER_LEVELUP;
                }
            case "ENTITY_VILLAGER_NO":
                try {
                    return Sound.valueOf("ENTITY_VILLAGER_NO");
                } catch (IllegalArgumentException e) {
                    return Sound.ENTITY_VILLAGER_NO;
                }
            default:
                return defaultSound;
        }
    }

    public static String getBukkitVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    }

    public static boolean isVersionOrNewer(String version) {
        String currentVersion = getBukkitVersion();
        // 简单的版本比较逻辑
        return currentVersion.compareTo(version) >= 0;
    }
}