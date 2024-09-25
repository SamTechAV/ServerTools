package ServerTools.commands;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import ServerTools.ServerTools;
import ServerTools.utils.MessageUtil;

public class GameModeCommand implements CommandExecutor {

    private final ServerTools plugin;
    private final String commandName;

    public GameModeCommand(ServerTools plugin, String commandName) {
        this.plugin = plugin;
        this.commandName = commandName;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.parseMessage(sender, "&cThis command can only be used by players!", null));
            return true;
        }

        Player player = (Player) sender;
        FileConfiguration config = plugin.getPluginConfig();

        switch (commandName.toLowerCase()) {
            case "gmc":
                player.setGameMode(GameMode.CREATIVE);
                sendMessage(player, "gmcMessage");
                break;
            case "gms":
                player.setGameMode(GameMode.SURVIVAL);
                sendMessage(player, "gmsMessage");
                break;
            case "gmsp":
                player.setGameMode(GameMode.SPECTATOR);
                sendMessage(player, "gmspMessage");
                break;
            case "gma":
                player.setGameMode(GameMode.ADVENTURE);
                sendMessage(player, "gmaMessage");
                break;
            default:
                return false;
        }

        return true;
    }

    private void sendMessage(Player player, String messageKey) {
        String message = plugin.getPluginConfig().getString("gamemode." + messageKey, "&aYour game mode has been updated.");
        player.sendMessage(MessageUtil.parseMessage(player, message, null));
    }
}