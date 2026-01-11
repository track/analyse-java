package net.analyse.api.platform;

import net.analyse.sdk.AnalyseClient;

/**
 * Interface for platform-specific plugin implementations.
 * Each platform (Paper, BungeeCord, Velocity) implements this to provide
 * access to the SDK client and logging.
 */
public interface AnalysePlatform {

  /**
   * Get the SDK client for API communication
   *
   * @return The Analyse SDK client
   */
  AnalyseClient getClient();

  /**
   * Check if debug mode is enabled
   *
   * @return true if debug mode is enabled
   */
  boolean isDebugEnabled();

  /**
   * Log an info message
   *
   * @param message The message to log
   */
  void logInfo(String message);

  /**
   * Log a warning message
   *
   * @param message The message to log
   */
  void logWarning(String message);
}
