package ServerTools.commands;

import ServerTools.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import ServerTools.ServerTools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RulesCommand implements CommandExecutor {

    private final ServerTools plugin;
    private FileConfiguration rulesConfig;
    private String title;
    private int linesPerPage;
    private List<String> rules;

    public RulesCommand(ServerTools plugin) {
        this.plugin = plugin;
        loadRulesConfig();
    }

    private void loadRulesConfig() {
        rulesConfig = plugin.getCustomConfig("config/rules.yml");
        title = rulesConfig.getString("title", "<gold><bold>Server Rules");
        linesPerPage = rulesConfig.getInt("linesPerPage", 5);
        rules = rulesConfig.getStringList("rules");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        int page = 1;

        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage(MessageUtil.parseMessage(sender, "<red>Invalid page number.", null));
                return true;
            }
        }

        int totalPages = (int) Math.ceil((double) rules.size() / linesPerPage);

        if (page < 1 || page > totalPages) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%total_pages%", String.valueOf(totalPages));
            sender.sendMessage(MessageUtil.parseMessage(sender, "<red>Page not found. There are %total_pages% pages.", placeholders));
            return true;
        }

        sendRulesPage(sender, page, totalPages);
        return true;
    }

    private void sendRulesPage(CommandSender sender, int page, int totalPages) {
        // Send title
        Map<String, String> headerPlaceholders = new HashMap<>();
        headerPlaceholders.put("%page%", String.valueOf(page));
        headerPlaceholders.put("%total_pages%", String.valueOf(totalPages));
        sender.sendMessage(MessageUtil.parseMessage(sender, title + " <yellow>(Page %page% of %total_pages%)", headerPlaceholders));

        // Send rules
        int startIndex = (page - 1) * linesPerPage;
        int endIndex = Math.min(startIndex + linesPerPage, rules.size());

        for (int i = startIndex; i < endIndex; i++) {
            sender.sendMessage(MessageUtil.parseMessage(sender, rules.get(i), null));
        }

        // Add navigation buttons
        if (sender instanceof Player) {
            StringBuilder footerBuilder = new StringBuilder();

            if (page > 1) {
                footerBuilder.append("<green>[<click:run_command:'/rules ").append(page - 1).append("'>Previous</click>]");
            }

            if (page < totalPages) {
                if (page > 1) {
                    footerBuilder.append(" ");
                }
                footerBuilder.append("<green>[<click:run_command:'/rules ").append(page + 1).append("'>Next</click>]");
            }

            if (footerBuilder.length() > 0) {
                sender.sendMessage(MessageUtil.parseMessage(sender, footerBuilder.toString(), null));
            }
        }
    }
}