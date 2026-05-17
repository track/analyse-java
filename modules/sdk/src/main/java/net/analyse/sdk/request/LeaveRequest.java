package net.analyse.sdk.request;

import lombok.Getter;
import java.util.UUID;

/**
 * Request payload for the player leave endpoint
 */
@Getter
public class LeaveRequest {

  private final String sessionId;
  private final String playerUuid;
  private final String instanceId;

  /**
   * Create a new leave request
   *
   * @param sessionId The session ID returned from the join endpoint
   */
  public LeaveRequest(String sessionId) {
    this(sessionId, null, null);
  }

  /**
   * Create a new leave request using the player UUID fallback.
   *
   * @param playerUuid The player's UUID
   * @param instanceId The instance identifier
   */
  public LeaveRequest(UUID playerUuid, String instanceId) {
    this(null, playerUuid, instanceId);
  }

  /**
   * Create a new leave request.
   *
   * @param sessionId The session ID returned from the join endpoint
   * @param playerUuid The player's UUID fallback if sessionId is unavailable
   * @param instanceId The instance identifier
   */
  public LeaveRequest(String sessionId, UUID playerUuid, String instanceId) {
    boolean hasSessionId = sessionId != null && !sessionId.trim().isEmpty();
    if (!hasSessionId && playerUuid == null) {
      throw new IllegalArgumentException("Session ID or player UUID is required");
    }

    this.sessionId = hasSessionId ? sessionId : null;
    this.playerUuid = playerUuid != null ? playerUuid.toString() : null;
    this.instanceId = (instanceId == null || instanceId.trim().isEmpty()) ? "default" : instanceId;
  }
}
