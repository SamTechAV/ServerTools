package org.yes.ServerTools.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class MessageUtil {

    public static Component parseMessage(CommandSender sender, String message, Map<String, String> customPlaceholders) {
        String parsedMessage;

        // Process PlaceholderAPI placeholders
        if (sender instanceof Player) {
            Player player = (Player) sender;
            parsedMessage = PlaceholderAPI.setPlaceholders(player, message);
        } else {
            parsedMessage = PlaceholderAPI.setPlaceholders(null, message);
        }

        // Replace custom placeholders
        if (customPlaceholders != null) {
            for (Map.Entry<String, String> entry : customPlaceholders.entrySet()) {
                parsedMessage = parsedMessage.replace(entry.getKey(), entry.getValue());
            }
        }

        // Apply color codes
        return LegacyComponentSerializer.legacyAmpersand().deserialize(parsedMessage);
    }

    public static TextColor parseTextColor(String colorName) {
        if (colorName.startsWith("#")) {
            try {
                return TextColor.fromHexString(colorName);
            } catch (IllegalArgumentException e) {
                return NamedTextColor.WHITE;
            }
        } else {
            try {
                return NamedTextColor.NAMES.value(colorName.toLowerCase());
            } catch (IllegalArgumentException e) {
                return NamedTextColor.WHITE;
            }
        }
    }
}
