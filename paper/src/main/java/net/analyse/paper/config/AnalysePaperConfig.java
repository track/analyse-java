package net.analyse.paper.config;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Configuration for the Paper plugin
 */
@Getter
public class AnalysePaperConfig {

  private final String apiKey;
  private final String bedrockPrefix;

  /**
   * Create a new configuration from the plugin's config file
   *
   * @param plugin The plugin instance
   */
  public AnalysePaperConfig(JavaPlugin plugin) {
    plugin.saveDefaultConfig();
    FileConfiguration config = plugin.getConfig();

    this.apiKey = config.getString("api-key", "");
    this.bedrockPrefix = config.getString("bedrock-prefix", ".");
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
}
