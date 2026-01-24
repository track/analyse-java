package com.serverstats.sdk.config;

import lombok.Getter;

/**
 * Configuration for the ServerStats SDK
 */
@Getter
public class ServerStatsConfig {

  private static final String API_URL = "https://api.serverstats.com";

  private final String apiUrl;
  private final String apiKey;

  /**
   * Create a new configuration
   *
   * @param apiKey The API key for authentication
   */
  public ServerStatsConfig(String apiKey) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalArgumentException("API key cannot be null or blank");
    }

    this.apiUrl = API_URL;
    this.apiKey = apiKey;
  }
}
