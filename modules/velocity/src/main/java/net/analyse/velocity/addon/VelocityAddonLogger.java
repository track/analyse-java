package net.analyse.velocity.addon;

import net.analyse.api.addon.AddonLogger;
import net.analyse.velocity.AnalyseVelocity;
import org.slf4j.Logger;

/**
 * Velocity implementation of AddonLogger that prefixes messages with the addon name.
 */
public class VelocityAddonLogger implements AddonLogger {

    private final Logger logger;
    private final AnalyseVelocity plugin;
    private final String prefix;

    /**
     * Create a new addon logger
     *
     * @param plugin The main plugin instance
     * @param addonName The addon display name
     */
    public VelocityAddonLogger(AnalyseVelocity plugin, String addonName) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.prefix = "[" + addonName + "] ";
    }

    @Override
    public void info(String message) {
        logger.info(prefix + message);
    }

    @Override
    public void info(String format, Object... args) {
        logger.info(prefix + String.format(format, args));
    }

    @Override
    public void warning(String message) {
        logger.warn(prefix + message);
    }

    @Override
    public void warning(String format, Object... args) {
        logger.warn(prefix + String.format(format, args));
    }

    @Override
    public void error(String message) {
        logger.error(prefix + message);
    }

    @Override
    public void error(String format, Object... args) {
        logger.error(prefix + String.format(format, args));
    }

    @Override
    public void error(String message, Throwable throwable) {
        logger.error(prefix + message, throwable);
    }

    @Override
    public void debug(String message) {
        if (plugin.isDebugEnabled()) {
            logger.info(prefix + "[DEBUG] " + message);
        }
    }

    @Override
    public void debug(String format, Object... args) {
        if (plugin.isDebugEnabled()) {
            logger.info(prefix + "[DEBUG] " + String.format(format, args));
        }
    }
}
