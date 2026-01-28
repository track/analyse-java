package com.serverstats.api.addon;

import java.util.Collection;
import java.util.Optional;

/**
 * Manager interface for loading, enabling, disabling, and reloading addons.
 * Each platform provides its own implementation of this interface.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * AddonManager manager = platform.getAddonManager();
 *
 * // Get all loaded addons
 * Collection<LoadedAddon> addons = manager.getLoadedAddons();
 *
 * // Check if a specific addon is enabled
 * if (manager.isAddonEnabled("shopguiplus")) {
 *     // Do something
 * }
 *
 * // Reload a specific addon
 * manager.reloadAddon("shopguiplus");
 * }</pre>
 */
public interface AddonManager {

    /**
     * Discover and load all addons from the addons folder.
     * This should be called during plugin initialization.
     */
    void loadAddons();

    /**
     * Enable all loaded addons in dependency order.
     * This should be called after loadAddons().
     */
    void enableAddons();

    /**
     * Disable all enabled addons in reverse dependency order.
     * This should be called during plugin shutdown.
     */
    void disableAddons();

    /**
     * Reload all addons. This will disable, unload, reload from disk, and re-enable addons.
     */
    void reloadAddons();

    /**
     * Reload a specific addon by ID.
     *
     * @param addonId The addon ID to reload
     * @return true if the addon was reloaded successfully
     */
    boolean reloadAddon(String addonId);

    /**
     * Enable a specific addon by ID.
     *
     * @param addonId The addon ID to enable
     * @return true if the addon was enabled successfully
     */
    boolean enableAddon(String addonId);

    /**
     * Disable a specific addon by ID.
     *
     * @param addonId The addon ID to disable
     * @return true if the addon was disabled successfully
     */
    boolean disableAddon(String addonId);

    /**
     * Get a loaded addon by ID.
     *
     * @param addonId The addon ID
     * @return Optional containing the loaded addon, or empty if not found
     */
    Optional<LoadedAddon> getAddon(String addonId);

    /**
     * Get all loaded addons.
     *
     * @return Collection of loaded addons
     */
    Collection<LoadedAddon> getLoadedAddons();

    /**
     * Check if an addon is currently enabled.
     *
     * @param addonId The addon ID
     * @return true if the addon is enabled
     */
    boolean isAddonEnabled(String addonId);

    /**
     * Check if an addon is loaded (may or may not be enabled).
     *
     * @param addonId The addon ID
     * @return true if the addon is loaded
     */
    boolean isAddonLoaded(String addonId);
}
