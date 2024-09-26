package ServerTools.utils;

import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import ServerTools.ServerTools;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LegacyCombatManager implements Listener {

    private final ServerTools plugin;
    private final Set<UUID> modifiedPlayers;
    private final double MAX_ATTACK_SPEED = 1024.0;
    private Map<String, Double> weaponDamage;
    private boolean disableSweepAttacks;
    private double criticalHitMultiplier;
    private File configFile;
    private FileConfiguration config;

    public LegacyCombatManager(ServerTools plugin) {
        this.plugin = plugin;
        this.modifiedPlayers = new HashSet<>();
        this.weaponDamage = new HashMap<>();
        loadConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            setLegacyCombat(player);
        }
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder() + "/config", "LegacyCombat.yml");

        // If the file doesn't exist, create it with default values
        if (!configFile.exists()) {
            try {
                configFile.getParentFile().mkdirs();  // Ensure the config folder exists
                configFile.createNewFile();  // Create the file if it doesn't exist
                config = YamlConfiguration.loadConfiguration(configFile);

                // Add default configuration values
                config.set("weapon_damage.WOODEN_SWORD", 4);
                config.set("weapon_damage.GOLDEN_SWORD", 4);
                config.set("weapon_damage.STONE_SWORD", 5);
                config.set("weapon_damage.IRON_SWORD", 6);
                config.set("weapon_damage.DIAMOND_SWORD", 7);
                config.set("weapon_damage.NETHERITE_SWORD", 8);
                config.set("weapon_damage.DEFAULT", 1);
                config.set("disable_sweep_attacks", true);
                config.set("critical_hit_multiplier", 1.5);

                // Save the default config to file
                config.save(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            config = YamlConfiguration.loadConfiguration(configFile);
        }

        // Check if "weapon_damage" section exists
        if (config.contains("weapon_damage")) {
            // Load weapon damage values
            for (String key : config.getConfigurationSection("weapon_damage").getKeys(false)) {
                weaponDamage.put(key, config.getDouble("weapon_damage." + key));
            }
        } else {
            plugin.getLogger().warning("The 'weapon_damage' section is missing in LegacyCombat.yml!");
            weaponDamage.put("DEFAULT", 1.0); // Provide a default value if the section is missing
        }

        disableSweepAttacks = config.getBoolean("disable_sweep_attacks", true);
        criticalHitMultiplier = config.getDouble("critical_hit_multiplier", 1.5);
    }

    public void reloadConfig() {
        loadConfig();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        setLegacyCombat(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        resetCombat(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();
        ItemStack weapon = player.getInventory().getItemInMainHand();

        double baseDamage = getBaseDamage(weapon.getType());
        double strength = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
        double damage = baseDamage + strength - 1;

        if (player.getFallDistance() > 0.0F && !player.isOnGround() && !player.isInWater() && !player.isClimbing()) {
            damage *= criticalHitMultiplier;
        }

        event.setDamage(damage);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (disableSweepAttacks && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            event.setCancelled(true);
        }
    }

    private void setLegacyCombat(Player player) {
        AttributeInstance attackSpeed = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.setBaseValue(MAX_ATTACK_SPEED);
            modifiedPlayers.add(player.getUniqueId());
        }
    }

    private void resetCombat(Player player) {
        AttributeInstance attackSpeed = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        if (attackSpeed != null && modifiedPlayers.remove(player.getUniqueId())) {
            attackSpeed.setBaseValue(4.0);
        }
    }

    public void disable() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            resetCombat(player);
        }
        modifiedPlayers.clear();
    }

    private double getBaseDamage(org.bukkit.Material material) {
        return weaponDamage.getOrDefault(material.name(), weaponDamage.get("DEFAULT"));
    }
}
