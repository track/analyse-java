package net.analyse.velocity.config;

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
 * Configuration for the Velocity plugin
 */
@Getter
public class AnalyseVelocityConfig {

  private static final String CONFIG_FILE = "config.json";

  @Setter
  private boolean debug = false;
  private String bedrockPrefix = ".";
  private String instanceId = "default";
  private String defaultServer = null;
  private Map<String, ServerConfig> servers = new HashMap<>();

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
  public static AnalyseVelocityConfig load(Path dataDirectory) throws IOException {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    Path configPath = dataDirectory.resolve(CONFIG_FILE);

    // Create data directory if it doesn't exist
    if (!Files.exists(dataDirectory)) {
      Files.createDirectories(dataDirectory);
    }

    // Create default config if it doesn't exist
    if (!Files.exists(configPath)) {
      AnalyseVelocityConfig defaultConfig = createDefault();
      try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
        gson.toJson(defaultConfig, writer);
      }

      return defaultConfig;
    }

    // Load existing config
    try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
      return gson.fromJson(reader, AnalyseVelocityConfig.class);
    }
  }

  /**
   * Create a default configuration with example servers
   *
   * @return The default configuration
   */
  private static AnalyseVelocityConfig createDefault() {
    AnalyseVelocityConfig config = new AnalyseVelocityConfig();
    config.debug = false;
    config.bedrockPrefix = ".";
    config.instanceId = "default";
    config.servers.put("lobby", new ServerConfig("anl_your_lobby_key_here"));
    config.servers.put("survival", new ServerConfig("anl_your_survival_key_here"));
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
}
