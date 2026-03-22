package net.analyse.sdk.config;

import lombok.Getter;

/**
 * Configuration for the Analyse SDK
 */
@Getter
public class AnalyseConfig {

  private static final String API_URL = "https://api.analyse.net";
  private static final String STAGING_API_URL = "https://staging.analyse.net";

  private final String apiUrl;
  private final String apiKey;

  /**
   * Create a new configuration
   *
   * @param apiKey The API key for authentication
   */
  public AnalyseConfig(String apiKey) {
    this(apiKey, false);
  }

  /**
   * Create a new configuration with optional development mode
   *
   * @param apiKey The API key for authentication
   * @param development Whether to use the staging API
   */
  public AnalyseConfig(String apiKey, boolean development) {
    if (apiKey == null || apiKey.trim().isEmpty()) {
      throw new IllegalArgumentException("API key cannot be null or blank");
    }

    this.apiUrl = development ? STAGING_API_URL : API_URL;
    this.apiKey = apiKey;
  }
}
