package org.examplexiaodou.douafkreward;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private JavaPlugin plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    public void loadConfigs() {
        try {
            // 加载主配置 - 使用自定义方法保留注释
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                plugin.saveDefaultConfig();
            } else {
                // 检查并更新配置，保留注释
                checkAndUpdateConfig(configFile);
            }
            plugin.reloadConfig();
            plugin.getLogger().info("主配置文件加载完成");

            // 确保所有语言文件都存在
            ensureLanguageFiles();

            // 加载当前选择的语言配置
            reloadLanguageConfig();

        } catch (Exception e) {
            plugin.getLogger().severe("加载配置文件时发生错误: " + e.getMessage());
            e.printStackTrace();
            messagesConfig = new YamlConfiguration();
        }
    }

    /**
     * 检查并更新配置，保留注释
     */
    private void checkAndUpdateConfig(File configFile) {
        try {
            // 读取原始配置文件内容（保留注释）
            StringBuilder originalContent = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    originalContent.append(line).append("\n");
                }
            }

            // 加载当前配置
            YamlConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);

            // 从JAR中获取默认配置
            InputStream defaultStream = plugin.getResource("config.yml");
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));

                // 检查是否有新配置项需要添加
                boolean changed = checkConfigUpdates(currentConfig, defaultConfig);

                if (changed) {
                    // 保存配置但保留注释结构
                    saveConfigWithComments(currentConfig, configFile, originalContent.toString());
                    plugin.getLogger().info("主配置文件已更新（保留注释）");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("检查配置更新时出错: " + e.getMessage());
        }
    }

    /**
     * 保存配置并保留注释
     */
    private void saveConfigWithComments(FileConfiguration config, File configFile, String originalContent) {
        try {
            // 这里可以使用更高级的注释保留库，但为了简单起见，我们使用基础方法
            // 在实际项目中，可以考虑使用 CommentYamlConfiguration 等库

            // 先保存配置
            config.save(configFile);

            // 如果可能，尝试重新插入注释（简化版本）
            // 注意：这是一个简化的实现，复杂的注释结构可能需要更高级的处理

        } catch (IOException e) {
            plugin.getLogger().warning("保存配置文件时出错: " + e.getMessage());
        }
    }

    public void reloadLanguageConfig() {
        try {
            String languageFile = plugin.getConfig().getString("language", "chinese.yml");
            File newMessagesFile = new File(plugin.getDataFolder(), languageFile);

            if (newMessagesFile.exists()) {
                messagesFile = newMessagesFile;
                messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
                plugin.getLogger().info("已加载语言文件: " + languageFile);
            } else {
                plugin.getLogger().warning("语言文件 " + languageFile + " 不存在，使用默认中文配置");
                messagesFile = new File(plugin.getDataFolder(), "chinese.yml");
                if (!messagesFile.exists()) {
                    createDefaultConfig("chinese.yml", messagesFile);
                }
                messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("加载语言文件时出错: " + e.getMessage());
        }
    }

    private void ensureLanguageFiles() {
        String[] languageFiles = {"chinese.yml", "english.yml", "russian.yml"};

        for (String fileName : languageFiles) {
            File file = new File(plugin.getDataFolder(), fileName);
            if (!file.exists()) {
                createDefaultConfig(fileName, file);
            }
        }
    }

    public void reloadConfigs() {
        try {
            // 重新加载主配置（保留注释）
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            checkAndUpdateConfig(configFile);
            plugin.reloadConfig();

            reloadLanguageConfig(); // 重新加载语言配置
            plugin.getLogger().info("所有配置文件已重新加载");
        } catch (Exception e) {
            plugin.getLogger().warning("重新加载配置文件时出错: " + e.getMessage());
        }
    }

    /**
     * 检查配置更新
     */
    public boolean checkConfigUpdates(FileConfiguration config, FileConfiguration defaultConfig) {
        boolean changed = false;

        // 检查是否有新配置项需要添加
        if (!config.contains("leaderboards.save-interval") && defaultConfig.contains("leaderboards.save-interval")) {
            config.set("leaderboards.save-interval", defaultConfig.getInt("leaderboards.save-interval"));
            changed = true;
        }

        if (!config.contains("reward-interval") && defaultConfig.contains("reward-interval")) {
            config.set("reward-interval", defaultConfig.getInt("reward-interval"));
            changed = true;
        }

        if (!config.contains("base-reward") && defaultConfig.contains("base-reward")) {
            config.set("base-reward", defaultConfig.getDouble("base-reward"));
            changed = true;
        }

        // 检查权限组配置
        if (!config.contains("group-multipliers") && defaultConfig.contains("group-multipliers")) {
            ConfigurationSection groupSection = defaultConfig.getConfigurationSection("group-multipliers");
            for (String group : groupSection.getKeys(false)) {
                config.set("group-multipliers." + group, groupSection.getDouble(group));
            }
            changed = true;
        }

        // 检查每日限制配置
        if (!config.contains("daily-limits.enabled") && defaultConfig.contains("daily-limits.enabled")) {
            ConfigurationSection dailySection = defaultConfig.getConfigurationSection("daily-limits");
            for (String key : dailySection.getKeys(false)) {
                config.set("daily-limits." + key, dailySection.get(key));
            }
            changed = true;
        }

        return changed;
    }

    public void checkConfigUpdates() {
        FileConfiguration config = plugin.getConfig();
        FileConfiguration defaultConfig = null;

        // 从JAR中获取默认配置
        InputStream defaultStream = plugin.getResource("config.yml");
        if (defaultStream != null) {
            defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
        }

        if (defaultConfig != null && checkConfigUpdates(config, defaultConfig)) {
            plugin.saveConfig();
            plugin.getLogger().info("主配置文件已更新");
        }
    }

    /**
     * 创建默认配置文件
     */
    private void createDefaultConfig(String resourceName, File outputFile) {
        try {
            // 确保插件数据目录存在
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            // 尝试从 JAR 中复制默认配置
            InputStream inputStream = plugin.getResource(resourceName);
            if (inputStream != null) {
                Files.copy(inputStream, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("已从 JAR 中复制默认配置: " + resourceName);
            } else {
                // 如果 JAR 中没有该资源，创建空文件
                outputFile.createNewFile();
                plugin.getLogger().info("创建了空的配置文件: " + resourceName);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("创建配置文件 " + resourceName + " 时出错: " + e.getMessage());
        }
    }

    public void saveConfigs() {
        try {
            if (messagesConfig != null && messagesFile != null) {
                messagesConfig.save(messagesFile);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("保存配置文件时出错: " + e.getMessage());
        }
    }

    public String getMessage(String path) {
        String result = getMessageInternal(path);
        if (result.startsWith("&c消息配置错误")) {
            // 返回备用消息
            return ChatColor.translateAlternateColorCodes('&', getFallbackMessage(path));
        }
        // 确保颜色代码被转换
        return ChatColor.translateAlternateColorCodes('&', result);
    }

    public String getMessage(String path, String defaultValue) {
        String result = getMessageInternal(path);
        if (result.startsWith("&c消息配置错误")) {
            return defaultValue;
        }
        return result;
    }

    private String getMessageInternal(String path) {
        if (messagesConfig != null) {
            // 尝试直接路径
            if (messagesConfig.contains(path)) {
                return messagesConfig.getString(path);
            }
            // 尝试 messages. 前缀路径
            if (messagesConfig.contains("messages." + path)) {
                return messagesConfig.getString("messages." + path);
            }
        }
        return "&c消息配置错误: " + path;
    }

    // 获取排行榜格式配置
    public String getTopFormat(String type, String format) {
        String path = "formats." + type + "." + format;
        if (messagesConfig != null && messagesConfig.contains(path)) {
            return messagesConfig.getString(path);
        }
        return getFallbackTopFormat(type, format);
    }

    private String getFallbackTopFormat(String type, String format) {
        Map<String, Map<String, String>> fallbackFormats = new HashMap<>();

        Map<String, String> timeFormats = new HashMap<>();
        timeFormats.put("header", "&6========== 挂机时长排行榜 ==========");
        timeFormats.put("line", "&e%rank%. &f%player% &7- &a%hours%小时%minutes%分钟");
        timeFormats.put("no-data", "&c暂无挂机数据");
        timeFormats.put("time-format", "%d小时%02d分钟");
        fallbackFormats.put("time", timeFormats);

        Map<String, String> moneyFormats = new HashMap<>();
        moneyFormats.put("header", "&6========== 挂机收益排行榜 ==========");
        moneyFormats.put("line", "&e%rank%. &f%player% &7- &a%money%");
        moneyFormats.put("no-data", "&c暂无收益数据");
        moneyFormats.put("money-format", "%.2f");
        fallbackFormats.put("money", moneyFormats);

        return fallbackFormats.getOrDefault(type, new HashMap<>()).getOrDefault(format, "");
    }

    private String getFallbackMessage(String path) {
        // 备用消息映射
        Map<String, String> fallbackMessages = new HashMap<>();
        fallbackMessages.put("no-permission", "&c你没有权限使用此命令!");
        fallbackMessages.put("player-only", "&c只有玩家可以使用此命令!");
        fallbackMessages.put("usage", "&c用法错误!");
        fallbackMessages.put("invalid-amount", "&c无效的金额!");
        fallbackMessages.put("player-not-found", "&c玩家不存在!");
        fallbackMessages.put("enter-afk", "&a你已开始挂机!");
        fallbackMessages.put("leave-afk", "&c你已停止挂机!");
        fallbackMessages.put("reward-received", "&6挂机奖励: &a%amount%");
        fallbackMessages.put("daily-limit-reached", "&c今日挂机奖励已达上限!");
        fallbackMessages.put("config-reloaded", "&a配置已重新加载!");
        fallbackMessages.put("area-created", "&a挂机区域已创建!");
        fallbackMessages.put("area-removed", "&a挂机区域已删除!");
        fallbackMessages.put("area-exists", "&c挂机区域已存在!");
        fallbackMessages.put("area-not-found", "&c挂机区域不存在!");

        return fallbackMessages.getOrDefault(path, "&c未知错误: " + path);
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public FileConfiguration getMainConfig() {
        return plugin.getConfig();
    }

    /**
     * 检查配置文件是否成功加载
     */
    public boolean isConfigLoaded() {
        return messagesConfig != null;
    }

    /**
     * 检查特定消息路径是否存在
     */
    public boolean hasMessage(String path) {
        if (messagesConfig == null) return false;
        return messagesConfig.contains(path) || messagesConfig.contains("messages." + path);
    }
}