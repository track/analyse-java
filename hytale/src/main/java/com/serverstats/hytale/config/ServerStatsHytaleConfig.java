package com.serverstats.hytale.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration for the Hytale plugin
 */
@Getter
public class ServerStatsHytaleConfig {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private boolean debug;
  private String apiKey;
  private String instanceId;

  /**
   * Load or create the configuration file
   *
   * @param plugin The plugin instance
   * @return The loaded configuration
   */
  public static ServerStatsHytaleConfig load(JavaPlugin plugin) {
    Path configPath = plugin.getDataDirectory().resolve("config.json");

    // If config doesn't exist, create default
    if (!Files.exists(configPath)) {
      ServerStatsHytaleConfig defaultConfig = createDefault();
      defaultConfig.save(configPath);
      return defaultConfig;
    }

    // Load existing config
    try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
      return GSON.fromJson(reader, ServerStatsHytaleConfig.class);
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
  private static ServerStatsHytaleConfig createDefault() {
    ServerStatsHytaleConfig config = new ServerStatsHytaleConfig();
    config.debug = false;
    config.apiKey = "";
    config.instanceId = "default";
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
}
