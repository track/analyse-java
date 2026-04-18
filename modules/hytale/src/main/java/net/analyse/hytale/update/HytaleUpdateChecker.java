package net.analyse.hytale.update;

import net.analyse.hytale.HytalePlugin;
import net.analyse.sdk.update.UpdateChecker;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Hytale-specific update checker with console notifications
 */
public class HytaleUpdateChecker {

  private static final long CHECK_INTERVAL_MINUTES = 30;

  private final HytalePlugin plugin;
  private final UpdateChecker checker;
  private final ScheduledExecutorService scheduler;
  private ScheduledFuture<?> task;

  /**
   * Create a new Hytale update checker
   *
   * @param plugin The plugin instance
   * @param currentVersion The current plugin version
   * @param scheduler The scheduler to use for periodic checks
   */
  public HytaleUpdateChecker(HytalePlugin plugin, String currentVersion, ScheduledExecutorService scheduler) {
    this.plugin = plugin;
    this.checker = new UpdateChecker(plugin.getClient(), currentVersion, "hytale");
    this.scheduler = scheduler;
  }

  /**
   * Start the update checker
   */
  public void start() {
    // Initial check
    check(false);

    // Schedule periodic checks
    task = scheduler.scheduleAtFixedRate(
        () -> check(false),
        CHECK_INTERVAL_MINUTES,
        CHECK_INTERVAL_MINUTES,
        TimeUnit.MINUTES
    );

    plugin.logInfo("Update checker started (checking every 30 minutes)");
  }

  /**
   * Stop the update checker
   */
  public void stop() {
    if (task != null) {
      task.cancel(false);
      task = null;
    }
  }

  /**
   * Check for updates
   *
   * @param force If true, notify even if already notified for this version
   */
  public void check(boolean force) {
    checker.check(new UpdateChecker.UpdateListener() {
      @Override
      public void onUpdateAvailable(String currentVersion, String newVersion, String downloadUrl) {
        // Log to console
        plugin.logInfo("╔════════════════════════════════════════════════════════════╗");
        plugin.logInfo("║  A new version of Analyse is available!                    ║");
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
    }, force);
  }

  /**
   * Check if an update is available
   *
   * @return true if an update is available
   */
  public boolean isUpdateAvailable() {
    return checker.isUpdateAvailable();
  }

  /**
   * Get the latest version
   *
   * @return The latest version, or null if not yet checked
   */
  public String getLatestVersion() {
    return checker.getLatestVersion();
  }

  /**
   * Get the download URL
   *
   * @return The download URL, or null if not yet checked
   */
  public String getDownloadUrl() {
    return checker.getDownloadUrl();
  }

  /**
   * Get the current version
   *
   * @return The current plugin version
   */
  public String getCurrentVersion() {
    return checker.getCurrentVersion();
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
