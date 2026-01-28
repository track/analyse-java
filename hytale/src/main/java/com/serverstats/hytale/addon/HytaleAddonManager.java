package com.serverstats.hytale.addon;

import com.serverstats.api.addon.AbstractAddonManager;
import com.serverstats.api.addon.AddonLogger;
import com.serverstats.hytale.HytalePlugin;
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
        super(plugin, dataDirectory.resolve("addons"));
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
