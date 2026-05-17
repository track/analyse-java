package net.analyse.sdk.response;

import lombok.Getter;

/**
 * Response from the custom event endpoint
 */
@Getter
public class EventResponse {

  private boolean success;
  private String eventId;
  private String event;
  private String instanceId;

  public EventResponse() {
  }

  public EventResponse(boolean success, String eventId, String event, String instanceId) {
    this.success = success;
    this.eventId = eventId;
    this.event = event;
    this.instanceId = instanceId;
  }
}
