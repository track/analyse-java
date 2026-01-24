package com.serverstats.sdk.response;

import lombok.Getter;

/**
 * Response from the player join endpoint
 */
@Getter
public class JoinResponse {

  private boolean success;
  private String sessionId;
  private String playerId;
  private boolean isFirstJoin;
  private boolean attributed;
}

