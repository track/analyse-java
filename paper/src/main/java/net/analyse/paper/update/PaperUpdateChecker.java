package net.analyse.paper.update;

import net.analyse.paper.AnalysePlugin;
import net.analyse.paper.util.ComponentUtil;
import net.analyse.sdk.update.UpdateChecker;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Paper-specific update checker with console and in-game notifications
 */
public class PaperUpdateChecker {

  private static final long CHECK_INTERVAL_TICKS = 20 * 60 * 30; // 30 minutes
  private static final String ADMIN_PERMISSION = "analyse.admin";

  private final AnalysePlugin plugin;
  private final UpdateChecker checker;
  private BukkitTask task;

  /**
   * Create a new Paper update checker
   *
   * @param plugin The plugin instance
   * @param currentVersion The current plugin version
   */
  public PaperUpdateChecker(AnalysePlugin plugin, String currentVersion) {
    this.plugin = plugin;
    this.checker = new UpdateChecker(plugin.getClient(), currentVersion, "paper");
  }

  /**
   * Start the update checker
   */
  public void start() {
    // Initial check
    check(false);

    // Schedule periodic checks
    task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
        plugin,
        () -> check(false),
        CHECK_INTERVAL_TICKS,
        CHECK_INTERVAL_TICKS
    );

    plugin.logInfo("Update checker started (checking every 30 minutes)");
  }

  /**
   * Stop the update checker
   */
  public void stop() {
    if (task != null) {
      task.cancel();
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

        // Notify online admins
        notifyAdmins(currentVersion, newVersion, downloadUrl);
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
   * Notify online admins about the update
   *
   * @param currentVersion The current plugin version
   * @param newVersion The new version available
   * @param downloadUrl The download URL
   */
  private void notifyAdmins(String currentVersion, String newVersion, String downloadUrl) {
    plugin.getServer().getScheduler().runTask(plugin, () -> {
      for (Player player : plugin.getServer().getOnlinePlayers()) {
        if (player.hasPermission(ADMIN_PERMISSION)) {
          sendUpdateMessage(player, currentVersion, newVersion, downloadUrl);
        }
      }
    });
  }

  /**
   * Send update message to a player
   *
   * @param player The player to notify
   * @param currentVersion The current plugin version
   * @param newVersion The new version available
   * @param downloadUrl The download URL
   */
  public void sendUpdateMessage(Player player, String currentVersion, String newVersion, String downloadUrl) {
    player.sendMessage(ComponentUtil.parse("<dark_gray><strikethrough>                                                </strikethrough></dark_gray>"));
    player.sendMessage(ComponentUtil.parse("<aqua><bold>Analyse</bold></aqua> <dark_gray>»</dark_gray> <gray>A new version is available!</gray>"));
    player.sendMessage(ComponentUtil.parse("<gray>Current: <red>%current%</red> <dark_gray>→</dark_gray> Latest: <green>%latest%</green></gray>",
        "current", currentVersion, "latest", newVersion));
    player.sendMessage(ComponentUtil.parse("<gray>Download: <white>%url%</white></gray>", "url", downloadUrl));
    player.sendMessage(ComponentUtil.parse("<dark_gray><strikethrough>                                                </strikethrough></dark_gray>"));
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
