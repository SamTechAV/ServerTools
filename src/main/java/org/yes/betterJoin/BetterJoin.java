package org.yes.betterJoin;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

public class BetterJoin extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private final Map<UUID, UUID> tpaRequests = new HashMap<>();
    private final Map<UUID, UUID> tpahereRequests = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("BetterJoin with TPA system and clickable chat interactions enabled.");

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
        event.setJoinMessage(ChatColor.translateAlternateColorCodes('&', joinMessage));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String quitMessage = config.getString("quitMessage", "&c%player_name% has left the server.");
        quitMessage = replacePlaceholders(player, quitMessage);
        event.setQuitMessage(ChatColor.translateAlternateColorCodes('&', quitMessage));
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
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
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
            default:
                return false;
        }
    }

    private boolean handleGameModeCommand(Player player, GameMode gameMode, String configMessageKey) {
        player.setGameMode(gameMode);
        String message = config.getString(configMessageKey, "&aYour game mode has been updated.");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        return true;
    }

    private boolean handleTpaCommand(Player requester, String[] args) {
        if (args.length != 1) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("tpaUsage", "&cUsage: /tpa <player>")));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("playerNotFound", "&cPlayer not found or not online.")));
            return true;
        }

        if (target.equals(requester)) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("cannotTeleportToSelf", "&cYou cannot send a teleport request to yourself!")));
            return true;
        }

        tpaRequests.put(target.getUniqueId(), requester.getUniqueId());
        requester.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("tpaRequestSent", "&aYou have sent a teleport request to %target%").replace("%target%", target.getName())));
        sendClickableRequest(target, requester, "tpa");
        return true;
    }

    private boolean handleTpahereCommand(Player requester, String[] args) {
        if (args.length != 1) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("tpahereUsage", "&cUsage: /tpahere <player>")));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("playerNotFound", "&cPlayer not found or not online.")));
            return true;
        }

        if (target.equals(requester)) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("cannotTeleportToSelf", "&cYou cannot send a teleport request to yourself!")));
            return true;
        }

        tpahereRequests.put(target.getUniqueId(), requester.getUniqueId());
        requester.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("tpahereRequestSent", "&aYou have sent a teleport request for %target% to come to you.").replace("%target%", target.getName())));
        sendClickableRequest(target, requester, "tpahere");
        return true;
    }

    private void sendClickableRequest(Player target, Player requester, String requestType) {
        String message = ChatColor.translateAlternateColorCodes('&',
                config.getString(requestType + "RequestReceived", "&a%requester% has requested to teleport " + (requestType.equals("tpa") ? "to you" : "you to them") + ". Click to respond:")
                        .replace("%requester%", requester.getName()));

        TextComponent baseComponent = new TextComponent(message);

        // Fetch accept symbol, color, and hover text from config
        String acceptSymbol = ChatColor.translateAlternateColorCodes('&', config.getString("tpaAcceptSymbol", " ✔ "));
        String acceptSymbolColor = config.getString("tpaAcceptSymbolColor", "GREEN");
        String acceptHoverText = ChatColor.translateAlternateColorCodes('&', config.getString("tpaAcceptHoverText", "Click to accept"));

        // Create accept component
        TextComponent acceptComponent = new TextComponent(acceptSymbol);
        acceptComponent.setColor(parseChatColor(acceptSymbolColor, net.md_5.bungee.api.ChatColor.GREEN));
        acceptComponent.setBold(true);
        acceptComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept"));
        acceptComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(acceptHoverText).create()));

        // Fetch deny symbol, color, and hover text from config
        String denySymbol = ChatColor.translateAlternateColorCodes('&', config.getString("tpaDenySymbol", " ✘ "));
        String denySymbolColor = config.getString("tpaDenySymbolColor", "RED");
        String denyHoverText = ChatColor.translateAlternateColorCodes('&', config.getString("tpaDenyHoverText", "Click to deny"));

        // Create deny component
        TextComponent denyComponent = new TextComponent(denySymbol);
        denyComponent.setColor(parseChatColor(denySymbolColor, net.md_5.bungee.api.ChatColor.RED));
        denyComponent.setBold(true);
        denyComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny"));
        denyComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(denyHoverText).create()));

        baseComponent.addExtra(acceptComponent);
        baseComponent.addExtra(denyComponent);

        target.spigot().sendMessage(baseComponent);
    }

    private net.md_5.bungee.api.ChatColor parseChatColor(String colorName, net.md_5.bungee.api.ChatColor defaultColor) {
        try {
            return net.md_5.bungee.api.ChatColor.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid color specified in config: " + colorName + ". Using default color.");
            return defaultColor;
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
            target.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("noRequestToAccept", "&cYou have no pending teleport requests to accept.")));
            return true;
        }

        Player requester = Bukkit.getPlayer(requesterId);
        if (requester == null || !requester.isOnline()) {
            target.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("playerNotFound", "&cPlayer not found or not online.")));
            return true;
        }

        if (isTpaHere) {
            target.teleport(requester.getLocation());
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("tpahereAccepted", "&aYour teleport request was accepted. %target% is teleporting to you.").replace("%target%", target.getName())));
            target.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("tpahereRequestAccepted", "&aYou accepted %requester%'s request to teleport to them.").replace("%requester%", requester.getName())));
        } else {
            requester.teleport(target.getLocation());
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("tpaAccepted", "&aYour teleport request was accepted. Teleporting to %target%.").replace("%target%", target.getName())));
            target.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("tpaRequestAccepted", "&aYou accepted %requester%'s teleport request.").replace("%requester%", requester.getName())));
        }

        return true;
    }

    private boolean handleTpDenyCommand(Player target) {
        UUID requesterId = tpaRequests.remove(target.getUniqueId());
        if (requesterId == null) {
            requesterId = tpahereRequests.remove(target.getUniqueId());
        }

        if (requesterId == null) {
            target.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("noRequestToDeny", "&cYou have no pending teleport requests to deny.")));
            return true;
        }

        Player requester = Bukkit.getPlayer(requesterId);
        String denyMessage = ChatColor.translateAlternateColorCodes('&', config.getString("tpRequestDenied", "&cYour teleport request was denied."));

        if (requester != null && requester.isOnline()) {
            requester.sendMessage(denyMessage);
        }

        target.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("tpDenyConfirmation", "&aYou denied the teleport request.")));

        return true;
    }
}
