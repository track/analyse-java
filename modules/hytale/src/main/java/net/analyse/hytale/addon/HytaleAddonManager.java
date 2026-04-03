package net.analyse.hytale.addon;

import net.analyse.api.addon.AbstractAddonManager;
import net.analyse.api.addon.AddonLogger;
import net.analyse.hytale.HytalePlugin;
import java.nio.file.Path;

/**
 * Hytale implementation of the addon manager.
 */
public class HytaleAddonManager extends AbstractAddonManager {

    private final HytalePlugin plugin;

    /**
     * Create a new Hytale addon manager
     *
     * @param plugin The main plugin instance
     * @param dataDirectory The plugin's data directory
     */
    public HytaleAddonManager(HytalePlugin plugin, Path dataDirectory) {
        super(plugin, dataDirectory.resolve("addons"), plugin.getClass().getClassLoader());
        this.plugin = plugin;
    }

    @Override
    protected AddonLogger createAddonLogger(String addonId, String addonName) {
        return new HytaleAddonLogger(plugin, addonName);
    }

    @Override
    protected void logInfo(String message) {
        plugin.logInfo("[Addons] " + message);
    }

    @Override
    protected void logWarning(String message) {
        plugin.logWarning("[Addons] " + message);
    }

    @Override
    protected void logError(String message, Throwable throwable) {
        plugin.logError("[Addons] " + message, throwable);
    }

    @Override
    protected void logDebug(String message) {
        plugin.debug("[Addons] " + message);
    }
}
