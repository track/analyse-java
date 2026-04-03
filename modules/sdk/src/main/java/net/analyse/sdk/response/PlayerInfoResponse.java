package net.analyse.sdk.response;

import lombok.Getter;

/**
 * Response from the player info endpoint containing detailed player analytics
 */
@Getter
public class PlayerInfoResponse {

  private boolean success;
  private String uuid;
  private String username;
  private boolean online;
  private long currentSessionSeconds;
  private long totalPlaytimeSeconds;
  private int totalSessions;
  private String firstSeen;
  private String lastSeen;
  private String campaign;
  private String country;
}
