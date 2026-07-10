package net.analyse.spigot.config;

import lombok.Getter;
import net.analyse.sdk.config.BatchConfig;
import net.analyse.sdk.config.BedrockMode;
import net.analyse.sdk.util.BedrockUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Configuration for the Paper plugin
 */
@Getter
public class AnalyseSpigotConfig {

  private static final Map<String, Boolean> DEFAULT_EVENTS;

  static {
    Map<String, Boolean> defaults = new HashMap<>();
    defaults.put("chat", true);
    defaults.put("command", true);
    defaults.put("block-place", false);
    defaults.put("block-break", false);
    defaults.put("death", false);
    defaults.put("kill-entity", false);
    DEFAULT_EVENTS = defaults;
  }

  private final JavaPlugin plugin;
  private boolean debug;
  private boolean development;
  private String apiKey;
  private BedrockMode bedrockMode;
  private String bedrockPrefix;
  private String instanceId;
  private BatchConfig batchConfig;
  private Map<String, Boolean> events;

  /**
   * Create a new configuration from the plugin's config file
   *
   * @param plugin The plugin instance
   */
  public AnalyseSpigotConfig(JavaPlugin plugin) {
    this.plugin = plugin;
    plugin.saveDefaultConfig();
    migrateConfigFile();
    plugin.reloadConfig();
    loadValues();
  }

  /**
   * Load configuration values from the config file
   */
  private void loadValues() {
    FileConfiguration config = plugin.getConfig();

    this.debug = config.getBoolean("debug", false);
    this.development = config.getBoolean("development", false);
    this.apiKey = config.getString("api-key", "");
    this.bedrockMode = BedrockMode.fromConfig(config.getString("bedrock-mode", "NAME"));
    this.bedrockPrefix = config.getString("bedrock-prefix", ".");
    this.instanceId = config.getString("instance-id", "default");
    boolean modified = false;
    if (!config.contains("batch.size")) {
      config.set("batch.size", BatchConfig.DEFAULT_SIZE);
      modified = true;
    }
    if (!config.contains("batch.flush-interval-seconds")) {
      config.set("batch.flush-interval-seconds", BatchConfig.DEFAULT_FLUSH_INTERVAL_SECONDS);
      modified = true;
    }
    if (!config.contains("batch.max-queue-size")) {
      config.set("batch.max-queue-size", BatchConfig.DEFAULT_MAX_QUEUE_SIZE);
      modified = true;
    }
    if (!config.contains("batch.max-retries")) {
      config.set("batch.max-retries", BatchConfig.DEFAULT_MAX_RETRIES);
      modified = true;
    }
    this.batchConfig = new BatchConfig(
        config.getInt("batch.size", BatchConfig.DEFAULT_SIZE),
        config.getInt("batch.flush-interval-seconds", BatchConfig.DEFAULT_FLUSH_INTERVAL_SECONDS),
        config.getInt("batch.max-queue-size", BatchConfig.DEFAULT_MAX_QUEUE_SIZE),
        config.getInt("batch.max-retries", BatchConfig.DEFAULT_MAX_RETRIES)
    );

    // Load event toggles, writing missing defaults to the config file
    this.events = new HashMap<>(DEFAULT_EVENTS);
    for (Map.Entry<String, Boolean> entry : DEFAULT_EVENTS.entrySet()) {
      String path = "events." + entry.getKey();
      if (config.contains(path)) {
        this.events.put(entry.getKey(), config.getBoolean(path));
      } else {
        config.set(path, entry.getValue());
        modified = true;
      }
    }
    if (modified) {
      plugin.saveConfig();
    }
  }

  /**
   * Reload configuration from the config file
   */
  public void reload() {
    migrateConfigFile();
    plugin.reloadConfig();
    loadValues();
  }

  /**
   * Check if the configuration is valid
   *
   * @return true if the API key is configured
   */
  public boolean isValid() {
    return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.startsWith("anl_your_");
  }

  /**
   * Check if a player is connecting from Bedrock Edition.
   *
   * <p>In NAME mode the username is matched against the configured prefix; in
   * UUID mode the player's UUID is inspected for the Floodgate pattern.
   *
   * @param uuid     The player's UUID
   * @param username The player's username
   * @return true if the player is detected as a Bedrock player
   */
  public boolean isBedrock(UUID uuid, String username) {
    if (bedrockMode == BedrockMode.UUID) {
      return BedrockUtil.isBedrockUuid(uuid);
    }

    if (bedrockPrefix == null || bedrockPrefix.isEmpty()) {
      return false;
    }

    return username != null && username.startsWith(bedrockPrefix);
  }

  /**
   * Set debug mode at runtime
   *
   * @param debug Whether debug mode should be enabled
   */
  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  /**
   * Check if a built-in event type is enabled
   *
   * @param key The event key (e.g. "chat", "command", "block-place")
   * @return true if the event is enabled in config
   */
  public boolean isEventEnabled(String key) {
    return events.getOrDefault(key, false);
  }

  /**
   * Append new documented config sections to older config.yml files.
   */
  private void migrateConfigFile() {
    File configFile = new File(plugin.getDataFolder(), "config.yml");
    if (!configFile.exists()) {
      return;
    }

    try {
      String content = new String(Files.readAllBytes(configFile.toPath()), StandardCharsets.UTF_8);
      boolean hasBatch = content.contains("batch:");
      boolean hasBedrockMode = content.contains("bedrock-mode:");
      if (hasBatch && hasBedrockMode) {
        return;
      }

      StringBuilder block = new StringBuilder();
      if (!content.endsWith("\n")) {
        block.append("\n");
      }
      block.append("\n");
      if (!hasBedrockMode) {
        block.append("# How Bedrock (Floodgate/Geyser) players are detected.\n");
        block.append("# NAME: match the username against bedrock-prefix.\n");
        block.append("# UUID: detect by UUID - Floodgate gives Bedrock players a UUID of the form\n");
        block.append("#       00000000-0000-0000-xxxx-xxxxxxxxxxxx. More reliable, as it won't flag\n");
        block.append("#       Java players whose name happens to start with the prefix.\n");
        block.append("bedrock-mode: \"NAME\"\n");
        block.append("\n");
      }
      if (!hasBatch) {
        block.append("# Batch sending settings. Joins, leaves, and custom events are queued in memory\n");
        block.append("# and sent together using /v1/plugin/batch.\n");
        block.append("batch:\n");
        block.append("  # Number of queued items that triggers an immediate flush.\n");
        block.append("  # Recommended: 100-250. Maximum API batch size is 1000.\n");
        block.append("  size: 200\n");
        block.append("\n");
        block.append("  # How often queued items are flushed, in seconds.\n");
        block.append("  # Recommended: 2-5 seconds.\n");
        block.append("  flush-interval-seconds: 3\n");
        block.append("\n");
        block.append("  # Maximum queued items kept in memory before new items are rejected.\n");
        block.append("  max-queue-size: 10000\n");
        block.append("\n");
        block.append("  # Number of retry attempts for temporary HTTP/network failures.\n");
        block.append("  max-retries: 3\n");
      }

      Files.write(configFile.toPath(), block.toString().getBytes(StandardCharsets.UTF_8),
          java.nio.file.StandardOpenOption.APPEND);
    } catch (IOException e) {
      plugin.getLogger().warning("Failed to migrate Analyse config.yml with batch settings: " + e.getMessage());
    }
  }
}
