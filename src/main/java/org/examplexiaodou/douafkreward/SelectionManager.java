// SelectionManager.java
package org.examplexiaodou.douafkreward;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class SelectionManager {
    private DouAFKReward plugin;
    private Map<String, Location[]> selections = new HashMap<>();
    private Map<String, Integer> particleTasks = new HashMap<>();
    private Map<String, Integer> areaDisplayTasks = new HashMap<>();

    public SelectionManager(DouAFKReward plugin) {
        this.plugin = plugin;
    }

    public void setFirstPoint(Player player, Location loc) {
        String playerName = player.getName();
        if (!selections.containsKey(playerName)) {
            selections.put(playerName, new Location[2]);
        }
        selections.get(playerName)[0] = loc;
        player.sendMessage(ChatColor.GREEN + "已设置第一个点: " + formatLocation(loc));

        // 移除粒子效果显示
        // showParticles(loc);

        // 移除持续显示选择区域
        // startSelectionParticles(player);
    }

    public void setSecondPoint(Player player, Location loc) {
        String playerName = player.getName();
        if (!selections.containsKey(playerName) || selections.get(playerName)[0] == null) {
            player.sendMessage(ChatColor.RED + "请先设置第一个点!");
            return;
        }
        selections.get(playerName)[1] = loc;
        player.sendMessage(ChatColor.GREEN + "已设置第二个点: " + formatLocation(loc));

        // 移除粒子效果显示
        // showParticles(loc);

        // 移除持续显示选择区域
        // startSelectionParticles(player);
    }

    public Location[] getSelection(Player player) {
        return selections.get(player.getName());
    }

    public void clearSelection(Player player) {
        String playerName = player.getName();
        selections.remove(playerName);

        // 停止粒子效果任务
        stopSelectionParticles(player);
        stopAreaDisplay(player);
    }

    private String formatLocation(Location loc) {
        return String.format("X: %d, Y: %d, Z: %d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    // 移除粒子效果方法
    /*
    private void showParticles(Location loc) {
        loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0.5, 0.5, 0.5), 20, 0.2, 0.2, 0.2, 0.05);
    }
    */

    // 移除选择区域粒子效果
    private void startSelectionParticles(Player player) {
        // 空实现，不显示任何粒子效果
    }

    private void stopSelectionParticles(Player player) {
        String playerName = player.getName();
        if (particleTasks.containsKey(playerName)) {
            Bukkit.getScheduler().cancelTask(particleTasks.get(playerName));
            particleTasks.remove(playerName);
        }
    }

    // 移除显示所有已创建的挂机区域
    public void startDisplayingAllAreas(Player player) {
        // 空实现，不显示任何区域粒子效果
    }

    // 移除停止显示区域
    public void stopAreaDisplay(Player player) {
        String playerName = player.getName();
        if (areaDisplayTasks.containsKey(playerName)) {
            Bukkit.getScheduler().cancelTask(areaDisplayTasks.get(playerName));
            areaDisplayTasks.remove(playerName);
        }
    }

    // 移除显示所有已创建的挂机区域边界
    /*
    private void displayAllAFKAreas(Player player) {
        // 移除所有区域边界显示
    }
    */

    // 移除显示单个区域边界
    /*
    private void displayAreaBoundary(Player player, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, String worldName) {
        // 移除区域边界显示
    }
    */

    private boolean isHoldingSelectionTool(Player player) {
        if (player.getInventory().getItemInMainHand() == null) {
            return false;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        return item.getType() == Material.BLAZE_ROD &&
                item.hasItemMeta() &&
                item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "挂机区域选择工具");
    }
}