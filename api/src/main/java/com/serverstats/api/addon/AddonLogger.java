package com.serverstats.api.addon;

/**
 * Logger interface for addons to use for consistent logging.
 * Each addon receives its own logger that prefixes messages with the addon name.
 */
public interface AddonLogger {

    /**
     * Log an info message
     *
     * @param message The message to log
     */
    void info(String message);

    /**
     * Log a formatted info message
     *
     * @param format The format string
     * @param args The arguments
     */
    void info(String format, Object... args);

    /**
     * Log a warning message
     *
     * @param message The message to log
     */
    void warning(String message);

    /**
     * Log a formatted warning message
     *
     * @param format The format string
     * @param args The arguments
     */
    void warning(String format, Object... args);

    /**
     * Log an error message
     *
     * @param message The message to log
     */
    void error(String message);

    /**
     * Log a formatted error message
     *
     * @param format The format string
     * @param args The arguments
     */
    void error(String format, Object... args);

    /**
     * Log an error message with an exception
     *
     * @param message The message to log
     * @param throwable The exception to log
     */
    void error(String message, Throwable throwable);

    /**
     * Log a debug message (only shown if debug mode is enabled)
     *
     * @param message The message to log
     */
    void debug(String message);

    /**
     * Log a formatted debug message (only shown if debug mode is enabled)
     *
     * @param format The format string
     * @param args The arguments
     */
    void debug(String format, Object... args);
}
