package ServerTools.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection; // Don't forget this import
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import ServerTools.ServerTools;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class HomeManager {

    private final ServerTools plugin;
    private final File homesFile;
    private final FileConfiguration homesConfig;

    public HomeManager(ServerTools plugin) {
        this.plugin = plugin;

        // Define the file path where homes.yml will be stored
        File storageDir = new File(plugin.getDataFolder(), "storage");
        if (!storageDir.exists()) {
            storageDir.mkdirs(); // Create the directory if it doesn't exist
        }

        homesFile = new File(storageDir, "homes.yml");
        if (!homesFile.exists()) {
            try {
                homesFile.createNewFile(); // Create the file if it doesn't exist
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create homes.yml file!");
                e.printStackTrace();
            }
        }
        homesConfig = YamlConfiguration.loadConfiguration(homesFile);
    }

    public void setHome(Player player, String homeName, Location location) {
        Map<String, Location> homes = getHomes(player);
        homes.put(homeName.toLowerCase(), location);
        saveHomes(player.getUniqueId(), homes);
    }

    public Location getHome(Player player, String homeName) {
        Map<String, Location> homes = getHomes(player);
        return homes.get(homeName.toLowerCase());
    }

    public void deleteHome(Player player, String homeName) {
        Map<String, Location> homes = getHomes(player);
        homes.remove(homeName.toLowerCase());
        saveHomes(player.getUniqueId(), homes);
    }

    public boolean hasHome(Player player, String homeName) {
        Map<String, Location> homes = getHomes(player);
        return homes.containsKey(homeName.toLowerCase());
    }

    public Map<String, Location> getHomes(Player player) {
        String path = player.getUniqueId().toString();
        if (!homesConfig.contains(path)) {
            return new HashMap<>();
        }

        Map<String, Location> homes = new HashMap<>();
        ConfigurationSection homesSection = homesConfig.getConfigurationSection(path);
        for (String homeName : homesSection.getKeys(false)) {
            String worldName = homesConfig.getString(path + "." + homeName + ".world");
            double x = homesConfig.getDouble(path + "." + homeName + ".x");
            double y = homesConfig.getDouble(path + "." + homeName + ".y");
            double z = homesConfig.getDouble(path + "." + homeName + ".z");
            float yaw = (float) homesConfig.getDouble(path + "." + homeName + ".yaw");
            float pitch = (float) homesConfig.getDouble(path + "." + homeName + ".pitch");

            Location loc = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
            homes.put(homeName.toLowerCase(), loc);
        }

        return homes;
    }

    private void saveHomes(UUID uuid, Map<String, Location> homes) {
        String path = uuid.toString();
        homesConfig.set(path, null); // Clear existing homes

        for (Map.Entry<String, Location> entry : homes.entrySet()) {
            String homeName = entry.getKey();
            Location loc = entry.getValue();

            homesConfig.set(path + "." + homeName + ".world", loc.getWorld().getName());
            homesConfig.set(path + "." + homeName + ".x", loc.getX());
            homesConfig.set(path + "." + homeName + ".y", loc.getY());
            homesConfig.set(path + "." + homeName + ".z", loc.getZ());
            homesConfig.set(path + "." + homeName + ".yaw", loc.getYaw());
            homesConfig.set(path + "." + homeName + ".pitch", loc.getPitch());
        }

        saveHomesConfig();
    }

    public int getMaxHomes(Player player) {
        FileConfiguration config = plugin.getConfig();
        int defaultMax = config.getInt("home.maxHomes", 3);

        // Check for per-permission limits
        ConfigurationSection limitsSection = config.getConfigurationSection("home.homeLimits");
        if (limitsSection != null) {
            for (String permission : limitsSection.getKeys(false)) {
                if (player.hasPermission(permission)) {
                    return limitsSection.getInt(permission, defaultMax);
                }
            }
        }

        return defaultMax;
    }

    private void saveHomesConfig() {
        try {
            homesConfig.save(homesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save homes.yml!");
            e.printStackTrace();
        }
    }
}
