package net.analyse.api;

import net.analyse.api.platform.AnalysePlatform;

/**
 * Internal provider that holds the reference to the active platform plugin.
 * This allows the static Analyse API to access the underlying implementation.
 *
 * <p><b>For plugin developers:</b> Use {@link Analyse} instead of this class.</p>
 *
 * <p><b>For platform implementations:</b> Call {@link #register(AnalysePlatform)}
 * in your onEnable and {@link #unregister()} in your onDisable.</p>
 */
public final class AnalyseProvider {

  private static AnalysePlatform platform;

  private AnalyseProvider() {
  }

  /**
   * Register the platform implementation. Called by platform plugins on enable.
   *
   * @param platform The platform implementation
   * @throws IllegalStateException if a platform is already registered
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
    Analyse.setConnectionStatus(false, null);
  }

  /**
   * Get the registered platform implementation
   *
   * @return The platform implementation, or null if not registered
   */
  public static AnalysePlatform getPlatform() {
    return platform;
  }

  /**
   * Check if a platform is registered
   *
   * @return true if a platform is registered
   */
  public static boolean isRegistered() {
    return platform != null;
  }
}
