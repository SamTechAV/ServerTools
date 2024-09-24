package org.yes.ServerTools.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.yes.ServerTools.ServerTools;
import org.yes.ServerTools.utils.MessageUtil;

public class FlyCommand implements CommandExecutor {

    private final ServerTools plugin;

    public FlyCommand(ServerTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.parseMessage(sender, plugin.getConfig().getString("fly.playerOnly", "<red>This command can only be used by players!")));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("servertools.fly")) {
            player.sendMessage(MessageUtil.parseMessage(player, plugin.getConfig().getString("fly.noPermission", "<red>You don't have permission to use this command.")));
            return true;
        }

        boolean isFlying = !player.getAllowFlight();
        player.setAllowFlight(isFlying);
        player.setFlying(isFlying);

        String message = isFlying
                ? plugin.getConfig().getString("fly.enabled", "<green>Flight mode enabled.")
                : plugin.getConfig().getString("fly.disabled", "<red>Flight mode disabled.");

        player.sendMessage(MessageUtil.parseMessage(player, message));

        return true;
    }
}