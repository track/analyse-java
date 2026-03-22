package net.analyse.sdk.config;

import lombok.Getter;

/**
 * Configuration for the Analyse SDK
 */
@Getter
public class AnalyseConfig {

  private static final String API_URL = "https://api.analyse.com";

  private final String apiUrl;
  private final String apiKey;

  /**
   * Create a new configuration
   *
   * @param apiKey The API key for authentication
   */
  public AnalyseConfig(String apiKey) {
    if (apiKey == null || apiKey.trim().isEmpty()) {
      throw new IllegalArgumentException("API key cannot be null or blank");
    }

    this.apiUrl = API_URL;
    this.apiKey = apiKey;
  }
}
