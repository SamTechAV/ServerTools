package org.yes.ServerTools.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder().build();

    public static Component parseMessage(CommandSender sender, String message, Map<String, String> customPlaceholders) {
        if (message == null) {
            return Component.empty();
        }

        // Process PlaceholderAPI placeholders
        if (sender instanceof Player) {
            message = PlaceholderAPI.setPlaceholders((Player) sender, message);
        } else {
            message = PlaceholderAPI.setPlaceholders(null, message);
        }

        // Replace custom placeholders
        if (customPlaceholders != null) {
            for (Map.Entry<String, String> entry : customPlaceholders.entrySet()) {
                message = message.replace(entry.getKey(), entry.getValue());
            }
        }

        // Convert '&' color codes and hex codes to MiniMessage format
        message = convertToMiniMessage(message);

        // Parse with MiniMessage
        return MINI_MESSAGE.deserialize(message);
    }

    public static Component parseMessage(CommandSender sender, String message) {
        return parseMessage(sender, message, null);
    }

    private static String convertToMiniMessage(String message) {
        // Convert '&' color codes to MiniMessage format
        message = LEGACY_SERIALIZER.serialize(LEGACY_SERIALIZER.deserialize(message));

        // Convert hex color codes to MiniMessage format
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<#" + matcher.group(1) + ">");
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    public static TextColor parseTextColor(String colorName) {
        if (colorName == null || colorName.isEmpty()) {
            return TextColor.color(0xFFFFFF); // White
        }

        if (colorName.startsWith("#")) {
            try {
                return TextColor.fromHexString(colorName);
            } catch (IllegalArgumentException e) {
                return TextColor.color(0xFFFFFF); // White
            }
        } else {
            try {
                return TextColor.fromCSSHexString("#" + colorName);
            } catch (IllegalArgumentException e) {
                return TextColor.color(0xFFFFFF); // White
            }
        }
    }
}