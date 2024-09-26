package ServerTools;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import ServerTools.commands.*;
import ServerTools.listeners.*;
import ServerTools.utils.*;
import ServerTools.ui.*;

import java.io.File;

public class ServerTools extends JavaPlugin implements Listener {
    private FileConfiguration config;
    private EconomyManager economyManager;
    private BankNoteManager bankNoteManager;
    private TabMenuManager tabMenuManager;
    private HomeManager homeManager;
    private AFKManager afkManager;
    private LegacyCombatManager legacyCombatManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();

        if (!setupDependencies()) {
            getLogger().severe("Required dependencies not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        initializeFeatures();
        registerCommands();
        registerListeners();

        getLogger().info("ServerTools plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (economyManager != null) {
            economyManager.saveData();
        }
        if (legacyCombatManager != null) {
            legacyCombatManager.disable();
        }

        getLogger().info("ServerTools plugin disabled.");
    }

    private boolean setupDependencies() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().severe("PlaceholderAPI not found!");
            return false;
        }
        if (getServer().getPluginManager().getPlugin("LuckPerms") == null) {
            getLogger().severe("LuckPerms not found!");
            return false;
        }
        return true;
    }

    private void initializeFeatures() {
        if (config.getBoolean("features.afk", true)) {
            afkManager = new AFKManager(this);
        }

        if (config.getBoolean("features.homeManager", true)) {
            homeManager = new HomeManager(this);
        }

        if (config.getBoolean("features.tabMenu", true)) {
            tabMenuManager = new TabMenuManager(this);
        }

        if (config.getBoolean("features.economy", true)) {
            File economyFile = new File(getDataFolder(), "storage/economy.yml");
            if (!economyFile.exists()) {
                saveResource("storage/economy.yml", false);
            }
            economyManager = new EconomyManager(economyFile);
        }

        if (config.getBoolean("features.bankNotes", true)) {
            bankNoteManager = new BankNoteManager(this);
        }

        if (config.getBoolean("features.legacyCombat", false)) {
            enableLegacyCombat();
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(this, this);

        if (config.getBoolean("features.joinLeaveMessages", true)) {
            getServer().getPluginManager().registerEvents(new JoinLeaveListener(this), this);
        }

        if (config.getBoolean("features.afk", true)) {
            getServer().getPluginManager().registerEvents(new AFKListener(this), this);
        }
    }

    private void registerCommands() {
        if (config.getBoolean("features.gameModeCommands", true)) {
            getCommand("gmc").setExecutor(new GameModeCommand(this, "gmc"));
            getCommand("gms").setExecutor(new GameModeCommand(this, "gms"));
            getCommand("gmsp").setExecutor(new GameModeCommand(this, "gmsp"));
            getCommand("gma").setExecutor(new GameModeCommand(this, "gma"));
        }

        if (config.getBoolean("features.teleportCommands", true)) {
            TpaCommand tpaCommand = new TpaCommand(this);
            getCommand("tpa").setExecutor(tpaCommand);
            getCommand("tpahere").setExecutor(tpaCommand);
            getCommand("tpaccept").setExecutor(tpaCommand);
            getCommand("tpdeny").setExecutor(tpaCommand);
        }

        if (config.getBoolean("features.messagingCommands", true)) {
            MsgCommand msgCommand = new MsgCommand(this);
            getCommand("msg").setExecutor(msgCommand);
            getCommand("tell").setExecutor(msgCommand);
            getCommand("w").setExecutor(msgCommand);
            getCommand("r").setExecutor(msgCommand);
        }

        if (config.getBoolean("features.economy", true)) {
            EconomyCommand economyCommand = new EconomyCommand(this);
            getCommand("balance").setExecutor(economyCommand);
            getCommand("bal").setExecutor(economyCommand);
            getCommand("pay").setExecutor(economyCommand);
            getCommand("withdraw").setExecutor(economyCommand);
            getCommand("baltop").setExecutor(economyCommand);
            getCommand("addmoney").setExecutor(economyCommand);
            getCommand("removemoney").setExecutor(economyCommand);
        }

        if (config.getBoolean("features.flyCommand", true)) {
            getCommand("fly").setExecutor(new FlyCommand(this));
        }

        if (config.getBoolean("features.homeManager", true)) {
            HomeCommand homeCommand = new HomeCommand(this);
            getCommand("sethome").setExecutor(homeCommand);
            getCommand("home").setExecutor(homeCommand);
            getCommand("delhome").setExecutor(homeCommand);
            getCommand("homes").setExecutor(homeCommand);
        }

        if (config.getBoolean("features.helpCommand", true)) {
            getCommand("help").setExecutor(new HelpCommand(this));
        }

        if (config.getBoolean("features.rulesCommand", true)) {
            getCommand("rules").setExecutor(new RulesCommand(this));
        }

        if (config.getBoolean("features.afk", true)) {
            getCommand("afk").setExecutor(new AFKCommand(this));
        }
    }

    public FileConfiguration getPluginConfig() {
        return config;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public BankNoteManager getBankNoteManager() {
        return bankNoteManager;
    }

    public HomeManager getHomeManager() {
        return homeManager;
    }

    public AFKManager getAFKManager() {
        return afkManager;
    }

    public LegacyCombatManager getLegacyCombatManager() {
        return legacyCombatManager;
    }

    public void reloadPluginConfig() {
        reloadConfig();
        config = getConfig();

        // Check if legacy combat setting has changed
        boolean legacyCombatEnabled = config.getBoolean("features.legacyCombat", false);
        if (legacyCombatEnabled && legacyCombatManager == null) {
            enableLegacyCombat();
        } else if (!legacyCombatEnabled && legacyCombatManager != null) {
            disableLegacyCombat();
        }

        // Reload other configuration-dependent components here
    }

    public void enableLegacyCombat() {
        if (legacyCombatManager == null) {
            legacyCombatManager = new LegacyCombatManager(this);
            getLogger().info("Legacy combat system enabled.");
        }
    }

    public void disableLegacyCombat() {
        if (legacyCombatManager != null) {
            legacyCombatManager.disable();
            legacyCombatManager = null;
            getLogger().info("Legacy combat system disabled.");
        }
    }

    public FileConfiguration getCustomConfig(String filename) {
        File configFile = new File(getDataFolder(), filename);
        if (!configFile.exists()) {
            saveResource(filename, false);
        }
        return YamlConfiguration.loadConfiguration(configFile);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!config.getBoolean("features.bankNotes", true)) return;
        if (event.getItem() == null) return;
        ItemStack item = event.getItem();
        if (bankNoteManager.isBankNote(item)) {
            event.setCancelled(true);
            bankNoteManager.depositBankNote(event.getPlayer(), item);
        }
    }
}