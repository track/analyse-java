package com.serverstats.api.platform;

import com.serverstats.api.manager.ABTestManager;
import com.serverstats.api.manager.SessionManager;

/**
 * Interface for platform-specific plugin implementations.
 * Each platform (Paper, BungeeCord, Velocity) implements this to provide
 * access to managers and logging.
 *
 * <p>Other plugins should use {@link com.serverstats.api.ServerStats} for most operations,
 * but can access this interface for advanced functionality:</p>
 * <pre>{@code
 * ServerStatsPlatform platform = ServerStats.get();
 * SessionManager sessions = platform.getSessionManager();
 * ABTestManager abTests = platform.getABTestManager();
 * }</pre>
 */
public interface ServerStatsPlatform {

  /**
   * Get the session manager for accessing player sessions
   *
   * @return The session manager
   */
  SessionManager getSessionManager();

  /**
   * Get the A/B test manager for accessing tests and variants
   *
   * @return The A/B test manager, or null if not available
   */
  ABTestManager getABTestManager();

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

  /**
   * Get the plugin version
   *
   * @return The version string
   */
  String getVersion();
}
