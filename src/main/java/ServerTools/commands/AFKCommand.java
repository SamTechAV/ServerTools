package ServerTools.commands;

import ServerTools.ServerTools;
import ServerTools.utils.AFKManager;
import ServerTools.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AFKCommand implements CommandExecutor {

    private final ServerTools plugin;
    private final AFKManager afkManager;

    public AFKCommand(ServerTools plugin) {
        this.plugin = plugin;
        this.afkManager = plugin.getAFKManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.parseMessage(sender, "<red>Only players can use this command.", null));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("servertools.afk")) {
            player.sendMessage(MessageUtil.parseMessage(player, "<red>You don't have permission to use this command.", null));
            return true;
        }

        boolean isAFK = afkManager.isAFK(player);
        afkManager.setAFK(player, !isAFK);

        return true;
    }
}