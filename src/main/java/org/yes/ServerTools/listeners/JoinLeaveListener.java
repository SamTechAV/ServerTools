package org.yes.ServerTools.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.yes.ServerTools.ServerTools;
import org.yes.ServerTools.utils.EconomyManager;
import org.yes.ServerTools.utils.MessageUtil;

import java.util.HashMap;
import java.util.Map;

public class JoinLeaveListener implements Listener {

    private final ServerTools plugin;
    private final EconomyManager economyManager;

    public JoinLeaveListener(ServerTools plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Initialize player's balance
        economyManager.initializePlayer(event.getPlayer());

        // Send join message
        String joinMessage = plugin.getPluginConfig().getString("joinandleave.joinMessage", "&a%player_name% has joined the server!");

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%player_name%", event.getPlayer().getName());

        event.joinMessage(MessageUtil.parseMessage(event.getPlayer(), joinMessage, placeholders));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Send quit message
        String quitMessage = plugin.getPluginConfig().getString("joinandleave.quitMessage", "&c%player_name% has left the server.");

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%player_name%", event.getPlayer().getName());

        event.quitMessage(MessageUtil.parseMessage(event.getPlayer(), quitMessage, placeholders));
    }
}
