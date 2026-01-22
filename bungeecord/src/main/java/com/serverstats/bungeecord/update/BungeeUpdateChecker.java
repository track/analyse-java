package com.serverstats.bungeecord.update;

import com.serverstats.bungeecord.ServerStatsBungee;
import com.serverstats.sdk.update.UpdateChecker;
import java.util.concurrent.TimeUnit;

/**
 * BungeeCord-specific update checker with console notifications
 */
public class BungeeUpdateChecker {

  private static final long CHECK_INTERVAL_MINUTES = 30;

  private final ServerStatsBungee plugin;
  private final UpdateChecker checker;

  /**
   * Create a new BungeeCord update checker
   *
   * @param plugin The plugin instance
   * @param currentVersion The current plugin version
   */
  public BungeeUpdateChecker(ServerStatsBungee plugin, String currentVersion) {
    this.plugin = plugin;
    this.checker = new UpdateChecker(plugin.getClient(), currentVersion, "bungeecord");
  }

  /**
   * Start the update checker
   */
  public void start() {
    // Initial check
    check();

    // Schedule periodic checks
    plugin.getProxy().getScheduler().schedule(
        plugin,
        this::check,
        CHECK_INTERVAL_MINUTES,
        CHECK_INTERVAL_MINUTES,
        TimeUnit.MINUTES
    );

    plugin.logInfo("Update checker started (checking every 30 minutes)");
  }

  /**
   * Check for updates
   */
  private void check() {
    checker.check(new UpdateChecker.UpdateListener() {
      @Override
      public void onUpdateAvailable(String currentVersion, String newVersion, String downloadUrl) {
        plugin.logInfo("╔════════════════════════════════════════════════════════════╗");
        plugin.logInfo("║  A new version of ServerStats is available!                    ║");
        plugin.logInfo("║  Current: " + padRight(currentVersion, 20) + " Latest: " + padRight(newVersion, 20) + "║");
        plugin.logInfo("║  Download: " + padRight(downloadUrl, 47) + "║");
        plugin.logInfo("╚════════════════════════════════════════════════════════════╝");
      }

      @Override
      public void onUpToDate(String currentVersion) {
        if (plugin.isDebugEnabled()) {
          plugin.logInfo("[DEBUG] Plugin is up to date (v" + currentVersion + ")");
        }
      }

      @Override
      public void onCheckFailed(String error) {
        if (plugin.isDebugEnabled()) {
          plugin.logWarning("[DEBUG] Update check failed: " + error);
        }
      }
    }, false);
  }

  /**
   * Pad a string to the right with spaces
   *
   * @param s The string to pad
   * @param n The target length
   * @return The padded string
   */
  private String padRight(String s, int n) {
    return String.format("%-" + n + "s", s);
  }
}
