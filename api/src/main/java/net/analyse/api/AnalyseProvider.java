package net.analyse.api;

import net.analyse.api.platform.AnalysePlatform;

/**
 * Internal provider that holds the reference to the active platform plugin.
 * This allows the static Analyse API to access the underlying SDK client.
 */
public final class AnalyseProvider {

  private static AnalysePlatform platform;

  private AnalyseProvider() {
    // Prevent instantiation
  }

  /**
   * Register the platform implementation. Called by platform plugins on enable.
   *
   * @param platform The platform implementation
   */
  public static void register(AnalysePlatform platform) {
    if (AnalyseProvider.platform != null) {
      throw new IllegalStateException("Analyse platform is already registered");
    }

    AnalyseProvider.platform = platform;
  }

  /**
   * Unregister the platform implementation. Called by platform plugins on disable.
   */
  public static void unregister() {
    platform = null;
  }

  /**
   * Get the registered platform implementation
   *
   * @return The platform implementation, or null if not registered
   */
  static AnalysePlatform getPlatform() {
    return platform;
  }

  /**
   * Check if a platform is registered
   *
   * @return true if a platform is registered
   */
  static boolean isRegistered() {
    return platform != null;
  }
}
