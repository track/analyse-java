package net.analyse.sdk.request;

import lombok.Getter;
import java.util.UUID;

/**
 * Request payload for the player join endpoint
 */
@Getter
public class JoinRequest {

  private final String uuid;
  private final String username;
  private final String hostname;
  private final String ip;
  private final boolean bedrock;

  /**
   * Create a new join request
   *
   * @param uuid      The player's Minecraft UUID
   * @param username  The player's current username
   * @param hostname  The hostname the player used to connect
   * @param ip        The player's IP address (for geo lookup)
   * @param isBedrock Whether the player is connecting from Bedrock Edition
   */
  public JoinRequest(UUID uuid, String username, String hostname, String ip, boolean isBedrock) {
    if (uuid == null) {
      throw new IllegalArgumentException("UUID cannot be null");
    }

    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("Username cannot be null or blank");
    }

    if (hostname == null || hostname.isBlank()) {
      throw new IllegalArgumentException("Hostname cannot be null or blank");
    }

    if (ip == null || ip.isBlank()) {
      throw new IllegalArgumentException("IP cannot be null or blank");
    }

    this.uuid = uuid.toString();
    this.username = username;
    this.hostname = hostname;
    this.ip = ip;
    this.bedrock = isBedrock;
  }
}
