package com.geitenijs.commandblocks;

import com.geitenijs.commandblocks.commands.CommandWrapper;
import com.geitenijs.commandblocks.updatechecker.UpdateCheck;
import net.milkbowl.vault.Vault;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class Utilities {

    public static FileConfiguration config;
    public static FileConfiguration blocks;
    static ConcurrentHashMap<String, Double> timeouts = new ConcurrentHashMap<>();
    private static File configFile = new File(Main.plugin.getDataFolder(), "config.yml");
    private static File blocksFile = new File(Main.plugin.getDataFolder(), "blocks.yml");
    private static boolean updateAvailable;
    private static String updateVersion;

    static {
        config = YamlConfiguration.loadConfiguration(new File(Main.plugin.getDataFolder(), "config.yml"));
        blocks = YamlConfiguration.loadConfiguration(new File(Main.plugin.getDataFolder(), "blocks.yml"));
    }

    static void pluginBanner() {
        if (config.getBoolean("general.pluginbanner")) {
            consoleBanner("");
            consoleBanner("&c _______                                  _&8 ______  _             _          ");
            consoleBanner("&c(_______)                                | &8(____  \\| |           | |  &cv" + Strings.VERSION);
            consoleBanner("&c _       ___  ____  ____  _____ ____   __| &8|____)  ) | ___   ____| |  _  ___ ");
            consoleBanner("&c| |     / _ \\|    \\|    \\(____ |  _ \\ / _  &8|  __  (| |/ _ \\ / ___) |_/ )/___)");
            consoleBanner("&c| |____| |_| | | | | | | / ___ | | | ( (_| &8| |__)  ) | |_| ( (___|  _ (|___ |");
            consoleBanner("&c \\______)___/|_|_|_|_|_|_\\_____|_| |_|\\____&8|______/ \\_)___/ \\____)_| \\_|___/ ");
            consoleBanner("");
        }
    }

    static void createConfigs() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        FileConfiguration blocks = YamlConfiguration.loadConfiguration(blocksFile);

        // Manually setting default values
        config.addDefault("general.pluginbanner", true);
        config.addDefault("general.colourfulconsole", true);
        config.addDefault("updates.check", true);
        config.addDefault("updates.notify", true);

        config.addDefault("default.permission.value", "commandblocks.use");
        config.addDefault("default.cost.value", 0);
        config.addDefault("default.cost.bypasspermission", "commandblocks.cost.bypass");
        config.addDefault("default.timeout.value", 5);
        config.addDefault("default.timeout.bypasspermission", "commandblocks.timeout.bypass");
        config.addDefault("default.delay.value", 0);

        // Enable copying defaults
        config.options().copyDefaults(true);
        blocks.options().copyDefaults(true);

        // Save the configuration file
        saveConfigFile();
        reloadConfigFile();
        saveBlocksFile();
        reloadBlocksFile();
    }

    // Manually adding comments (since setHeader() is not available)
    static void saveConfigFile() {
        try {
            // Write comments manually
            List<String> comments = Arrays.asList(
                    "# Executing your commands since 2018! By " + Strings.AUTHOR,
                    "# Information & Support: " + Strings.WEBSITE,
                    "#",
                    "# general:",
                    "#   pluginbanner: Whether or not to display the fancy banner in your console on server startup.",
                    "#   colourfulconsole: Console messages will be colored when this is enabled.",
                    "# updates:",
                    "#   check: When enabled, the plugin will check for updates.",
                    "#   notify: Would you like to get an in-game reminder of a new update?",
                    "# default:"
            );

            // Write the comments manually before saving
            FileWriter writer = new FileWriter(configFile);
            for (String comment : comments) {
                writer.write(comment + "\n");
            }

            //save the actual YAML configuration
            writer.write(config.saveToString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    static void registerCommandsAndCompletions() {
        Main.plugin.getCommand("commandblocks").setExecutor(new CommandWrapper());
        Main.plugin.getCommand("cb").setExecutor(new CommandWrapper());
        Main.plugin.getCommand("commandblocks").setTabCompleter(new CommandWrapper());
        Main.plugin.getCommand("cb").setTabCompleter(new CommandWrapper());
    }

    static void registerEvents() {
            Bukkit.getPluginManager().registerEvents(new Events(), Main.plugin);
    }

    static void startTasks() {
        BukkitRunnable runnable = (new BukkitRunnable() {
            @Override
            public void run() {
                for (String o : timeouts.keySet()) {
                    timeouts.put(o, timeouts.get(o) - 1);
                    if (timeouts.get(o) <= 0) {
                        timeouts.remove(o);
                    }
                }
            }
        });
        runnable.runTaskTimerAsynchronously(Main.plugin, 0L, 1L);
        Bukkit.getScheduler().scheduleSyncDelayedTask(Main.plugin, Utilities::checkForUpdates, 200L);
    }

    static void stopTasks() {
        Bukkit.getScheduler().cancelTasks(Main.plugin);
    }

    static void startMetrics() {
        Metrics metrics = new Metrics(Main.plugin, 3655);
        metrics.addCustomChart(new Metrics.SingleLineChart("definedCommandBlocks", () -> blocks.getKeys(false).size()));
        metrics.addCustomChart(new Metrics.SimplePie("pluginBannerEnabled", () -> config.getString("general.pluginbanner")));
        metrics.addCustomChart(new Metrics.SimplePie("colourfulConsoleEnabled", () -> config.getString("general.colourfulconsole")));
        metrics.addCustomChart(new Metrics.SimplePie("updateCheckEnabled", () -> config.getString("updates.check")));
        metrics.addCustomChart(new Metrics.SimplePie("updateNotificationEnabled", () -> config.getString("updates.notify")));
        metrics.addCustomChart(new Metrics.SimplePie("vaultVersion", () -> {
            final Plugin p = Bukkit.getPluginManager().getPlugin("Vault");
            if (!(p instanceof Vault)) {
                return Strings.NOSTAT;
            } else {
                return Bukkit.getServer().getPluginManager().getPlugin("Vault").getDescription().getVersion();
            }
        }));
    }

    static void done() {
        consoleMsg(Strings.PLUGIN + " v" + Strings.VERSION + " has been enabled");
    }

    private static void checkForUpdates() {
        if (config.getBoolean("updates.check")) {
            UpdateCheck
                    .of(Main.plugin)
                    .resourceId(Strings.RESOURCEID)
                    .handleResponse((versionResponse, version) -> {
                        switch (versionResponse) {
                            case FOUND_NEW:
                                consoleMsg("A new release of " + Strings.PLUGIN + ", v" + version + ", is available! You are still on v" + Strings.VERSION + ".");
                                consoleMsg("To download this update, head over to " + Strings.WEBSITE + "/updates in your browser.");
                                updateVersion = version;
                                updateAvailable = true;
                                break;
                            case LATEST:
                                updateAvailable = false;
                                break;
                            case UNAVAILABLE:
                                updateAvailable = false;
                        }
                    }).check();
        }
    }

    static boolean updateAvailable() {
        return updateAvailable;
    }

    static String updateVersion() {
        return updateVersion;
    }

    public static void reloadConfigFile() {
        if (configFile == null) {
            configFile = new File(Main.plugin.getDataFolder(), "config.yml");
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }



    public static void reloadBlocksFile() {
        if (blocksFile == null) {
            blocksFile = new File(Main.plugin.getDataFolder(), "blocks.yml");
        }
        blocks = YamlConfiguration.loadConfiguration(blocksFile);
    }

    public static void saveBlocksFile() {
        if (blocks == null || blocksFile == null) {
            return;
        }
        try {
            blocks.save(blocksFile);
        } catch (IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not save " + blocksFile, ex);
        }
    }

    public static void msg(final CommandSender s, String msg) {
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        if (!(s instanceof Player)) {
            if (!config.getBoolean("general.colourfulconsole")) {
                msg = ChatColor.stripColor(msg);
            }
        }
        s.sendMessage(msg);
    }

    public static void consoleMsg(String msg) {
        msg = ChatColor.translateAlternateColorCodes('&', Strings.INTERNALPREFIX + msg);
        if (!config.getBoolean("general.colourfulconsole")) {
            msg = ChatColor.stripColor(msg);
        }
        Bukkit.getServer().getConsoleSender().sendMessage(msg);
    }

    private static void consoleBanner(final String message) {
        Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}
