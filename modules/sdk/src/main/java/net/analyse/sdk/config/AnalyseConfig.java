package net.analyse.sdk.config;

import lombok.Getter;

/**
 * Configuration for the Analyse SDK
 */
@Getter
public class AnalyseConfig {

  private static final String API_URL = "https://api.games.analyse.net";
  private static final String STAGING_API_URL = "https://api-staging.games.analyse.net";

  private final String apiUrl;
  private final String apiKey;
  private final BatchConfig batchConfig;

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
    this(apiKey, development, new BatchConfig());
  }

  /**
   * Create a new configuration with optional development mode and batch settings
   *
   * @param apiKey The API key for authentication
   * @param development Whether to use the staging API
   * @param batchConfig Batch delivery configuration
   */
  public AnalyseConfig(String apiKey, boolean development, BatchConfig batchConfig) {
    this(apiKey, development ? STAGING_API_URL : API_URL, batchConfig);
  }

  AnalyseConfig(String apiKey, String apiUrl, BatchConfig batchConfig) {
    if (apiKey == null || apiKey.trim().isEmpty()) {
      throw new IllegalArgumentException("API key cannot be null or blank");
    }

    this.apiUrl = (apiUrl == null || apiUrl.trim().isEmpty()) ? API_URL : apiUrl;
    this.apiKey = apiKey;
    this.batchConfig = batchConfig != null ? batchConfig : new BatchConfig();
  }
}
