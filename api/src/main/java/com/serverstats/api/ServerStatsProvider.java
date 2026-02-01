package com.serverstats.api;

import com.serverstats.api.platform.ServerStatsPlatform;

/**
 * Internal provider that holds the reference to the active platform plugin.
 * This allows the static ServerStats API to access the underlying implementation.
 *
 * <p><b>For plugin developers:</b> Use {@link ServerStats} instead of this class.</p>
 *
 * <p><b>For platform implementations:</b> Call {@link #register(ServerStatsPlatform)}
 * in your onEnable and {@link #unregister()} in your onDisable.</p>
 */
public final class ServerStatsProvider {

  private static ServerStatsPlatform platform;

  private ServerStatsProvider() {
  }

  /**
   * Register the platform implementation. Called by platform plugins on enable.
   *
   * @param platform The platform implementation
   * @throws IllegalStateException if a platform is already registered
   */
  public static void register(ServerStatsPlatform platform) {
    if (ServerStatsProvider.platform != null) {
      throw new IllegalStateException("ServerStats platform is already registered");
    }

    ServerStatsProvider.platform = platform;
  }

  /**
   * Unregister the platform implementation. Called by platform plugins on disable.
   */
  public static void unregister() {
    platform = null;
    ServerStats.setConnectionStatus(false, null);
  }

  /**
   * Get the registered platform implementation
   *
   * @return The platform implementation, or null if not registered
   */
  public static ServerStatsPlatform getPlatform() {
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
