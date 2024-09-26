package ServerTools.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ServerTools.ServerTools;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AFKManager {

    private final ServerTools plugin;
    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    private final Set<UUID> afkPlayers = ConcurrentHashMap.newKeySet();
    private final boolean autoAFKEnabled;
    private final int autoAFKTime; // in seconds

    public AFKManager(ServerTools plugin) {
        this.plugin = plugin;
        this.autoAFKEnabled = plugin.getConfig().getBoolean("afk.auto-afk", true);
        this.autoAFKTime = plugin.getConfig().getInt("afk.auto-afk-time", 300);

        if (plugin.getConfig().getBoolean("afk.enabled", true)) {
            startAFKChecker();
        }
    }

    private void startAFKChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    if (afkPlayers.contains(uuid)) {
                        continue; // Skip players already AFK
                    }
                    long lastActive = lastActivity.getOrDefault(uuid, currentTime);
                    if (autoAFKEnabled && (currentTime - lastActive) >= autoAFKTime * 1000L) {
                        setAFK(player, true);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 100L); // Check every 5 seconds (100 ticks)
    }

    public void updateActivity(Player player) {
        UUID uuid = player.getUniqueId();
        lastActivity.put(uuid, System.currentTimeMillis());
        if (afkPlayers.contains(uuid)) {
            setAFK(player, false);
        }
    }

    public void setAFK(Player player, boolean isAFK) {
        UUID uuid = player.getUniqueId();
        if (isAFK && !afkPlayers.contains(uuid)) {
            afkPlayers.add(uuid);
            plugin.getLogger().info(player.getName() + " is now AFK");
            String playerAFKMessage = plugin.getConfig().getString("afk.messages.player-afk", "<yellow>%player_name% is now AFK.")
                    .replace("%player_name%", player.getName());
            broadcastMessage(playerAFKMessage);
            player.sendMessage(MessageUtil.parseMessage(player, plugin.getConfig().getString("afk.messages.afk-enabled", "<yellow>You are now AFK."), null));
        } else if (!isAFK && afkPlayers.remove(uuid)) {
            plugin.getLogger().info(player.getName() + " is no longer AFK");
            String playerReturnedMessage = plugin.getConfig().getString("afk.messages.player-returned", "<yellow>%player_name% is no longer AFK.")
                    .replace("%player_name%", player.getName());
            broadcastMessage(playerReturnedMessage);
            player.sendMessage(MessageUtil.parseMessage(player, plugin.getConfig().getString("afk.messages.afk-disabled", "<yellow>You are no longer AFK."), null));
        }
    }

    private void broadcastMessage(String message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(MessageUtil.parseMessage(p, message, null));
        }
    }

    public boolean isAFK(Player player) {
        return afkPlayers.contains(player.getUniqueId());
    }

    public Set<UUID> getAFKPlayers() {
        return Collections.unmodifiableSet(afkPlayers);
    }
}