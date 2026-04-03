package net.analyse.spigot.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import java.util.HashMap;
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

  // URL pattern for detecting links (with or without protocol)
  private static final Pattern URL_PATTERN = Pattern.compile(
      "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+|(?<![\\w@])([\\w\\-]+\\.)+(?:com|net|org|io|gg|me|co|dev|app|xyz|info|biz|us|uk|de|nl|be|fr|eu)[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]*)",
      Pattern.CASE_INSENSITIVE
  );

  // Legacy color code mappings
  private static final Map<String, String> LEGACY_COLORS = new HashMap<String, String>();

  static {
    LEGACY_COLORS.put("&0", "<black>");
    LEGACY_COLORS.put("&1", "<dark_blue>");
    LEGACY_COLORS.put("&2", "<dark_green>");
    LEGACY_COLORS.put("&3", "<dark_aqua>");
    LEGACY_COLORS.put("&4", "<dark_red>");
    LEGACY_COLORS.put("&5", "<dark_purple>");
    LEGACY_COLORS.put("&6", "<gold>");
    LEGACY_COLORS.put("&7", "<gray>");
    LEGACY_COLORS.put("&8", "<dark_gray>");
    LEGACY_COLORS.put("&9", "<blue>");
    LEGACY_COLORS.put("&a", "<green>");
    LEGACY_COLORS.put("&b", "<aqua>");
    LEGACY_COLORS.put("&c", "<red>");
    LEGACY_COLORS.put("&d", "<light_purple>");
    LEGACY_COLORS.put("&e", "<yellow>");
    LEGACY_COLORS.put("&f", "<white>");
    LEGACY_COLORS.put("&k", "<obfuscated>");
    LEGACY_COLORS.put("&l", "<bold>");
    LEGACY_COLORS.put("&m", "<strikethrough>");
    LEGACY_COLORS.put("&n", "<underlined>");
    LEGACY_COLORS.put("&o", "<italic>");
    LEGACY_COLORS.put("&r", "<reset>");
  }

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

    // Convert URLs to clickable links
    parsed = convertUrls(parsed);

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
    StringBuffer ampResult = new StringBuffer();
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
   * Convert URLs to clickable MiniMessage links
   *
   * @param text The text to convert
   * @return Text with URLs converted to clickable links
   */
  private static String convertUrls(String text) {
    Matcher matcher = URL_PATTERN.matcher(text);
    StringBuilder result = new StringBuilder();
    int lastEnd = 0;

    while (matcher.find()) {
      String url = matcher.group(0);
      int start = matcher.start();

      // Check if URL is already inside a MiniMessage click tag
      String before = text.substring(Math.max(0, start - 20), start);
      if (before.contains("<click:") && !before.contains(">")) {
        continue;
      }

      // Add protocol if missing for the click action
      String clickUrl = url.startsWith("http://") || url.startsWith("https://") ? url : "https://" + url;

      result.append(text, lastEnd, start);
      result.append("<click:open_url:'").append(clickUrl).append("'><hover:show_text:'<gray>Click to open'>");
      result.append(url);
      result.append("</hover></click>");
      lastEnd = matcher.end();
    }
    result.append(text.substring(lastEnd));

    return result.toString();
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
