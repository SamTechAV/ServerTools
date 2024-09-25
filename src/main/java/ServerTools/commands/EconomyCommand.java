package org.yes.ServerTools.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.yes.ServerTools.ServerTools;
import org.yes.ServerTools.utils.EconomyManager;
import org.yes.ServerTools.utils.MessageUtil;
import org.yes.ServerTools.utils.BankNoteManager;

import java.util.*;

public class EconomyCommand implements CommandExecutor {

    private final ServerTools plugin;
    private final FileConfiguration config;
    private final EconomyManager economyManager;
    private final BankNoteManager bankNoteManager;

    public EconomyCommand(ServerTools plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.economyManager = plugin.getEconomyManager();
        this.bankNoteManager = new BankNoteManager(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName = command.getName().toLowerCase();

        switch (commandName) {
            case "balance":
            case "bal":
                return handleBalanceCommand(sender, args);
            case "pay":
                return handlePayCommand(sender, args);
            case "withdraw":
                return handleWithdrawCommand(sender, args);
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

    private boolean handleBalanceCommand(CommandSender sender, String[] args) {
        if (args.length > 1) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.balanceUsage"), null));
            return true;
        }

        OfflinePlayer target;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.playerOnly"), null));
                return true;
            }
            target = (Player) sender;
        } else {
            target = Bukkit.getOfflinePlayer(args[0]);
        }

        double balance = economyManager.getBalance(target);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%player%", target.getName());
        placeholders.put("%balance%", String.format("%.2f", balance));

        sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.balanceMessage"), placeholders));
        return true;
    }

    private boolean handlePayCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.playerOnly"), null));
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.payUsage"), null));
            return true;
        }

        Player payer = (Player) sender;
        Player recipient = Bukkit.getPlayer(args[0]);
        if (recipient == null || !recipient.isOnline()) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.playerNotFound"), null));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.invalidAmount"), null));
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.invalidAmount"), null));
            return true;
        }

        if (economyManager.withdrawMoney(payer, amount)) {
            economyManager.depositMoney(recipient, amount);

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%amount%", String.format("%.2f", amount));
            placeholders.put("%player%", recipient.getName());

            payer.sendMessage(MessageUtil.parseMessage(payer, config.getString("economy.paymentSent"), placeholders));

            placeholders.put("%player%", payer.getName());
            recipient.sendMessage(MessageUtil.parseMessage(recipient, config.getString("economy.paymentReceived"), placeholders));
        } else {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.insufficientFunds"), null));
        }

        return true;
    }

    private boolean handleWithdrawCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.playerOnly"), null));
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.withdrawUsage"), null));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.invalidAmount"), null));
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.invalidAmount"), null));
            return true;
        }

        if (economyManager.withdrawMoney(player, amount)) {
            ItemStack bankNote = bankNoteManager.createBankNote(amount);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%amount%", String.format("%.2f", amount));
            placeholders.put("%balance%", String.format("%.2f", economyManager.getBalance(player)));

            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(bankNote);
                sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.withdrawSuccess"), placeholders));
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), bankNote);
                sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.withdrawSuccess") + " The bank note was dropped at your feet.", placeholders));
            }
        } else {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.insufficientFunds"), null));
        }

        return true;
    }

    private boolean handleBalTopCommand(CommandSender sender) {
        List<Map.Entry<UUID, Double>> topBalances = economyManager.getTopBalances(10);

        sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.balTopHeader"), null));

        int position = 1;
        for (Map.Entry<UUID, Double> entry : topBalances) {
            UUID uuid = entry.getKey();
            double balance = entry.getValue();
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%position%", String.valueOf(position));
            placeholders.put("%player%", playerName);
            placeholders.put("%balance%", String.format("%.2f", balance));

            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.balTopEntry"), placeholders));

            position++;
        }

        return true;
    }

    private boolean handleAddMoneyCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("servertools.addmoney")) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.noPermission"), null));
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.addMoneyUsage"), null));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.invalidAmount"), null));
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.invalidAmount"), null));
            return true;
        }

        economyManager.depositMoney(target, amount);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%amount%", String.format("%.2f", amount));
        placeholders.put("%player%", target.getName());

        sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.moneyAdded"), placeholders));

        if (target.isOnline()) {
            ((Player) target).sendMessage(MessageUtil.parseMessage((Player) target, config.getString("economy.moneyReceived"), placeholders));
        }

        return true;
    }

    private boolean handleRemoveMoneyCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("servertools.removemoney")) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.noPermission"), null));
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.removeMoneyUsage"), null));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.invalidAmount"), null));
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.invalidAmount"), null));
            return true;
        }

        if (economyManager.withdrawMoney(target, amount)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%amount%", String.format("%.2f", amount));
            placeholders.put("%player%", target.getName());

            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.moneyRemoved"), placeholders));

            if (target.isOnline()) {
                ((Player) target).sendMessage(MessageUtil.parseMessage((Player) target, config.getString("economy.moneyDeducted"), placeholders));
            }
        } else {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.insufficientFunds"), null));
        }

        return true;
    }
}