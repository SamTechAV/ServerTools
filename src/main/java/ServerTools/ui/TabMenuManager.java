package ServerTools.ui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import me.clip.placeholderapi.PlaceholderAPI;

public class TabMenuManager {
    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private final MiniMessage miniMessage;
    private final LuckPerms luckPerms;

    public TabMenuManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.miniMessage = MiniMessage.miniMessage();
        this.luckPerms = LuckPermsProvider.get();

        startUpdateTask();
    }

    private void startUpdateTask() {
        int updateInterval = config.getInt("tab-menu.update-interval", 20);
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllPlayers, 0L, updateInterval);
    }

    private void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerTabMenu(player);
        }
    }

    private void updatePlayerTabMenu(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard == Bukkit.getScoreboardManager().getMainScoreboard()) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(scoreboard);
        }

        String headerFormat = config.getString("tab-menu.header", "<gold>Welcome to the server!");
        String footerFormat = config.getString("tab-menu.footer", "<gray>Players online: <green>%server_online%</green>");

        Component header = miniMessage.deserialize(PlaceholderAPI.setPlaceholders(player, headerFormat));
        Component footer = miniMessage.deserialize(PlaceholderAPI.setPlaceholders(player, footerFormat));

        player.sendPlayerListHeaderAndFooter(header, footer);

        for (Player target : Bukkit.getOnlinePlayers()) {
            updatePlayerInTab(scoreboard, player, target);
        }
    }

    private void updatePlayerInTab(Scoreboard scoreboard, Player viewer, Player target) {
        User user = luckPerms.getUserManager().getUser(target.getUniqueId());
        if (user == null) return;

        String groupName = user.getPrimaryGroup();
        String format = config.getString("tab-menu.name-format", "<group_color><group_name> <white>| <player_name>");

        format = format.replace("<group_name>", groupName)
                .replace("<player_name>", target.getName())
                .replace("<group_color>", getGroupColor(groupName));

        String teamName = getTeamName(groupName, target.getName());
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        Component displayName = miniMessage.deserialize(PlaceholderAPI.setPlaceholders(target, format));
        team.prefix(displayName);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);

        // Remove the player from any other teams
        for (Team t : scoreboard.getTeams()) {
            t.removeEntry(target.getName());
        }

        // Add the player to the correct team
        team.addEntry(target.getName());
    }

    private String getTeamName(String groupName, String playerName) {
        return groupName + playerName.substring(0, Math.min(playerName.length(), 14));
    }

    private String getGroupColor(String groupName) {
        return config.getString("tab-menu.group-colors." + groupName.toLowerCase(), "<white>");
    }
}