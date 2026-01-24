package com.serverstats.sdk.update;

import lombok.Getter;
import com.serverstats.sdk.ServerStatsCallback;
import com.serverstats.sdk.ServerStatsClient;
import com.serverstats.api.exception.ServerStatsException;
import com.serverstats.sdk.response.VersionResponse;

/**
 * Shared update checker for all platforms
 */
@Getter
public class UpdateChecker {

  /**
   * Listener interface for update check results
   */
  public interface UpdateListener {

    /**
     * Called when an update is available
     *
     * @param currentVersion The current plugin version
     * @param newVersion The new version available
     * @param downloadUrl The download URL for the update
     */
    void onUpdateAvailable(String currentVersion, String newVersion, String downloadUrl);

    /**
     * Called when the plugin is up to date
     *
     * @param currentVersion The current plugin version
     */
    void onUpToDate(String currentVersion);

    /**
     * Called when the update check fails
     *
     * @param error The error message
     */
    void onCheckFailed(String error);
  }

  private final ServerStatsClient client;
  private final String currentVersion;
  private final String platform;

  private String lastNotifiedVersion = null;
  private boolean updateAvailable = false;
  private String latestVersion = null;
  private String downloadUrl = null;

  /**
   * Create a new update checker
   *
   * @param client The ServerStats client
   * @param currentVersion The current plugin version
   * @param platform The platform identifier (paper, velocity, bungeecord)
   */
  public UpdateChecker(ServerStatsClient client, String currentVersion, String platform) {
    this.client = client;
    this.currentVersion = currentVersion;
    this.platform = platform;
  }

  /**
   * Check for updates
   *
   * @param listener Callback for update status
   * @param force If true, notify even if already notified for this version
   */
  public void check(UpdateListener listener, boolean force) {
    client.checkVersion(new ServerStatsCallback<VersionResponse>() {
      @Override
      public void onSuccess(VersionResponse response) {
        if (!response.isSuccess() || response.getPlatforms() == null) {
          listener.onCheckFailed("Invalid response from server");
          return;
        }

        VersionResponse.PlatformInfo platformInfo = response.getPlatforms().get(platform);
        if (platformInfo == null) {
          listener.onCheckFailed("Platform not found in response");
          return;
        }

        latestVersion = platformInfo.getVersion();
        downloadUrl = platformInfo.getDownloadUrl();

        if (isNewerVersion(currentVersion, latestVersion)) {
          updateAvailable = true;

          // Only notify if this is a new version we haven't notified about
          if (force || !latestVersion.equals(lastNotifiedVersion)) {
            lastNotifiedVersion = latestVersion;
            listener.onUpdateAvailable(currentVersion, latestVersion, downloadUrl);
          }
        } else {
          updateAvailable = false;
          listener.onUpToDate(currentVersion);
        }
      }

      @Override
      public void onError(ServerStatsException exception) {
        listener.onCheckFailed(exception.getMessage());
      }
    });
  }

  /**
   * Simple version comparison (semver-like).
   * Returns true if newVersion is newer than currentVersion
   *
   * @param current The current version
   * @param newer The version to compare against
   * @return true if newer is a higher version than current
   */
  private boolean isNewerVersion(String current, String newer) {
    if (current == null || newer == null) {
      return false;
    }

    if (current.equals(newer)) {
      return false;
    }

    String[] currentParts = current.split("\\.");
    String[] newerParts = newer.split("\\.");

    int maxLength = Math.max(currentParts.length, newerParts.length);
    for (int i = 0; i < maxLength; i++) {
      int currentPart = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;
      int newerPart = i < newerParts.length ? parseVersionPart(newerParts[i]) : 0;

      if (newerPart > currentPart) {
        return true;
      }

      if (newerPart < currentPart) {
        return false;
      }
    }

    return false;
  }

  /**
   * Parse a version part, handling suffixes like -SNAPSHOT
   *
   * @param part The version part to parse
   * @return The numeric value
   */
  private int parseVersionPart(String part) {
    try {
      // Handle versions like "1.0.0-SNAPSHOT" - extract numeric part
      String numeric = part.replaceAll("[^0-9].*", "");
      return numeric.isEmpty() ? 0 : Integer.parseInt(numeric);
    } catch (NumberFormatException e) {
      return 0;
    }
  }
}
