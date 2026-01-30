package com.serverstats.paper.addon;

import com.serverstats.api.addon.AbstractAddonManager;
import com.serverstats.api.addon.AddonLogger;
import com.serverstats.paper.ServerStatsPlugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Level;

/**
 * Paper implementation of the addon manager.
 */
public class PaperAddonManager extends AbstractAddonManager {

    private final ServerStatsPlugin plugin;

    /**
     * Create a new Paper addon manager
     *
     * @param plugin The main plugin instance
     */
    public PaperAddonManager(ServerStatsPlugin plugin) {
        super(plugin, plugin.getDataFolder().toPath().resolve("addons"), plugin.getClass().getClassLoader());
        this.plugin = plugin;
    }

    @Override
    protected URLClassLoader createAddonClassLoader(URL[] urls, ClassLoader parent) {
        // Use custom classloader that can access other plugins' classes
        return new PaperAddonClassLoader(urls, parent);
    }

    @Override
    protected AddonLogger createAddonLogger(String addonId, String addonName) {
        return new PaperAddonLogger(plugin, addonName);
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
