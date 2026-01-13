package net.analyse.velocity.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for parsing text with colors into Adventure Components.
 * Supports MiniMessage, legacy color codes, and hex colors.
 */
public final class ComponentUtil {

  private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
      .strict(false)
      .build();

  // Hex color patterns
  private static final Pattern HEX_PATTERN = Pattern.compile("#([A-Fa-f0-9]{6})");
  private static final Pattern AMPERSAND_HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

  // Legacy color code mappings
  private static final Map<String, String> LEGACY_COLORS = Map.ofEntries(
      Map.entry("&0", "<black>"),
      Map.entry("&1", "<dark_blue>"),
      Map.entry("&2", "<dark_green>"),
      Map.entry("&3", "<dark_aqua>"),
      Map.entry("&4", "<dark_red>"),
      Map.entry("&5", "<dark_purple>"),
      Map.entry("&6", "<gold>"),
      Map.entry("&7", "<gray>"),
      Map.entry("&8", "<dark_gray>"),
      Map.entry("&9", "<blue>"),
      Map.entry("&a", "<green>"),
      Map.entry("&b", "<aqua>"),
      Map.entry("&c", "<red>"),
      Map.entry("&d", "<light_purple>"),
      Map.entry("&e", "<yellow>"),
      Map.entry("&f", "<white>"),
      Map.entry("&k", "<obfuscated>"),
      Map.entry("&l", "<bold>"),
      Map.entry("&m", "<strikethrough>"),
      Map.entry("&n", "<underlined>"),
      Map.entry("&o", "<italic>"),
      Map.entry("&r", "<reset>")
  );

  private ComponentUtil() {
  }

  /**
   * Parse a string into a Component with color support.
   * Supports MiniMessage tags, legacy codes (&a), and hex (#FFFFFF, &#FFFFFF).
   *
   * @param text The text to parse
   * @return The parsed Component
   */
  public static Component parse(String text) {
    if (text == null || text.isEmpty()) {
      return Component.empty();
    }

    String parsed = text;

    // Convert section symbols to ampersand
    parsed = parsed.replace("§", "&");

    // Convert hex colors to MiniMessage format
    parsed = convertHexColors(parsed);

    // Convert legacy color codes to MiniMessage
    parsed = convertLegacyCodes(parsed);

    return MINI_MESSAGE.deserialize(parsed);
  }

  /**
   * Parse a string with placeholders replaced.
   *
   * @param text         The text to parse
   * @param placeholders Key-value pairs of placeholders (without %)
   * @return The parsed Component
   */
  public static Component parse(String text, Object... placeholders) {
    if (text == null || text.isEmpty()) {
      return Component.empty();
    }

    String parsed = text;

    // Replace placeholders in pairs
    for (int i = 0; i < placeholders.length - 1; i += 2) {
      String key = "%" + placeholders[i] + "%";
      String value = String.valueOf(placeholders[i + 1]);
      parsed = parsed.replace(key, value);
    }

    return parse(parsed);
  }

  /**
   * Convert hex colors to MiniMessage format
   *
   * @param text The text to convert
   * @return Text with hex colors in MiniMessage format
   */
  private static String convertHexColors(String text) {
    // Convert &#FFFFFF to <#FFFFFF>
    Matcher ampMatcher = AMPERSAND_HEX_PATTERN.matcher(text);
    StringBuilder ampResult = new StringBuilder();
    while (ampMatcher.find()) {
      ampMatcher.appendReplacement(ampResult, "<#$1>");
    }
    ampMatcher.appendTail(ampResult);
    text = ampResult.toString();

    // Convert standalone #FFFFFF to <#FFFFFF> (only if not already in MiniMessage format)
    Matcher hexMatcher = HEX_PATTERN.matcher(text);
    StringBuilder hexResult = new StringBuilder();
    int lastEnd = 0;
    while (hexMatcher.find()) {
      int start = hexMatcher.start();

      // Check if already in MiniMessage format (preceded by <)
      if (start > 0 && text.charAt(start - 1) == '<') {
        continue;
      }

      hexResult.append(text, lastEnd, start);
      hexResult.append("<#").append(hexMatcher.group(1)).append(">");
      lastEnd = hexMatcher.end();
    }
    hexResult.append(text.substring(lastEnd));

    return hexResult.toString();
  }

  /**
   * Convert legacy color codes to MiniMessage format
   *
   * @param text The text to convert
   * @return Text with legacy codes converted to MiniMessage
   */
  private static String convertLegacyCodes(String text) {
    String result = text;

    for (Map.Entry<String, String> entry : LEGACY_COLORS.entrySet()) {
      // Case insensitive replacement
      result = result.replace(entry.getKey(), entry.getValue());
      result = result.replace(entry.getKey().toUpperCase(), entry.getValue());
    }

    return result;
  }

  /**
   * Serialize a Component back to a MiniMessage string
   *
   * @param component The component to serialize
   * @return The serialized string
   */
  public static String serialize(Component component) {
    return MINI_MESSAGE.serialize(component);
  }

  /**
   * Strip all color codes and formatting from text
   *
   * @param text The text to strip
   * @return Plain text without formatting
   */
  public static String stripColors(String text) {
    if (text == null) {
      return "";
    }

    // Remove MiniMessage tags
    String result = text.replaceAll("<[^>]+>", "");

    // Remove legacy codes
    result = result.replaceAll("[&§][0-9a-fk-or]", "");

    // Remove hex codes
    result = result.replaceAll("&#[A-Fa-f0-9]{6}", "");
    result = result.replaceAll("#[A-Fa-f0-9]{6}", "");

    return result;
  }
}
