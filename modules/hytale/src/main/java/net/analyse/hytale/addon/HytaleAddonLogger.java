package net.analyse.hytale.addon;

import net.analyse.api.addon.AddonLogger;
import net.analyse.hytale.HytalePlugin;

/**
 * Hytale implementation of AddonLogger that prefixes messages with the addon name.
 */
public class HytaleAddonLogger implements AddonLogger {

    private final HytalePlugin plugin;
    private final String prefix;

    /**
     * Create a new addon logger
     *
     * @param plugin The main plugin instance
     * @param addonName The addon display name
     */
    public HytaleAddonLogger(HytalePlugin plugin, String addonName) {
        this.plugin = plugin;
        this.prefix = "[" + addonName + "] ";
    }

    @Override
    public void info(String message) {
        plugin.logInfo(prefix + message);
    }

    @Override
    public void info(String format, Object... args) {
        plugin.logInfo(prefix + String.format(format, args));
    }

    @Override
    public void warning(String message) {
        plugin.logWarning(prefix + message);
    }

    @Override
    public void warning(String format, Object... args) {
        plugin.logWarning(prefix + String.format(format, args));
    }

    @Override
    public void error(String message) {
        plugin.logError(prefix + message);
    }

    @Override
    public void error(String format, Object... args) {
        plugin.logError(prefix + String.format(format, args));
    }

    @Override
    public void error(String message, Throwable throwable) {
        plugin.logError(prefix + message, throwable);
    }

    @Override
    public void debug(String message) {
        plugin.debug(prefix + message);
    }

    @Override
    public void debug(String format, Object... args) {
        plugin.debug(prefix + String.format(format, args));
    }
}
