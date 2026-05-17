package net.analyse.sdk.config;

import lombok.Getter;

/**
 * Configuration for the Analyse SDK
 */
@Getter
public class AnalyseConfig {

  private static final String API_URL = "https://api.analyse.net";
  private static final String STAGING_API_URL = "https://api-staging.analyse.net";

  private final String apiUrl;
  private final String apiKey;
  private final SendMode sendMode;
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
    this(apiKey, development, SendMode.SINGLE, new BatchConfig());
  }

  /**
   * Create a new configuration with optional development mode and send mode
   *
   * @param apiKey The API key for authentication
   * @param development Whether to use the staging API
   * @param sendMode How analytics items should be sent
   * @param batchConfig Batch delivery configuration
   */
  public AnalyseConfig(String apiKey, boolean development, SendMode sendMode, BatchConfig batchConfig) {
    this(apiKey, development ? STAGING_API_URL : API_URL, sendMode, batchConfig);
  }

  AnalyseConfig(String apiKey, String apiUrl, SendMode sendMode, BatchConfig batchConfig) {
    if (apiKey == null || apiKey.trim().isEmpty()) {
      throw new IllegalArgumentException("API key cannot be null or blank");
    }

    this.apiUrl = (apiUrl == null || apiUrl.trim().isEmpty()) ? API_URL : apiUrl;
    this.apiKey = apiKey;
    this.sendMode = sendMode != null ? sendMode : SendMode.SINGLE;
    this.batchConfig = batchConfig != null ? batchConfig : new BatchConfig();
  }
}
