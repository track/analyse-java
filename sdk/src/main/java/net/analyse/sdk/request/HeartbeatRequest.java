package net.analyse.sdk.request;

import lombok.Getter;
import java.util.List;

/**
 * Request payload for the heartbeat endpoint
 */
@Getter
public class HeartbeatRequest {

  private final String instanceId;
  private final List<PlayerInfo> onlinePlayers;

  /**
   * Create a new heartbeat request with player info (includes hostnames)
   *
   * @param instanceId    The instance identifier (optional, defaults to "default" if null/blank)
   * @param onlinePlayers List of player info objects with UUIDs and hostnames
   */
  public HeartbeatRequest(String instanceId, List<PlayerInfo> onlinePlayers) {
    if (onlinePlayers == null) {
      throw new IllegalArgumentException("Online players list cannot be null");
    }

    this.instanceId = (instanceId == null || instanceId.isBlank()) ? "default" : instanceId;
    this.onlinePlayers = onlinePlayers;
  }
}
