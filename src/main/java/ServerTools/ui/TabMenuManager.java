package ServerTools.ui;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.util.List;

public class TabMenuManager {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage;
    private final LuckPerms luckPerms;

    private FileConfiguration tabConfig;
    private List<String> headerFrames;
    private List<String> footerFrames;
    private int refreshInterval;
    private int currentFrameIndex = 0;

    public TabMenuManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.luckPerms = LuckPermsProvider.get();

        loadTabConfig();
        startUpdateTask();
    }

    private void loadTabConfig() {
        File tabConfigFile = new File(plugin.getDataFolder(), "config/tabmenu.yml");
        if (!tabConfigFile.exists()) {
            plugin.saveResource("config/tabmenu.yml", false);
        }
        tabConfig = YamlConfiguration.loadConfiguration(tabConfigFile);

        refreshInterval = tabConfig.getInt("refresh-interval", 40);
        headerFrames = tabConfig.getStringList("header-frames");
        footerFrames = tabConfig.getStringList("footer-frames");
    }

    private void startUpdateTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllPlayers, 0L, refreshInterval);
    }

    private void updateAllPlayers() {
        // Cycle to the next frame
        currentFrameIndex = (currentFrameIndex + 1) % Math.max(headerFrames.size(), footerFrames.size());

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

        String headerFormat = getCurrentFrame(headerFrames);
        String footerFormat = getCurrentFrame(footerFrames);

        String parsedHeader = PlaceholderAPI.setPlaceholders(player, headerFormat);
        String parsedFooter = PlaceholderAPI.setPlaceholders(player, footerFormat);

        Component header = miniMessage.deserialize(parsedHeader);
        Component footer = miniMessage.deserialize(parsedFooter);

        player.sendPlayerListHeaderAndFooter(header, footer);

        for (Player target : Bukkit.getOnlinePlayers()) {
            updatePlayerInTab(scoreboard, player, target);
        }
    }

    private String getCurrentFrame(List<String> frames) {
        if (frames.isEmpty()) {
            return "";
        }
        int index = currentFrameIndex % frames.size();
        return frames.get(index);
    }

    private void updatePlayerInTab(Scoreboard scoreboard, Player viewer, Player target) {
        User user = luckPerms.getUserManager().getUser(target.getUniqueId());
        if (user == null) return;

        String groupName = user.getPrimaryGroup();
        String format = tabConfig.getString("name-format", "<group_color><group_name> <white>| <player_name>");

        format = format.replace("<group_name>", groupName)
                .replace("<player_name>", target.getName())
                .replace("<group_color>", getGroupColor(groupName));

        String parsedFormat = PlaceholderAPI.setPlaceholders(target, format);
        Component displayName = miniMessage.deserialize(parsedFormat);

        String teamName = getTeamName(groupName, target.getName());
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        team.prefix(displayName);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);

        // Remove the player from any other teams
        for (Team t : scoreboard.getTeams()) {
            if (t.hasEntry(target.getName())) {
                t.removeEntry(target.getName());
            }
        }

        // Add the player to the correct team
        team.addEntry(target.getName());
    }

    private String getTeamName(String groupName, String playerName) {
        String name = groupName + playerName;
        // Ensure team names are unique and not longer than 16 characters
        return name.substring(0, Math.min(name.length(), 16));
    }

    private String getGroupColor(String groupName) {
        return tabConfig.getString("group-colors." + groupName.toLowerCase(), "<white>");
    }

    public void reloadTabConfig() {
        loadTabConfig();
    }
}
