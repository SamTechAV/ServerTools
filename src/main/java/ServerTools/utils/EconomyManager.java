package ServerTools.utils;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class EconomyManager {

    private final File economyFile;
    private final FileConfiguration economyConfig;

    public EconomyManager(File economyFile) {
        this.economyFile = economyFile;
        if (!economyFile.exists()) {
            try {
                economyFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.economyConfig = YamlConfiguration.loadConfiguration(economyFile);
    }

    public void initializePlayer(Player player) {
        if (!economyConfig.contains(player.getUniqueId().toString())) {
            economyConfig.set(player.getUniqueId().toString(), 0.0);
            saveEconomyConfig();
        }
    }

    public double getBalance(OfflinePlayer player) {
        return economyConfig.getDouble(player.getUniqueId().toString(), 0.0);
    }

    public boolean withdrawMoney(OfflinePlayer player, double amount) {
        double balance = getBalance(player);
        if (balance >= amount) {
            economyConfig.set(player.getUniqueId().toString(), balance - amount);
            saveEconomyConfig();
            return true;
        }
        return false;
    }

    public void depositMoney(OfflinePlayer player, double amount) {
        double balance = getBalance(player);
        economyConfig.set(player.getUniqueId().toString(), balance + amount);
        saveEconomyConfig();
    }

    public List<Map.Entry<UUID, Double>> getTopBalances(int limit) {
        return economyConfig.getKeys(false).stream()
                .map(key -> new AbstractMap.SimpleEntry<>(UUID.fromString(key), economyConfig.getDouble(key)))
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private void saveEconomyConfig() {
        try {
            economyConfig.save(economyFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Add this method to match the call in ServerTools
    public void saveData() {
        saveEconomyConfig();
    }
}