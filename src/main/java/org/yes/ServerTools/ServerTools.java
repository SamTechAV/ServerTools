package org.yes.ServerTools;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.yes.ServerTools.commands.*;
import org.yes.ServerTools.listeners.JoinLeaveListener;
import org.yes.ServerTools.utils.EconomyManager;

import java.io.File;

public class ServerTools extends JavaPlugin {
    private FileConfiguration config;
    private EconomyManager economyManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();

        if (!setupPlaceholderAPI()) {
            getLogger().severe("PlaceholderAPI not found! This plugin requires PlaceholderAPI to function properly.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize economy manager
        File economyFile = new File(getDataFolder(), "economy.yml");
        if (!economyFile.exists()) {
            saveResource("economy.yml", false);
        }
        economyManager = new EconomyManager(economyFile);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new JoinLeaveListener(this), this);

        // Register commands
        registerCommands();

        getLogger().info("ServerTools plugin enabled.");
    }

    @Override
    public void onDisable() {
        // Save economy data
        if (economyManager != null) {
            economyManager.saveData();
        }

        getLogger().info("ServerTools plugin disabled.");
    }

    public FileConfiguration getPluginConfig() {
        return config;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public void reloadPluginConfig() {
        reloadConfig();
        config = getConfig();
        // Reload any other configuration-dependent components
    }

    private boolean setupPlaceholderAPI() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return false;
        }
        return true;
    }

    private void registerCommands() {
        getCommand("gmc").setExecutor(new GameModeCommand(this, "gmc"));
        getCommand("gms").setExecutor(new GameModeCommand(this, "gms"));
        getCommand("gmsp").setExecutor(new GameModeCommand(this, "gmsp"));
        getCommand("gma").setExecutor(new GameModeCommand(this, "gma"));

        TpaCommand tpaCommand = new TpaCommand(this);
        getCommand("tpa").setExecutor(tpaCommand);
        getCommand("tpahere").setExecutor(tpaCommand);
        getCommand("tpaccept").setExecutor(tpaCommand);
        getCommand("tpdeny").setExecutor(tpaCommand);

        MsgCommand msgCommand = new MsgCommand(this);
        getCommand("msg").setExecutor(msgCommand);
        getCommand("tell").setExecutor(msgCommand);
        getCommand("w").setExecutor(msgCommand);
        getCommand("r").setExecutor(msgCommand);

        EconomyCommand economyCommand = new EconomyCommand(this);
        getCommand("balance").setExecutor(economyCommand);
        getCommand("bal").setExecutor(economyCommand);
        getCommand("pay").setExecutor(economyCommand);
        getCommand("withdraw").setExecutor(economyCommand);
        getCommand("baltop").setExecutor(economyCommand);
        getCommand("addmoney").setExecutor(economyCommand);
        getCommand("removemoney").setExecutor(economyCommand);
    }
}