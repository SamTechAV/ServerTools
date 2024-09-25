package ServerTools.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import ServerTools.ServerTools;
import ServerTools.utils.HomeManager;
import ServerTools.utils.MessageUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class HomeCommand implements CommandExecutor {

    private final ServerTools plugin;
    private final FileConfiguration config;
    private final HomeManager homeManager;

    public HomeCommand(ServerTools plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.homeManager = plugin.getHomeManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("home.playerOnly", "<red>This command can only be used by players!"), null));
            return true;
        }

        Player player = (Player) sender;
        String commandName = command.getName().toLowerCase();

        switch (commandName) {
            case "sethome":
                return handleSetHome(player, args);
            case "home":
                return handleHome(player, args);
            case "delhome":
                return handleDelHome(player, args);
            case "homes":
                return handleListHomes(player);
            default:
                return false;
        }
    }

    private boolean handleSetHome(Player player, String[] args) {
        if (!player.hasPermission("servertools.home.sethome")) {
            player.sendMessage(MessageUtil.parseMessage(player, config.getString("home.noPermission", "<red>You don't have permission to use this command."), null));
            return true;
        }

        String homeName = args.length > 0 ? args[0] : "home";

        Map<String, Location> homes = homeManager.getHomes(player);
        int maxHomes = homeManager.getMaxHomes(player);

        if (!homes.containsKey(homeName.toLowerCase()) && maxHomes != -1 && homes.size() >= maxHomes) {
            player.sendMessage(MessageUtil.parseMessage(player, config.getString("home.homeLimitReached", "<red>You have reached the maximum number of homes."), null));
            return true;
        }

        homeManager.setHome(player, homeName, player.getLocation());

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%home_name%", homeName);
        player.sendMessage(MessageUtil.parseMessage(player, config.getString("home.setHome", "<green>Home '%home_name%' set!"), placeholders));
        return true;
    }

    private boolean handleHome(Player player, String[] args) {
        if (!player.hasPermission("servertools.home.home")) {
            player.sendMessage(MessageUtil.parseMessage(player, config.getString("home.noPermission", "<red>You don't have permission to use this command."), null));
            return true;
        }

        String homeName = args.length > 0 ? args[0] : "home";

        if (!homeManager.hasHome(player, homeName)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%home_name%", homeName);
            player.sendMessage(MessageUtil.parseMessage(player, config.getString("home.noHomeSet", "<red>You have not set a home named '%home_name%' yet."), placeholders));
            return true;
        }

        player.teleport(homeManager.getHome(player, homeName));

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%home_name%", homeName);
        player.sendMessage(MessageUtil.parseMessage(player, config.getString("home.teleportHome", "<green>Teleported to home '%home_name%'."), placeholders));
        return true;
    }

    private boolean handleDelHome(Player player, String[] args) {
        if (!player.hasPermission("servertools.home.delhome")) {
            player.sendMessage(MessageUtil.parseMessage(player, config.getString("home.noPermission", "<red>You don't have permission to use this command."), null));
            return true;
        }

        String homeName = args.length > 0 ? args[0] : "home";

        if (!homeManager.hasHome(player, homeName)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%home_name%", homeName);
            player.sendMessage(MessageUtil.parseMessage(player, config.getString("home.noHomeToDelete", "<red>You don't have a home named '%home_name%' to delete."), placeholders));
            return true;
        }

        homeManager.deleteHome(player, homeName);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%home_name%", homeName);
        player.sendMessage(MessageUtil.parseMessage(player, config.getString("home.homeDeleted", "<green>Your home '%home_name%' has been deleted."), placeholders));
        return true;
    }

    private boolean handleListHomes(Player player) {
        if (!player.hasPermission("servertools.home.homes")) {
            player.sendMessage(MessageUtil.parseMessage(player, config.getString("home.noPermission", "<red>You don't have permission to use this command."), null));
            return true;
        }

        Map<String, Location> homes = homeManager.getHomes(player);
        if (homes.isEmpty()) {
            player.sendMessage(MessageUtil.parseMessage(player, config.getString("home.noHomesSet", "<red>You have not set any homes yet."), null));
            return true;
        }

        List<String> homeNames = new ArrayList<>(homes.keySet());
        String homesList = String.join(", ", homeNames);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%homes%", homesList);
        player.sendMessage(MessageUtil.parseMessage(player, config.getString("home.listHomes", "<green>Your homes: %homes%"), placeholders));
        return true;
    }
}
