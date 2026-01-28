package com.serverstats.velocity.addon;

import com.serverstats.api.addon.AbstractAddonManager;
import com.serverstats.api.addon.AddonLogger;
import com.serverstats.velocity.ServerStatsVelocity;
import java.nio.file.Path;

/**
 * Velocity implementation of the addon manager.
 */
public class VelocityAddonManager extends AbstractAddonManager {

    private final ServerStatsVelocity plugin;

    /**
     * Create a new Velocity addon manager
     *
     * @param plugin The main plugin instance
     * @param dataDirectory The plugin's data directory
     */
    public VelocityAddonManager(ServerStatsVelocity plugin, Path dataDirectory) {
        super(plugin, dataDirectory.resolve("addons"));
        this.plugin = plugin;
    }

    @Override
    protected AddonLogger createAddonLogger(String addonId, String addonName) {
        return new VelocityAddonLogger(plugin, addonName);
    }

    @Override
    protected void logInfo(String message) {
        plugin.getLogger().info("[Addons] " + message);
    }

    @Override
    protected void logWarning(String message) {
        plugin.getLogger().warn("[Addons] " + message);
    }

    @Override
    protected void logError(String message, Throwable throwable) {
        if (throwable != null) {
            plugin.getLogger().error("[Addons] " + message, throwable);
        } else {
            plugin.getLogger().error("[Addons] " + message);
        }
    }

    @Override
    protected void logDebug(String message) {
        plugin.debug("[Addons] " + message);
    }
}
