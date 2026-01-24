package com.serverstats.sdk.request;

import lombok.Getter;

/**
 * Request payload for the player leave endpoint
 */
@Getter
public class LeaveRequest {

  private final String sessionId;

  /**
   * Create a new leave request
   *
   * @param sessionId The session ID returned from the join endpoint
   */
  public LeaveRequest(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      throw new IllegalArgumentException("Session ID cannot be null or blank");
    }

    this.sessionId = sessionId;
  }
}
