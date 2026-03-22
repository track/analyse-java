package net.analyse.spigot.addon;

import net.analyse.api.addon.AbstractAddonManager;
import net.analyse.api.addon.AddonLogger;
import net.analyse.spigot.AnalysePlugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Level;

/**
 * Paper implementation of the addon manager.
 */
public class SpigotAddonManager extends AbstractAddonManager {

    private final AnalysePlugin plugin;

    /**
     * Create a new Paper addon manager
     *
     * @param plugin The main plugin instance
     */
    public SpigotAddonManager(AnalysePlugin plugin) {
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
        return new SpigotAddonLogger(plugin, addonName);
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
