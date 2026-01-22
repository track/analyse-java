package com.serverstats.sdk.response;

import lombok.Getter;

/**
 * Response from the custom event endpoint
 */
@Getter
public class EventResponse {

  private boolean success;
  private String eventId;
}
