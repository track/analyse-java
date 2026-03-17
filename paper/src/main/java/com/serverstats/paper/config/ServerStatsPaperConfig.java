package com.serverstats.paper.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for the Paper plugin
 */
@Getter
public class ServerStatsPaperConfig {

  private static final Map<String, Boolean> DEFAULT_EVENTS = Map.of(
      "chat", true,
      "command", true,
      "block-place", false,
      "block-break", false,
      "death", false,
      "kill-entity", false
  );

  private final JavaPlugin plugin;
  private boolean debug;
  private String apiKey;
  private String bedrockPrefix;
  private String instanceId;
  private Map<String, Boolean> events;

  /**
   * Create a new configuration from the plugin's config file
   *
   * @param plugin The plugin instance
   */
  public ServerStatsPaperConfig(JavaPlugin plugin) {
    this.plugin = plugin;
    plugin.saveDefaultConfig();
    loadValues();
  }

  /**
   * Load configuration values from the config file
   */
  private void loadValues() {
    FileConfiguration config = plugin.getConfig();

    this.debug = config.getBoolean("debug", false);
    this.apiKey = config.getString("api-key", "");
    this.bedrockPrefix = config.getString("bedrock-prefix", ".");
    this.instanceId = config.getString("instance-id", "default");

    // Load event toggles, writing missing defaults to the config file
    this.events = new HashMap<>(DEFAULT_EVENTS);
    boolean modified = false;
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
    loadValues();
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
   * Check if a username belongs to a Bedrock player
   *
   * @param username The player's username
   * @return true if the username starts with the bedrock prefix
   */
  public boolean isBedrock(String username) {
    if (bedrockPrefix == null || bedrockPrefix.isEmpty()) {
      return false;
    }

    return username.startsWith(bedrockPrefix);
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
}
