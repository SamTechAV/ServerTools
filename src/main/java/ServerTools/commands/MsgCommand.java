package org.yes.ServerTools.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.yes.ServerTools.ServerTools;
import org.yes.ServerTools.utils.MessageUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MsgCommand implements CommandExecutor {

    private final ServerTools plugin;
    private final FileConfiguration config;
    private final Map<UUID, UUID> lastMessageSender = new HashMap<>();

    public MsgCommand(ServerTools plugin) {
        this.plugin = plugin;
        this.config = plugin.getPluginConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.parseMessage(sender, "&cThis command can only be used by players!", null));
            return true;
        }

        Player player = (Player) sender;
        String commandName = command.getName().toLowerCase();

        switch (commandName) {
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

    private boolean handleMessageCommand(Player sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("directmessaging.msgUsage", "&cUsage: /msg <player> <message>"), null));
            return true;
        }

        Player recipient = Bukkit.getPlayer(args[0]);
        if (recipient == null || !recipient.isOnline()) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.playerNotFound", "&cPlayer not found or not online."), null));
            return true;
        }

        String messageContent = String.join(" ", args).substring(args[0].length() + 1);
        sendPrivateMessage(sender, recipient, messageContent);
        return true;
    }

    private boolean handleReplyCommand(Player sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("directmessaging.replyUsage", "&cUsage: /r <message>"), null));
            return true;
        }

        UUID lastSenderUUID = lastMessageSender.get(sender.getUniqueId());
        if (lastSenderUUID == null) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("directmessaging.noOneToReplyTo", "&cYou have no one to reply to."), null));
            return true;
        }

        Player recipient = Bukkit.getPlayer(lastSenderUUID);
        if (recipient == null || !recipient.isOnline()) {
            sender.sendMessage(MessageUtil.parseMessage(sender, config.getString("economy.playerNotFound", "&cPlayer not found or not online."), null));
            return true;
        }

        String messageContent = String.join(" ", args);
        sendPrivateMessage(sender, recipient, messageContent);
        return true;
    }

    private void sendPrivateMessage(Player sender, Player recipient, String messageContent) {
        String senderFormat = config.getString("directmessaging.msgFormatSender", "&7[&cme &7-> &c%recipient%&7] &f%message%");
        String recipientFormat = config.getString("directmessaging.msgFormatRecipient", "&7[&c%sender% &7-> &cme&7] &f%message%");

        Map<String, String> senderPlaceholders = new HashMap<>();
        senderPlaceholders.put("%recipient%", recipient.getName());
        senderPlaceholders.put("%message%", messageContent);

        Map<String, String> recipientPlaceholders = new HashMap<>();
        recipientPlaceholders.put("%sender%", sender.getName());
        recipientPlaceholders.put("%message%", messageContent);

        sender.sendMessage(MessageUtil.parseMessage(sender, senderFormat, senderPlaceholders));
        recipient.sendMessage(MessageUtil.parseMessage(recipient, recipientFormat, recipientPlaceholders));

        lastMessageSender.put(recipient.getUniqueId(), sender.getUniqueId());
    }
}
