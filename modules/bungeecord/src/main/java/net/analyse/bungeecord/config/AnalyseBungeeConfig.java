package net.analyse.bungeecord.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for the BungeeCord plugin
 */
@Getter
public class AnalyseBungeeConfig {

  private static final String CONFIG_FILE = "config.json";

  private static final transient Map<String, Boolean> DEFAULT_EVENTS = createDefaultEvents();

  /**
   * Build the default event toggles map used when merging config
   *
   * @return A new map with built-in event keys and default enabled flags
   */
  private static Map<String, Boolean> createDefaultEvents() {
    Map<String, Boolean> map = new HashMap<>();
    map.put("command", true);
    map.put("chat", true);
    map.put("serverSwitch", true);
    return map;
  }

  @Setter
  private boolean debug = false;
  private boolean development = false;
  private String bedrockPrefix = ".";
  private String instanceId = "default";
  private String defaultServer = null;
  private Map<String, ServerConfig> servers = new HashMap<>();
  private Map<String, Boolean> events = new HashMap<>(DEFAULT_EVENTS);

  /**
   * Server-specific configuration
   */
  @Getter
  public static class ServerConfig {
    private String apiKey = "";

    public ServerConfig() {
    }

    public ServerConfig(String apiKey) {
      this.apiKey = apiKey;
    }
  }

  /**
   * Load configuration from the data directory
   *
   * @param dataDirectory The plugin's data directory
   * @return The loaded configuration
   * @throws IOException If the config cannot be read or written
   */
  public static AnalyseBungeeConfig load(Path dataDirectory) throws IOException {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    Path configPath = dataDirectory.resolve(CONFIG_FILE);

    // Create data directory if it doesn't exist
    if (!Files.exists(dataDirectory)) {
      Files.createDirectories(dataDirectory);
    }

    // Create default config if it doesn't exist
    if (!Files.exists(configPath)) {
      AnalyseBungeeConfig defaultConfig = createDefault();
      try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
        gson.toJson(defaultConfig, writer);
      }

      return defaultConfig;
    }

    // Load existing config and fill in any missing event defaults
    AnalyseBungeeConfig config;
    try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
      config = gson.fromJson(reader, AnalyseBungeeConfig.class);
    }

    if (config.fillMissingEventDefaults()) {
      try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
        gson.toJson(config, writer);
      }
    }

    return config;
  }

  /**
   * Create a default configuration with example servers
   *
   * @return The default configuration
   */
  private static AnalyseBungeeConfig createDefault() {
    AnalyseBungeeConfig config = new AnalyseBungeeConfig();
    config.debug = false;
    config.bedrockPrefix = ".";
    config.instanceId = "default";
    config.servers.put("lobby", new ServerConfig("anl_your_lobby_key_here"));
    config.servers.put("survival", new ServerConfig("anl_your_survival_key_here"));
    config.events = new HashMap<>(DEFAULT_EVENTS);
    return config;
  }

  /**
   * Get the API key for a specific server
   *
   * @param serverName The name of the server
   * @return The API key, or null if not configured
   */
  public String getApiKeyForServer(String serverName) {
    ServerConfig serverConfig = servers.get(serverName);
    if (serverConfig == null) {
      return null;
    }

    return serverConfig.getApiKey();
  }

  /**
   * Check if a server is configured
   *
   * @param serverName The name of the server
   * @return true if the server has a configuration
   */
  public boolean isServerConfigured(String serverName) {
    String apiKey = getApiKeyForServer(serverName);
    return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.startsWith("anl_your_");
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
   * Fill in any missing event keys from defaults
   *
   * @return true if any keys were added (config should be re-saved)
   */
  private boolean fillMissingEventDefaults() {
    if (events == null) {
      events = new HashMap<>(DEFAULT_EVENTS);
      return true;
    }

    boolean modified = false;
    for (Map.Entry<String, Boolean> entry : DEFAULT_EVENTS.entrySet()) {
      if (!events.containsKey(entry.getKey())) {
        events.put(entry.getKey(), entry.getValue());
        modified = true;
      }
    }

    return modified;
  }

  /**
   * Check if a built-in event type is enabled
   *
   * @param key The event key (e.g. "command", "chat", "serverSwitch")
   * @return true if the event is enabled in config
   */
  public boolean isEventEnabled(String key) {
    if (events == null) {
      return DEFAULT_EVENTS.getOrDefault(key, false);
    }

    return events.getOrDefault(key, false);
  }
}
