package net.analyse.sdk.response;

import lombok.Getter;

/**
 * Response from the player leave endpoint
 */
@Getter
public class LeaveResponse {

  private boolean success;
  private int duration;
  private String instanceId;
  private String sessionId;

  public LeaveResponse() {
  }

  public LeaveResponse(boolean success, int duration, String instanceId, String sessionId) {
    this.success = success;
    this.duration = duration;
    this.instanceId = instanceId;
    this.sessionId = sessionId;
  }
}
