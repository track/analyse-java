package net.analyse.bungeecord.addon;

import net.analyse.api.addon.AddonLogger;
import net.analyse.bungeecord.AnalyseBungee;
import java.util.logging.Level;

/**
 * BungeeCord implementation of AddonLogger that prefixes messages with the addon name.
 */
public class BungeeAddonLogger implements AddonLogger {

    private final AnalyseBungee plugin;
    private final String prefix;

    /**
     * Create a new addon logger
     *
     * @param plugin The main plugin instance
     * @param addonName The addon display name
     */
    public BungeeAddonLogger(AnalyseBungee plugin, String addonName) {
        this.plugin = plugin;
        this.prefix = "[" + addonName + "] ";
    }

    @Override
    public void info(String message) {
        plugin.getLogger().info(prefix + message);
    }

    @Override
    public void info(String format, Object... args) {
        plugin.getLogger().info(prefix + String.format(format, args));
    }

    @Override
    public void warning(String message) {
        plugin.getLogger().warning(prefix + message);
    }

    @Override
    public void warning(String format, Object... args) {
        plugin.getLogger().warning(prefix + String.format(format, args));
    }

    @Override
    public void error(String message) {
        plugin.getLogger().severe(prefix + message);
    }

    @Override
    public void error(String format, Object... args) {
        plugin.getLogger().severe(prefix + String.format(format, args));
    }

    @Override
    public void error(String message, Throwable throwable) {
        plugin.getLogger().log(Level.SEVERE, prefix + message, throwable);
    }

    @Override
    public void debug(String message) {
        if (plugin.isDebugEnabled()) {
            plugin.getLogger().info(prefix + "[DEBUG] " + message);
        }
    }

    @Override
    public void debug(String format, Object... args) {
        if (plugin.isDebugEnabled()) {
            plugin.getLogger().info(prefix + "[DEBUG] " + String.format(format, args));
        }
    }
}
