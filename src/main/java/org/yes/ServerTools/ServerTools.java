package org.yes.ServerTools;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ServerTools extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private final Map<UUID, UUID> tpaRequests = new HashMap<>();
    private final Map<UUID, UUID> tpahereRequests = new HashMap<>();
    private final Map<UUID, UUID> lastMessageSender = new HashMap<>();
    private File economyFile;
    private FileConfiguration economyConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        getServer().getPluginManager().registerEvents(this, this);

        // Initialize economy
        economyFile = new File(getDataFolder(), "economy.yml");
        if (!economyFile.exists()) {
            saveResource("economy.yml", false);
        }
        economyConfig = YamlConfiguration.loadConfiguration(economyFile);

        getLogger().info("ServerTools with TPA system, clickable chat interactions, direct messaging, and integrated economy features enabled.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String joinMessage = config.getString("joinMessage", "&a%player_name% has joined the server!");
        joinMessage = joinMessage.replace("%player_name%", player.getName());
        event.joinMessage(parseColoredMessage(joinMessage));

        // Initialize player's balance if not exists
        if (!economyConfig.contains(player.getUniqueId().toString())) {
            economyConfig.set(player.getUniqueId().toString(), 0.0);
            saveEconomyConfig();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String quitMessage = config.getString("quitMessage", "&c%player_name% has left the server.");
        quitMessage = quitMessage.replace("%player_name%", player.getName());
        event.quitMessage(parseColoredMessage(quitMessage));
        tpaRequests.remove(player.getUniqueId());
        tpahereRequests.remove(player.getUniqueId());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) && !command.getName().equalsIgnoreCase("addmoney") && !command.getName().equalsIgnoreCase("removemoney")) {
            sender.sendMessage(Component.text("This command can only be used by players!").color(NamedTextColor.RED));
            return true;
        }

        Player player = (sender instanceof Player) ? (Player) sender : null;
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
            case "pay":
                return handlePayCommand(player, args);
            case "balance":
            case "bal":
                return handleBalanceCommand(player);
            case "withdraw":
                return handleWithdrawCommand(player, args);
            case "baltop":
                return handleBalTopCommand(sender);
            case "addmoney":
                return handleAddMoneyCommand(sender, args);
            case "removemoney":
                return handleRemoveMoneyCommand(sender, args);
            default:
                return false;
        }
    }

    private boolean handleRemoveMoneyCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("servertools.removemoney")) {
            sender.sendMessage(parseColoredMessage(config.getString("noPermission", "&cYou don't have permission to use this command.")));
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(parseColoredMessage(config.getString("removeMoneyUsage", "&cUsage: /removemoney <player> <amount>")));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(parseColoredMessage(config.getString("playerNotFound", "&cPlayer not found or not online.")));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(parseColoredMessage(config.getString("invalidAmount", "&cInvalid amount. Please enter a valid number.")));
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage(parseColoredMessage(config.getString("invalidAmount", "&cInvalid amount. Please enter a positive number.")));
            return true;
        }

        if (withdrawMoney(target, amount)) {
            sender.sendMessage(parseColoredMessage(config.getString("moneyRemoved", "&aRemoved %amount% from %player%'s balance.")
                    .replace("%amount%", String.format("%.2f", amount))
                    .replace("%player%", target.getName())));
            target.sendMessage(parseColoredMessage(config.getString("moneyDeducted", "&a%amount% has been deducted from your balance.")
                    .replace("%amount%", String.format("%.2f", amount))));
        } else {
            sender.sendMessage(parseColoredMessage(config.getString("insufficientFunds", "&c%player% doesn't have enough money.")
                    .replace("%player%", target.getName())));
        }

        return true;
    }

    private boolean handleBalTopCommand(CommandSender sender) {
        List<Map.Entry<UUID, Double>> topBalances = economyConfig.getKeys(false).stream()
                .map(key -> new AbstractMap.SimpleEntry<>(UUID.fromString(key), economyConfig.getDouble(key)))
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        sender.sendMessage(parseColoredMessage("&6Top 10 Balances:"));
        for (int i = 0; i < topBalances.size(); i++) {
            UUID playerUUID = topBalances.get(i).getKey();
            double balance = topBalances.get(i).getValue();
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
            String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
            sender.sendMessage(parseColoredMessage(String.format("&e%d. &f%s: &a$%.2f", i + 1, playerName, balance)));
        }
        return true;
    }

    private boolean handleAddMoneyCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("servertools.addmoney")) {
            sender.sendMessage(parseColoredMessage(config.getString("noPermission", "&cYou don't have permission to use this command.")));
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(parseColoredMessage(config.getString("addMoneyUsage", "&cUsage: /addmoney <player> <amount>")));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(parseColoredMessage(config.getString("playerNotFound", "&cPlayer not found or not online.")));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(parseColoredMessage(config.getString("invalidAmount", "&cInvalid amount. Please enter a valid number.")));
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage(parseColoredMessage(config.getString("invalidAmount", "&cInvalid amount. Please enter a positive number.")));
            return true;
        }

        depositMoney(target, amount);
        sender.sendMessage(parseColoredMessage(config.getString("moneyAdded", "&aAdded %amount% to %player%'s balance.")
                .replace("%amount%", String.format("%.2f", amount))
                .replace("%player%", target.getName())));
        target.sendMessage(parseColoredMessage(config.getString("moneyReceived", "&a%amount% has been added to your balance.")
                .replace("%amount%", String.format("%.2f", amount))));

        return true;
    }

    private boolean handlePayCommand(Player sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(parseColoredMessage(config.getString("payUsage", "&cUsage: /pay <player> <amount>")));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(parseColoredMessage(config.getString("playerNotFound", "&cPlayer not found or not online.")));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(parseColoredMessage(config.getString("invalidAmount", "&cInvalid amount. Please enter a valid number.")));
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage(parseColoredMessage(config.getString("invalidAmount", "&cInvalid amount. Please enter a positive number.")));
            return true;
        }

        if (withdrawMoney(sender, amount)) {
            depositMoney(target, amount);
            sender.sendMessage(parseColoredMessage(config.getString("paymentSent", "&aYou sent %amount% to %player%.")
                    .replace("%amount%", String.format("%.2f", amount))
                    .replace("%player%", target.getName())));
            target.sendMessage(parseColoredMessage(config.getString("paymentReceived", "&aYou received %amount% from %player%.")
                    .replace("%amount%", String.format("%.2f", amount))
                    .replace("%player%", sender.getName())));
        } else {
            sender.sendMessage(parseColoredMessage(config.getString("insufficientFunds", "&cYou don't have enough money to make this payment.")));
        }

        return true;
    }

    private boolean handleBalanceCommand(Player player) {
        double balance = getBalance(player);
        player.sendMessage(parseColoredMessage(config.getString("balanceMessage", "&aYour balance: %balance%")
                .replace("%balance%", String.format("%.2f", balance))));
        return true;
    }

    private boolean handleWithdrawCommand(Player player, String[] args) {
        if (args.length != 1) {
            player.sendMessage(parseColoredMessage(config.getString("withdrawUsage", "&cUsage: /withdraw <amount>")));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(parseColoredMessage(config.getString("invalidAmount", "&cInvalid amount. Please enter a valid number.")));
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(parseColoredMessage(config.getString("invalidAmount", "&cInvalid amount. Please enter a positive number.")));
            return true;
        }

        if (withdrawMoney(player, amount)) {
            player.sendMessage(parseColoredMessage(config.getString("withdrawSuccess", "&aYou withdrew %amount%. New balance: %balance%")
                    .replace("%amount%", String.format("%.2f", amount))
                    .replace("%balance%", String.format("%.2f", getBalance(player)))));
            // Here you would typically give the player a physical item representing the withdrawn money
            // This could be a custom item, or you might integrate with another plugin that handles physical currency
        } else {
            player.sendMessage(parseColoredMessage(config.getString("insufficientFunds", "&cYou don't have enough money to withdraw this amount.")));
        }

        return true;
    }

    // Economy methods
    private double getBalance(OfflinePlayer player) {
        return economyConfig.getDouble(player.getUniqueId().toString(), 0.0);
    }

    private boolean withdrawMoney(OfflinePlayer player, double amount) {
        double balance = getBalance(player);
        if (balance >= amount) {
            economyConfig.set(player.getUniqueId().toString(), balance - amount);
            saveEconomyConfig();
            return true;
        }
        return false;
    }

    private void depositMoney(OfflinePlayer player, double amount) {
        double balance = getBalance(player);
        economyConfig.set(player.getUniqueId().toString(), balance + amount);
        saveEconomyConfig();
    }

    private void saveEconomyConfig() {
        try {
            economyConfig.save(economyFile);
        } catch (IOException e) {
            getLogger().severe("Could not save economy data!");
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