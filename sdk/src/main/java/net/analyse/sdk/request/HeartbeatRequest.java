package net.analyse.sdk.request;

import lombok.Getter;
import java.util.List;
import java.util.UUID;

/**
 * Request payload for the heartbeat endpoint
 */
@Getter
public class HeartbeatRequest {

  private final String instanceId;
  private final List<String> onlinePlayers;

  /**
   * Create a new heartbeat request
   *
   * @param instanceId The instance identifier (optional, defaults to "default" if null/blank)
   * @param onlinePlayers List of UUIDs of currently online players
   */
  public HeartbeatRequest(String instanceId, List<UUID> onlinePlayers) {
    if (onlinePlayers == null) {
      throw new IllegalArgumentException("Online players list cannot be null");
    }

    this.instanceId = (instanceId == null || instanceId.isBlank()) ? "default" : instanceId;
    this.onlinePlayers = onlinePlayers.stream()
        .map(UUID::toString)
        .toList();
  }
}

