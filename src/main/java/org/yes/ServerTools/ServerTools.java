package org.yes.ServerTools;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.yes.ServerTools.commands.*;
import org.yes.ServerTools.listeners.JoinLeaveListener;
import org.yes.ServerTools.utils.EconomyManager;
import org.yes.ServerTools.utils.BankNoteManager;
import org.yes.ServerTools.ui.TabMenuManager;

import java.io.File;

public class ServerTools extends JavaPlugin implements Listener {
    private FileConfiguration config;
    private EconomyManager economyManager;
    private BankNoteManager bankNoteManager;
    private TabMenuManager tabMenuManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();

        if (!setupDependencies()) {
            getLogger().severe("Required dependencies not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize economy manager
        File economyFile = new File(getDataFolder(), "economy.yml");
        if (!economyFile.exists()) {
            saveResource("economy.yml", false);
        }
        economyManager = new EconomyManager(economyFile);

        // Initialize bank note manager
        bankNoteManager = new BankNoteManager(this);

        // Initialize tab menu manager
        tabMenuManager = new TabMenuManager(this);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new JoinLeaveListener(this), this);
        getServer().getPluginManager().registerEvents(this, this);

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

    public BankNoteManager getBankNoteManager() {
        return bankNoteManager;
    }

    public void reloadPluginConfig() {
        reloadConfig();
        config = getConfig();
        // Reload any other configuration-dependent components
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

    private void registerCommands() {
        // Game mode commands
        getCommand("gmc").setExecutor(new GameModeCommand(this, "gmc"));
        getCommand("gms").setExecutor(new GameModeCommand(this, "gms"));
        getCommand("gmsp").setExecutor(new GameModeCommand(this, "gmsp"));
        getCommand("gma").setExecutor(new GameModeCommand(this, "gma"));

        // Teleport commands
        TpaCommand tpaCommand = new TpaCommand(this);
        getCommand("tpa").setExecutor(tpaCommand);
        getCommand("tpahere").setExecutor(tpaCommand);
        getCommand("tpaccept").setExecutor(tpaCommand);
        getCommand("tpdeny").setExecutor(tpaCommand);

        // Messaging commands
        MsgCommand msgCommand = new MsgCommand(this);
        getCommand("msg").setExecutor(msgCommand);
        getCommand("tell").setExecutor(msgCommand);
        getCommand("w").setExecutor(msgCommand);
        getCommand("r").setExecutor(msgCommand);

        // Economy commands
        EconomyCommand economyCommand = new EconomyCommand(this);
        getCommand("balance").setExecutor(economyCommand);
        getCommand("bal").setExecutor(economyCommand);
        getCommand("pay").setExecutor(economyCommand);
        getCommand("withdraw").setExecutor(economyCommand);
        getCommand("baltop").setExecutor(economyCommand);
        getCommand("addmoney").setExecutor(economyCommand);
        getCommand("removemoney").setExecutor(economyCommand);

        // Fly command
        getCommand("fly").setExecutor(new FlyCommand(this));

    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getItem() == null) return;
        ItemStack item = event.getItem();
        if (bankNoteManager.isBankNote(item)) {
            event.setCancelled(true);
            bankNoteManager.depositBankNote(event.getPlayer(), item);
        }
    }
}