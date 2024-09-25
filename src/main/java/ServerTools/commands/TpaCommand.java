package ServerTools.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import ServerTools.ServerTools;
import ServerTools.utils.MessageUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TpaCommand implements CommandExecutor {

    private final ServerTools plugin;
    private final FileConfiguration config;
    private final Map<UUID, UUID> tpaRequests = new HashMap<>();
    private final Map<UUID, UUID> tpahereRequests = new HashMap<>();

    public TpaCommand(ServerTools plugin) {
        this.plugin = plugin;
        this.config = plugin.getPluginConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.parseMessage(sender, "<red>This command can only be used by players!", null));
            return true;
        }

        Player player = (Player) sender;
        String commandName = command.getName().toLowerCase();

        switch (commandName) {
            case "tpa":
                return handleTpaCommand(player, args);
            case "tpahere":
                return handleTpahereCommand(player, args);
            case "tpaccept":
                return handleTpAcceptCommand(player);
            case "tpdeny":
                return handleTpDenyCommand(player);
            default:
                return false;
        }
    }

    private boolean handleTpaCommand(Player requester, String[] args) {
        if (args.length != 1) {
            requester.sendMessage(MessageUtil.parseMessage(requester, config.getString("tpa.tpaUsage", "<red>Usage: /tpa <player>"), null));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            requester.sendMessage(MessageUtil.parseMessage(requester, config.getString("economy.playerNotFound", "<red>Player not found or not online."), null));
            return true;
        }

        if (target.equals(requester)) {
            requester.sendMessage(MessageUtil.parseMessage(requester, config.getString("tpa.cannotTeleportToSelf", "<red>You cannot send a teleport request to yourself!"), null));
            return true;
        }

        tpaRequests.put(target.getUniqueId(), requester.getUniqueId());

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%target%", target.getName());
        placeholders.put("%requester%", requester.getName());

        requester.sendMessage(MessageUtil.parseMessage(requester, config.getString("tpa.tpaRequestSent"), placeholders));
        sendClickableRequest(target, requester, "tpa");
        return true;
    }

    private boolean handleTpahereCommand(Player requester, String[] args) {
        if (args.length != 1) {
            requester.sendMessage(MessageUtil.parseMessage(requester, config.getString("tpa.tpahereUsage", "<red>Usage: /tpahere <player>"), null));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            requester.sendMessage(MessageUtil.parseMessage(requester, config.getString("economy.playerNotFound", "<red>Player not found or not online."), null));
            return true;
        }

        if (target.equals(requester)) {
            requester.sendMessage(MessageUtil.parseMessage(requester, config.getString("tpa.cannotTeleportToSelf", "<red>You cannot send a teleport request to yourself!"), null));
            return true;
        }

        tpahereRequests.put(target.getUniqueId(), requester.getUniqueId());

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%target%", target.getName());
        placeholders.put("%requester%", requester.getName());

        requester.sendMessage(MessageUtil.parseMessage(requester, config.getString("tpa.tpahereRequestSent"), placeholders));
        sendClickableRequest(target, requester, "tpahere");
        return true;
    }

    private void sendClickableRequest(Player target, Player requester, String requestType) {
        String messageKey = "tpa." + requestType + "RequestReceived";
        String messageTemplate = config.getString(messageKey, "<green>%requester% has requested to teleport " + (requestType.equals("tpa") ? "to you" : "you to them") + ". Click to respond:");

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%requester%", requester.getName());

        Component baseComponent = MessageUtil.parseMessage(target, messageTemplate, placeholders);

        String acceptSymbol = config.getString("tpa.tpaAcceptSymbol", " ✔ ");
        String acceptSymbolColor = config.getString("tpa.tpaAcceptSymbolColor", "GREEN");
        String acceptHoverText = config.getString("tpa.tpaAcceptHoverText", "<green>Click to accept");

        Component acceptComponent = Component.text(acceptSymbol)
                .color(MessageUtil.parseTextColor(acceptSymbolColor))
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/tpaccept"))
                .hoverEvent(HoverEvent.showText(MessageUtil.parseMessage(target, acceptHoverText, null)));

        String denySymbol = config.getString("tpa.tpaDenySymbol", " ✘ ");
        String denySymbolColor = config.getString("tpa.tpaDenySymbolColor", "RED");
        String denyHoverText = config.getString("tpa.tpaDenyHoverText", "<red>Click to deny");

        Component denyComponent = Component.text(denySymbol)
                .color(MessageUtil.parseTextColor(denySymbolColor))
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/tpdeny"))
                .hoverEvent(HoverEvent.showText(MessageUtil.parseMessage(target, denyHoverText, null)));

        target.sendMessage(baseComponent.append(acceptComponent).append(denyComponent));
    }

    private boolean handleTpAcceptCommand(Player target) {
        UUID requesterId = tpaRequests.remove(target.getUniqueId());
        boolean isTpaHere = false;
        if (requesterId == null) {
            requesterId = tpahereRequests.remove(target.getUniqueId());
            isTpaHere = true;
        }

        if (requesterId == null) {
            target.sendMessage(MessageUtil.parseMessage(target, config.getString("tpa.noRequestToAccept", "<red>You have no pending teleport requests to accept."), null));
            return true;
        }

        Player requester = Bukkit.getPlayer(requesterId);
        if (requester == null || !requester.isOnline()) {
            target.sendMessage(MessageUtil.parseMessage(target, config.getString("economy.playerNotFound", "<red>Player not found or not online."), null));
            return true;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%target%", target.getName());
        placeholders.put("%requester%", requester.getName());

        if (isTpaHere) {
            target.teleport(requester.getLocation());
            requester.sendMessage(MessageUtil.parseMessage(requester, config.getString("tpa.tpahereAccepted"), placeholders));
            target.sendMessage(MessageUtil.parseMessage(target, config.getString("tpa.tpahereRequestAccepted"), placeholders));
        } else {
            requester.teleport(target.getLocation());
            requester.sendMessage(MessageUtil.parseMessage(requester, config.getString("tpa.tpaAccepted"), placeholders));
            target.sendMessage(MessageUtil.parseMessage(target, config.getString("tpa.tpaRequestAccepted"), placeholders));
        }

        return true;
    }

    private boolean handleTpDenyCommand(Player target) {
        UUID requesterId = tpaRequests.remove(target.getUniqueId());
        if (requesterId == null) {
            requesterId = tpahereRequests.remove(target.getUniqueId());
        }

        if (requesterId == null) {
            target.sendMessage(MessageUtil.parseMessage(target, config.getString("tpa.noRequestToDeny", "<red>You have no pending teleport requests to deny."), null));
            return true;
        }

        Player requester = Bukkit.getPlayer(requesterId);
        String denyMessage = config.getString("tpa.tpRequestDenied", "<red>Your teleport request was denied.");

        if (requester != null && requester.isOnline()) {
            requester.sendMessage(MessageUtil.parseMessage(requester, denyMessage, null));
        }

        target.sendMessage(MessageUtil.parseMessage(target, config.getString("tpa.tpDenyConfirmation", "<red>You denied the teleport request."), null));

        return true;
    }
}
