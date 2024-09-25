package ServerTools.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import ServerTools.ServerTools;
import ServerTools.utils.MessageUtil;

public class FlyCommand implements CommandExecutor {

    private final ServerTools plugin;
    private final FileConfiguration config;

    public FlyCommand(ServerTools plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("fly.playerOnly", "<red>This command can only be used by players!"), null));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("servertools.fly")) {
            player.sendMessage(MessageUtil.parseMessage(player, config.getString("fly.noPermission", "<red>You don't have permission to use this command."), null));
            return true;
        }

        boolean isFlying = !player.getAllowFlight();
        player.setAllowFlight(isFlying);
        player.setFlying(isFlying);

        String message = isFlying
                ? config.getString("fly.enabled", "<green>Flight mode enabled.")
                : config.getString("fly.disabled", "<red>Flight mode disabled.");

        player.sendMessage(MessageUtil.parseMessage(player, message, null));

        return true;
    }
}
