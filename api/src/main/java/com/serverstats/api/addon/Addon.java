package com.serverstats.api.addon;

import com.serverstats.api.platform.ServerStatsPlatform;
import java.nio.file.Path;

/**
 * Base interface for all ServerStats addons.
 * Addons must implement this interface and be annotated with {@link AddonInfo}.
 *
 * <p>Addon lifecycle:</p>
 * <ol>
 *   <li>{@link #onLoad(ServerStatsPlatform, AddonLogger, Path)} - Called when addon is loaded</li>
 *   <li>{@link #onEnable()} - Called when addon is enabled</li>
 *   <li>{@link #onDisable()} - Called when addon is disabled</li>
 * </ol>
 *
 * <p>Example addon:</p>
 * <pre>{@code
 * @AddonInfo(
 *     id = "example",
 *     name = "Example Addon",
 *     version = "1.0.0"
 * )
 * public class ExampleAddon implements Addon {
 *
 *     private ServerStatsPlatform platform;
 *     private AddonLogger logger;
 *     private Path dataFolder;
 *
 *     @Override
 *     public void onLoad(ServerStatsPlatform platform, AddonLogger logger, Path dataFolder) {
 *         this.platform = platform;
 *         this.logger = logger;
 *         this.dataFolder = dataFolder;
 *     }
 *
 *     @Override
 *     public void onEnable() {
 *         logger.info("Example addon enabled!");
 *     }
 *
 *     @Override
 *     public void onDisable() {
 *         logger.info("Example addon disabled!");
 *     }
 * }
 * }</pre>
 */
public interface Addon {

    /**
     * Called when the addon is loaded. This is where you should store references
     * to the platform, logger, and data folder.
     *
     * @param platform The ServerStats platform instance
     * @param logger The addon's logger
     * @param dataFolder The addon's data folder for configuration files
     */
    void onLoad(ServerStatsPlatform platform, AddonLogger logger, Path dataFolder);

    /**
     * Called when the addon is enabled. This is where you should register
     * listeners, commands, and initialize your addon.
     */
    void onEnable();

    /**
     * Called when the addon is disabled. This is where you should clean up
     * resources, save data, and unregister listeners.
     */
    void onDisable();

    /**
     * Called when the addon is reloaded. Override this to handle configuration
     * reloads gracefully. The default implementation calls onDisable() then onEnable().
     */
    default void onReload() {
        onDisable();
        onEnable();
    }
}
