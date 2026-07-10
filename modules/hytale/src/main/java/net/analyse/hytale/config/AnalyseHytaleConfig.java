package net.analyse.hytale.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import lombok.Getter;
import net.analyse.sdk.config.BatchConfig;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration for the Hytale plugin
 */
@Getter
public class AnalyseHytaleConfig {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private boolean debug;
  private boolean development;
  private String apiKey;
  private String instanceId;
  private BatchSettings batch;

  /**
   * Batch delivery configuration.
   */
  @Getter
  public static class BatchSettings {
    private int size = BatchConfig.DEFAULT_SIZE;
    private int flushIntervalSeconds = BatchConfig.DEFAULT_FLUSH_INTERVAL_SECONDS;
    private int maxQueueSize = BatchConfig.DEFAULT_MAX_QUEUE_SIZE;
    private int maxRetries = BatchConfig.DEFAULT_MAX_RETRIES;
  }

  /**
   * Load or create the configuration file
   *
   * @param plugin The plugin instance
   * @return The loaded configuration
   */
  public static AnalyseHytaleConfig load(JavaPlugin plugin) {
    Path configPath = plugin.getDataDirectory().resolve("config.json");

    // If config doesn't exist, create default
    if (!Files.exists(configPath)) {
      AnalyseHytaleConfig defaultConfig = createDefault();
      defaultConfig.save(configPath);
      return defaultConfig;
    }

    // Load existing config
    try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
      AnalyseHytaleConfig config = GSON.fromJson(reader, AnalyseHytaleConfig.class);
      if (config.fillMissingDefaults()) {
        config.save(configPath);
      }
      return config;
    } catch (IOException e) {
      plugin.getLogger().atWarning().log("Failed to load config.json, using defaults: %s", e.getMessage());
      return createDefault();
    }
  }

  /**
   * Create a default configuration
   *
   * @return The default config
   */
  private static AnalyseHytaleConfig createDefault() {
    AnalyseHytaleConfig config = new AnalyseHytaleConfig();
    config.debug = false;
    config.development = false;
    config.apiKey = "";
    config.instanceId = "default";
    config.batch = new BatchSettings();
    return config;
  }

  /**
   * Save the configuration to a file
   *
   * @param path The path to save to
   */
  public void save(Path path) {
    try {
      Files.createDirectories(path.getParent());
      Files.writeString(path, GSON.toJson(this), StandardCharsets.UTF_8);
    } catch (IOException e) {
      // Silently fail - will use defaults
    }
  }

  /**
   * Check if the configuration is valid
   *
   * @return true if the API key is configured
   */
  public boolean isValid() {
    return apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("anl_your_");
  }

  /**
   * Set debug mode at runtime
   *
   * @param debug Whether debug mode should be enabled
   */
  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  private boolean fillMissingDefaults() {
    boolean modified = false;
    if (batch == null) {
      batch = new BatchSettings();
      modified = true;
    }
    if (instanceId == null || instanceId.isBlank()) {
      instanceId = "default";
      modified = true;
    }
    return modified;
  }

  public BatchConfig getBatchConfig() {
    BatchSettings settings = batch != null ? batch : new BatchSettings();
    return new BatchConfig(
        settings.getSize(),
        settings.getFlushIntervalSeconds(),
        settings.getMaxQueueSize(),
        settings.getMaxRetries()
    );
  }
}
