package org.yes.ServerTools;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerTools extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private final Map<UUID, UUID> tpaRequests = new HashMap<>();
    private final Map<UUID, UUID> tpahereRequests = new HashMap<>();
    private final Map<UUID, UUID> lastMessageSender = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("ServerTools with TPA system, clickable chat interactions, and direct messaging enabled.");

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("PlaceholderAPI is not installed. Some placeholders will not work.");
        } else {
            getLogger().info("PlaceholderAPI found and hooked!");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String joinMessage = config.getString("joinMessage", "&a%player_name% has joined the server!");
        joinMessage = replacePlaceholders(player, joinMessage);
        event.joinMessage(parseColoredMessage(joinMessage));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String quitMessage = config.getString("quitMessage", "&c%player_name% has left the server.");
        quitMessage = replacePlaceholders(player, quitMessage);
        event.quitMessage(parseColoredMessage(quitMessage));
        tpaRequests.remove(player.getUniqueId());
        tpahereRequests.remove(player.getUniqueId());
    }

    private String replacePlaceholders(Player player, String message) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            return PlaceholderAPI.setPlaceholders(player, message);
        }
        return message.replace("%player_name%", player.getName());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players!").color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        switch (command.getName().toLowerCase()) {
            case "gmc":
                return handleGameModeCommand(player, GameMode.CREATIVE, "gmcMessage");
            case "gms":
                return handleGameModeCommand(player, GameMode.SURVIVAL, "gmsMessage");
            case "gmsp":
                return handleGameModeCommand(player, GameMode.SPECTATOR, "gmspMessage");
            case "gma":
                return handleGameModeCommand(player, GameMode.ADVENTURE, "gmaMessage");
            case "tpa":
                return handleTpaCommand(player, args);
            case "tpahere":
                return handleTpahereCommand(player, args);
            case "tpaccept":
                return handleTpAcceptCommand(player);
            case "tpdeny":
                return handleTpDenyCommand(player);
            case "msg":
            case "tell":
            case "w":
                return handleMessageCommand(player, args);
            case "r":
                return handleReplyCommand(player, args);
            default:
                return false;
        }
    }

    private boolean handleGameModeCommand(Player player, GameMode gameMode, String configMessageKey) {
        player.setGameMode(gameMode);
        String message = config.getString(configMessageKey, "Your game mode has been updated.");
        player.sendMessage(parseColoredMessage(message));
        return true;
    }

    private boolean handleTpaCommand(Player requester, String[] args) {
        if (args.length != 1) {
            requester.sendMessage(parseColoredMessage(config.getString("tpaUsage", "&cUsage: /tpa <player>")));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            requester.sendMessage(parseColoredMessage(config.getString("playerNotFound", "&cPlayer not found or not online.")));
            return true;
        }

        if (target.equals(requester)) {
            requester.sendMessage(parseColoredMessage(config.getString("cannotTeleportToSelf", "&cYou cannot send a teleport request to yourself!")));
            return true;
        }

        tpaRequests.put(target.getUniqueId(), requester.getUniqueId());
        requester.sendMessage(parseColoredMessage(config.getString("tpaRequestSent", "&aYou have sent a teleport request to %target%").replace("%target%", target.getName())));
        sendClickableRequest(target, requester, "tpa");
        return true;
    }

    private boolean handleTpahereCommand(Player requester, String[] args) {
        if (args.length != 1) {
            requester.sendMessage(parseColoredMessage(config.getString("tpahereUsage", "&cUsage: /tpahere <player>")));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            requester.sendMessage(parseColoredMessage(config.getString("playerNotFound", "&cPlayer not found or not online.")));
            return true;
        }

        if (target.equals(requester)) {
            requester.sendMessage(parseColoredMessage(config.getString("cannotTeleportToSelf", "&cYou cannot send a teleport request to yourself!")));
            return true;
        }

        tpahereRequests.put(target.getUniqueId(), requester.getUniqueId());
        requester.sendMessage(parseColoredMessage(config.getString("tpahereRequestSent", "&aYou have sent a teleport request for %target% to come to you.").replace("%target%", target.getName())));
        sendClickableRequest(target, requester, "tpahere");
        return true;
    }

    private void sendClickableRequest(Player target, Player requester, String requestType) {
        String message = config.getString(requestType + "RequestReceived", "&a%requester% has requested to teleport " + (requestType.equals("tpa") ? "to you" : "you to them") + ". Click to respond:")
                .replace("%requester%", requester.getName());

        Component baseComponent = parseColoredMessage(message);

        String acceptSymbol = config.getString("tpaAcceptSymbol", " ✔ ");
        String acceptSymbolColor = config.getString("tpaAcceptSymbolColor", "GREEN");
        String acceptHoverText = config.getString("tpaAcceptHoverText", "&aClick to accept");

        Component acceptComponent = Component.text(acceptSymbol)
                .color(parseTextColor(acceptSymbolColor, NamedTextColor.GREEN))
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/tpaccept"))
                .hoverEvent(HoverEvent.showText(parseColoredMessage(acceptHoverText)));

        String denySymbol = config.getString("tpaDenySymbol", " ✘ ");
        String denySymbolColor = config.getString("tpaDenySymbolColor", "RED");
        String denyHoverText = config.getString("tpaDenyHoverText", "&cClick to deny");

        Component denyComponent = Component.text(denySymbol)
                .color(parseTextColor(denySymbolColor, NamedTextColor.RED))
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/tpdeny"))
                .hoverEvent(HoverEvent.showText(parseColoredMessage(denyHoverText)));

        target.sendMessage(baseComponent.append(acceptComponent).append(denyComponent));
    }

    private TextColor parseTextColor(String colorName, TextColor defaultColor) {
        if (colorName.startsWith("#")) {
            try {
                return TextColor.fromHexString(colorName);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid hex color specified in config: " + colorName + ". Using default color.");
                return defaultColor;
            }
        } else {
            try {
                return NamedTextColor.NAMES.value(colorName.toLowerCase());
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid color name specified in config: " + colorName + ". Using default color.");
                return defaultColor;
            }
        }
    }

    private boolean handleTpAcceptCommand(Player target) {
        UUID requesterId = tpaRequests.remove(target.getUniqueId());
        boolean isTpaHere = false;
        if (requesterId == null) {
            requesterId = tpahereRequests.remove(target.getUniqueId());
            isTpaHere = true;
        }

        if (requesterId == null) {
            target.sendMessage(parseColoredMessage(config.getString("noRequestToAccept", "&cYou have no pending teleport requests to accept.")));
            return true;
        }

        Player requester = Bukkit.getPlayer(requesterId);
        if (requester == null || !requester.isOnline()) {
            target.sendMessage(parseColoredMessage(config.getString("playerNotFound", "&cPlayer not found or not online.")));
            return true;
        }

        if (isTpaHere) {
            target.teleport(requester.getLocation());
            requester.sendMessage(parseColoredMessage(config.getString("tpahereAccepted", "&aYour teleport request was accepted. %target% is teleporting to you.").replace("%target%", target.getName())));
            target.sendMessage(parseColoredMessage(config.getString("tpahereRequestAccepted", "&aYou accepted %requester%'s request to teleport to them.").replace("%requester%", requester.getName())));
        } else {
            requester.teleport(target.getLocation());
            requester.sendMessage(parseColoredMessage(config.getString("tpaAccepted", "&aYour teleport request was accepted. Teleporting to %target%.").replace("%target%", target.getName())));
            target.sendMessage(parseColoredMessage(config.getString("tpaRequestAccepted", "&aYou accepted %requester%'s teleport request.").replace("%requester%", requester.getName())));
        }

        return true;
    }

    private boolean handleTpDenyCommand(Player target) {
        UUID requesterId = tpaRequests.remove(target.getUniqueId());
        if (requesterId == null) {
            requesterId = tpahereRequests.remove(target.getUniqueId());
        }

        if (requesterId == null) {
            target.sendMessage(parseColoredMessage(config.getString("noRequestToDeny", "&cYou have no pending teleport requests to deny.")));
            return true;
        }

        Player requester = Bukkit.getPlayer(requesterId);
        String denyMessage = config.getString("tpRequestDenied", "&cYour teleport request was denied.");

        if (requester != null && requester.isOnline()) {
            requester.sendMessage(parseColoredMessage(denyMessage));
        }

        target.sendMessage(parseColoredMessage(config.getString("tpDenyConfirmation", "&aYou denied the teleport request.")));

        return true;
    }

    private boolean handleMessageCommand(Player sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(parseColoredMessage(config.getString("msgUsage", "&cUsage: /msg <player> <message>")));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(parseColoredMessage(config.getString("playerNotFound", "&cPlayer not found or not online.")));
            return true;
        }

        String message = String.join(" ", args).substring(args[0].length() + 1);
        sendPrivateMessage(sender, target, message);
        return true;
    }

    private boolean handleReplyCommand(Player sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(parseColoredMessage(config.getString("replyUsage", "&cUsage: /r <message>")));
            return true;
        }

        UUID lastSenderUUID = lastMessageSender.get(sender.getUniqueId());
        if (lastSenderUUID == null) {
            sender.sendMessage(parseColoredMessage(config.getString("noOneToReplyTo", "&cYou have no one to reply to.")));
            return true;
        }

        Player target = Bukkit.getPlayer(lastSenderUUID);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(parseColoredMessage(config.getString("playerNotFound", "&cPlayer not found or not online.")));
            return true;
        }

        String message = String.join(" ", args);
        sendPrivateMessage(sender, target, message);
        return true;
    }

    private void sendPrivateMessage(Player sender, Player recipient, String message) {
        String senderFormat = config.getString("msgFormatSender", "&7[&cme &7-> &c%recipient%&7] &f%message%");
        String recipientFormat = config.getString("msgFormatRecipient", "&7[&c%sender% &7-> &cme&7] &f%message%");

        sender.sendMessage(parseColoredMessage(senderFormat
                .replace("%recipient%", recipient.getName())
                .replace("%message%", message)));

        recipient.sendMessage(parseColoredMessage(recipientFormat
                .replace("%sender%", sender.getName())
                .replace("%message%", message)));

        lastMessageSender.put(recipient.getUniqueId(), sender.getUniqueId());
    }

    private Component parseColoredMessage(String message) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }
}