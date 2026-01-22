package com.serverstats.sdk.request;

import lombok.Getter;
import java.util.UUID;

/**
 * Request payload for the player info endpoint
 */
@Getter
public class PlayerInfoRequest {

  private final String uuid;

  /**
   * Create a new player info request
   *
   * @param uuid The player's Minecraft UUID
   */
  public PlayerInfoRequest(UUID uuid) {
    if (uuid == null) {
      throw new IllegalArgumentException("UUID cannot be null");
    }

    this.uuid = uuid.toString();
  }

  /**
   * Create a new player info request from a string UUID
   *
   * @param uuid The player's Minecraft UUID as a string
   */
  public PlayerInfoRequest(String uuid) {
    if (uuid == null || uuid.isBlank()) {
      throw new IllegalArgumentException("UUID cannot be null or blank");
    }

    this.uuid = uuid;
  }
}
