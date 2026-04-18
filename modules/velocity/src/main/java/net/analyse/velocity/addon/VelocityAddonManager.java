package net.analyse.velocity.addon;

import net.analyse.api.addon.AbstractAddonManager;
import net.analyse.api.addon.AddonLogger;
import net.analyse.velocity.AnalyseVelocity;
import java.nio.file.Path;

/**
 * Velocity implementation of the addon manager.
 */
public class VelocityAddonManager extends AbstractAddonManager {

    private final AnalyseVelocity plugin;

    /**
     * Create a new Velocity addon manager
     *
     * @param plugin The main plugin instance
     * @param dataDirectory The plugin's data directory
     */
    public VelocityAddonManager(AnalyseVelocity plugin, Path dataDirectory) {
        super(plugin, dataDirectory.resolve("addons"), plugin.getClass().getClassLoader());
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
