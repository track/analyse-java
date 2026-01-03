package net.analyse.sdk.request;

import lombok.Getter;
import java.util.List;
import java.util.UUID;

/**
 * Request payload for the heartbeat endpoint
 */
@Getter
public class HeartbeatRequest {

  private final List<String> onlinePlayers;

  /**
   * Create a new heartbeat request
   *
   * @param onlinePlayers List of UUIDs of currently online players
   */
  public HeartbeatRequest(List<UUID> onlinePlayers) {
    if (onlinePlayers == null) {
      throw new IllegalArgumentException("Online players list cannot be null");
    }

    this.onlinePlayers = onlinePlayers.stream()
        .map(UUID::toString)
        .toList();
  }
}

