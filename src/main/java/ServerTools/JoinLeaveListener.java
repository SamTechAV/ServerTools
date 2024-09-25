package ServerTools;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ServerTools.ServerTools;
import ServerTools.utils.EconomyManager;
import ServerTools.utils.MessageUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JoinLeaveListener implements Listener {

    private final ServerTools plugin;
    private final EconomyManager economyManager;
    private final MiniMessage miniMessage;

    private static final Pattern URL_PATTERN = Pattern.compile("(https?://\\S+)");
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");

    public JoinLeaveListener(ServerTools plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        this.miniMessage = MiniMessage.miniMessage();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        economyManager.initializePlayer(event.getPlayer());

        String joinMessage = plugin.getConfig().getString("joinandleave.joinMessage", "<green>%player_name% has joined the server!");
        Component parsedMessage = parseMessage(joinMessage, event.getPlayer().getName());
        event.joinMessage(parsedMessage);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String quitMessage = plugin.getConfig().getString("joinandleave.quitMessage", "<red>%player_name% has left the server.");
        Component parsedMessage = parseMessage(quitMessage, event.getPlayer().getName());
        event.quitMessage(parsedMessage);
    }

    private Component parseMessage(String message, String playerName) {
        message = message.replace("%player_name%", playerName);
        message = convertUrlsToClickableLinks(message);
        return MessageUtil.parseMessage(null, message);
    }

    private String convertUrlsToClickableLinks(String message) {
        StringBuilder result = new StringBuilder();
        int lastIndex = 0;
        Matcher matcher = URL_PATTERN.matcher(message);

        while (matcher.find()) {
            String textBefore = message.substring(lastIndex, matcher.start());
            result.append(preserveTags(textBefore));

            String url = matcher.group(1);
            String replacement = String.format("<click:open_url:'%s'><hover:show_text:'Click to open'><gradient:blue:aqua>%s</gradient></hover></click>", url, url);
            result.append(replacement);

            lastIndex = matcher.end();
        }

        if (lastIndex < message.length()) {
            result.append(preserveTags(message.substring(lastIndex)));
        }

        return result.toString();
    }

    private String preserveTags(String text) {
        StringBuilder result = new StringBuilder();
        int lastIndex = 0;
        Matcher matcher = TAG_PATTERN.matcher(text);

        while (matcher.find()) {
            String textBefore = text.substring(lastIndex, matcher.start());
            result.append(textBefore.replace("|", "\\|"));
            result.append(matcher.group());
            lastIndex = matcher.end();
        }

        if (lastIndex < text.length()) {
            result.append(text.substring(lastIndex).replace("|", "\\|"));
        }

        return result.toString();
    }
}