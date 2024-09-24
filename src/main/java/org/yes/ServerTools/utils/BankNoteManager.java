package org.yes.ServerTools.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.yes.ServerTools.ServerTools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BankNoteManager {

    private final ServerTools plugin;
    private final FileConfiguration config;
    private final NamespacedKey amountKey;

    public BankNoteManager(ServerTools plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.amountKey = new NamespacedKey(plugin, "banknote_amount");
    }

    public ItemStack createBankNote(double amount) {
        ItemStack bankNote = new ItemStack(Material.PAPER);
        ItemMeta meta = bankNote.getItemMeta();

        String name = config.getString("economy.banknote.name", "Bank Note");
        String description = config.getString("economy.banknote.description", "Right-click to deposit");
        List<String> lore = config.getStringList("economy.banknote.lore");
        String currency = config.getString("economy.banknote.currency", "Dollar");

        Component displayName = MessageUtil.parseMessage(null, name.replace("%currency%", currency).replace("%amount%", String.format("%.2f", amount)));
        meta.displayName(displayName);

        List<Component> formattedLore = new ArrayList<>();
        formattedLore.add(MessageUtil.parseMessage(null, description.replace("%amount%", String.format("%.2f", amount))));
        for (String loreLine : lore) {
            formattedLore.add(MessageUtil.parseMessage(null, loreLine.replace("%amount%", String.format("%.2f", amount))));
        }
        meta.lore(formattedLore);

        meta.getPersistentDataContainer().set(amountKey, PersistentDataType.DOUBLE, amount);
        bankNote.setItemMeta(meta);

        return bankNote;
    }

    public boolean isBankNote(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(amountKey, PersistentDataType.DOUBLE);
    }

    public double getBankNoteAmount(ItemStack item) {
        if (!isBankNote(item)) return 0;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(amountKey, PersistentDataType.DOUBLE);
    }

    public void depositBankNote(Player player, ItemStack bankNote) {
        if (!isBankNote(bankNote)) return;

        double amount = getBankNoteAmount(bankNote);
        EconomyManager economyManager = plugin.getEconomyManager();

        economyManager.depositMoney(player, amount);
        player.getInventory().removeItem(bankNote);

        Map<String, String> placeholders = Map.of("%amount%", String.format("%.2f", amount));
        player.sendMessage(MessageUtil.parseMessage(player, config.getString("economy.banknote.depositSuccess"), placeholders));
    }
}