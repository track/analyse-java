package com.serverstats.bungeecord.addon;

import com.serverstats.api.addon.AbstractAddonManager;
import com.serverstats.api.addon.AddonLogger;
import com.serverstats.bungeecord.ServerStatsBungee;
import java.util.logging.Level;

/**
 * BungeeCord implementation of the addon manager.
 */
public class BungeeAddonManager extends AbstractAddonManager {

    private final ServerStatsBungee plugin;

    /**
     * Create a new BungeeCord addon manager
     *
     * @param plugin The main plugin instance
     */
    public BungeeAddonManager(ServerStatsBungee plugin) {
        super(plugin, plugin.getDataFolder().toPath().resolve("addons"));
        this.plugin = plugin;
    }

    @Override
    protected AddonLogger createAddonLogger(String addonId, String addonName) {
        return new BungeeAddonLogger(plugin, addonName);
    }

    @Override
    protected void logInfo(String message) {
        plugin.getLogger().info("[Addons] " + message);
    }

    @Override
    protected void logWarning(String message) {
        plugin.getLogger().warning("[Addons] " + message);
    }

    @Override
    protected void logError(String message, Throwable throwable) {
        if (throwable != null) {
            plugin.getLogger().log(Level.SEVERE, "[Addons] " + message, throwable);
        } else {
            plugin.getLogger().severe("[Addons] " + message);
        }
    }

    @Override
    protected void logDebug(String message) {
        plugin.debug("[Addons] " + message);
    }
}
