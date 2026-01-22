package com.serverstats.sdk.response;

import lombok.Getter;

/**
 * Response from the player leave endpoint
 */
@Getter
public class LeaveResponse {

  private boolean success;
  private int duration;
}

