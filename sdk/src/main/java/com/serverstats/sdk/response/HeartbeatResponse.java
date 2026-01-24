package com.serverstats.sdk.response;

import lombok.Getter;

/**
 * Response from the heartbeat endpoint
 */
@Getter
public class HeartbeatResponse {

  private boolean success;
  private int onlineCount;
}

