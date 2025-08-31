package org.examplexiaodou.douafkreward;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class EventListener implements Listener {
    private DouAFKReward plugin;
    private SelectionManager selectionManager;
    private AFKManager afkManager;

    public EventListener(DouAFKReward plugin, SelectionManager selectionManager, AFKManager afkManager) {
        this.plugin = plugin;
        this.selectionManager = selectionManager;
        this.afkManager = afkManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.onPlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.BLAZE_ROD &&
                item.hasItemMeta() &&
                item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "挂机区域选择工具")) {

            event.setCancelled(true);

            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                selectionManager.setFirstPoint(player, event.getClickedBlock().getLocation());
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                selectionManager.setSecondPoint(player, event.getClickedBlock().getLocation());
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // 检查玩家是否在普通挂机区域内
        boolean isInAFKArea = afkManager.isInAFKArea(player);
        boolean isAFK = afkManager.isPlayerAFK(player);

        if (isInAFKArea && !isAFK) {
            String areaName = afkManager.getPlayerAFKAreaName(player);
            if (areaName != null) {
                String enterMessage = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfigManager().getMessage("enter-afk"));
                sendAFKMessage(player, enterMessage, "enter");
                afkManager.addAFKPlayer(player, areaName);
            }
        } else if (!isInAFKArea && isAFK) {
            String leaveMessage = ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfigManager().getMessage("leave-afk"));
            sendAFKMessage(player, leaveMessage, "leave");
            afkManager.removeAFKPlayer(player);
        }
    }

    private void sendAFKMessage(Player player, String message, String type) {
        FileConfiguration config = plugin.getConfigManager().getMainConfig();

        if (config.getBoolean("message-settings.title-enabled", true)) {
            player.sendTitle(message, "", 10, 70, 20);
        }

        if (config.getBoolean("message-settings.actionbar-enabled", false)) {
            try {
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
            } catch (Exception e) {
                player.sendMessage(message);
            }
        } else {
            player.sendMessage(message);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // 移除普通挂机状态
        if (afkManager.isPlayerAFK(player)) {
            afkManager.removeAFKPlayer(player);
        }

        // 停止区域显示
        selectionManager.clearSelection(player);
    }
}