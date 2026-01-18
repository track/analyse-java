package net.analyse.sdk.response;

import lombok.Getter;

/**
 * Response from the server info endpoint containing real-time server analytics
 */
@Getter
public class ServerInfoResponse {

  private boolean success;
  private int onlinePlayers;
  private int trackedSessions;
  private int peakToday;
  private int totalJoinsToday;
  private int uniquePlayersToday;
}
