package net.analyse.sdk.response;

import lombok.Getter;

/**
 * Response from the player join endpoint
 */
@Getter
public class JoinResponse {

  private boolean success;
  private String sessionId;
  private String instanceId;
  private String playerId;
  private boolean isFirstJoin;
  private boolean attributed;

  public JoinResponse() {
  }

  public JoinResponse(boolean success, String sessionId, String instanceId, String playerId, boolean isFirstJoin,
      boolean attributed) {
    this.success = success;
    this.sessionId = sessionId;
    this.instanceId = instanceId;
    this.playerId = playerId;
    this.isFirstJoin = isFirstJoin;
    this.attributed = attributed;
  }
}
