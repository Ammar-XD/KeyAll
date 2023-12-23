package dev.ammar.keyall;

import java.io.File;
import java.io.IOException;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.dvs.versioning.Versioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class KeyAll extends JavaPlugin {
    public static YamlDocument config;

    private long pluginStartTime;
    private static String command;

    public void onEnable() {
        getCommand("keyall").setExecutor((CommandExecutor)this);
        this.pluginStartTime = System.currentTimeMillis();
        try {
            config = YamlDocument.create(new File(getDataFolder(), "config.yml"), getResource("config.yml"), GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.DEFAULT, UpdaterSettings.builder().setVersioning((Versioning)new BasicVersioning("file-version")).build());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null)
            (new KeyAllPlaceholder(this)).register();
        int delayInSeconds = config.getInt("delayInSeconds", 60);
        int delayInMillis = delayInSeconds * 1000;
        scheduleCommandExecution(command, delayInMillis);
    }

    public void sendCommandToConsole(String command) {
                getServer().dispatchCommand(getServer().getConsoleSender(), command);
    }


    public void scheduleCommandExecution(String initialCommand, int delayInMillis) {
        final String keyName = config.getString("keyName");
        final String keyAmount = config.getString("keyAmount");

        if ("NONE".equalsIgnoreCase(keyName)) {
            getLogger().warning("Key name is set to NONE. No commands will be executed.");
            return;
        }

        final String finalCommand = "excellentcrates key giveall " + keyName + " " + keyAmount;

        getServer().getScheduler().runTaskTimer(this, () -> {
            sendCommandToConsole(finalCommand);
        }, 0L, delayInMillis);

        // Use the initialCommand wherever else you need it.
        getServer().dispatchCommand(getServer().getConsoleSender(), initialCommand);
    }


    public void onDisable() {}

    public class KeyAllPlaceholder extends PlaceholderExpansion {
        private final KeyAll plugin;

        public KeyAllPlaceholder(KeyAll plugin) {
            this.plugin = plugin;
        }

        public boolean canRegister() {
            return (this.plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null);
        }

        public String getIdentifier() {
            return "keyall";
        }

        public String getAuthor() {
            return this.plugin.getDescription().getAuthors().toString();
        }

        public String getVersion() {
            return this.plugin.getDescription().getVersion();
        }

        public String onRequest(OfflinePlayer player, String identifier) {
            if (identifier.equals("timer")) {
                int delayInSeconds = KeyAll.config.getInt("delayInSeconds", Integer.valueOf(60)).intValue();
                long timeLeft = delayInSeconds - (System.currentTimeMillis() - this.plugin.pluginStartTime) / 1000L;
                if (timeLeft < 0L) {
                    this.plugin.pluginStartTime = System.currentTimeMillis();
                    sendCommandToConsole(command);
                    return "00";
                }

                long hours = timeLeft / 3600L;
                long minutes = (timeLeft % 3600L) / 60L;
                long seconds = timeLeft % 60L;

                if (hours > 0) {
                    return String.format("%02d:%02d:%02d", hours, minutes, seconds);
                } else if (minutes > 0) {
                    return String.format("%02d:%02d", minutes, seconds);
                } else {
                    return String.format("%02d", seconds);
                }
            }
            return "00";
        }


    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("keyall") && args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            sender.sendMessage("KeyAll configuration reloaded.");
            return true;
        }
        return false;
    }

    public void reloadConfig() {
        try {
            config = YamlDocument.create(new File(getDataFolder(), "config.yml"), getResource("config.yml"), GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.DEFAULT, UpdaterSettings.builder().setVersioning((Versioning)new BasicVersioning("file-version")).build());
            config.reload();
            config.save();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        restartPlugin(this);
    }

    private void restartPlugin(final JavaPlugin plugin) {
        Bukkit.getScheduler().runTaskLater((Plugin)plugin, new Runnable() {
            public void run() {
                Bukkit.getServer().getPluginManager().disablePlugin((Plugin)plugin);
                Bukkit.getServer().getPluginManager().enablePlugin((Plugin)plugin);
            }
        },  20L);
    }

    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (player.isOp()) {
            player.sendMessage(ChatColor.GREEN + "This is a  developer build for KeyAll we recommend the stable builds from spigotMC");
        }
    }
}