package net.analyse.api.addon;

import java.nio.file.Path;

/**
 * Represents a loaded addon with its metadata and state.
 */
public interface LoadedAddon {

    /**
     * Get the addon instance.
     *
     * @return The addon instance
     */
    Addon getAddon();

    /**
     * Get the addon metadata from the @AddonInfo annotation.
     *
     * @return The addon info
     */
    AddonInfo getInfo();

    /**
     * Get the addon's ID.
     *
     * @return The addon ID
     */
    default String getId() {
        return getInfo().id();
    }

    /**
     * Get the addon's display name.
     *
     * @return The addon name
     */
    default String getName() {
        return getInfo().name();
    }

    /**
     * Get the addon's version.
     *
     * @return The addon version
     */
    default String getVersion() {
        return getInfo().version();
    }

    /**
     * Get the addon's author.
     *
     * @return The addon author
     */
    default String getAuthor() {
        return getInfo().author();
    }

    /**
     * Get the addon's description.
     *
     * @return The addon description
     */
    default String getDescription() {
        return getInfo().description();
    }

    /**
     * Get the addon's data folder.
     *
     * @return The data folder path
     */
    Path getDataFolder();

    /**
     * Get the addon's logger.
     *
     * @return The addon logger
     */
    AddonLogger getLogger();

    /**
     * Get the path to the addon's JAR file.
     *
     * @return The JAR file path
     */
    Path getJarPath();

    /**
     * Check if the addon is currently enabled.
     *
     * @return true if enabled
     */
    boolean isEnabled();
}
