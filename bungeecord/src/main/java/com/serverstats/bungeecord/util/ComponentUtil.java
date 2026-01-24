package com.serverstats.bungeecord.util;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for parsing text with colors into BungeeCord BaseComponents.
 * Supports legacy color codes and hex colors.
 */
public final class ComponentUtil {

  // Hex color pattern: &#FFFFFF or #FFFFFF
  private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
  private static final Pattern STANDALONE_HEX_PATTERN = Pattern.compile("(?<!&)#([A-Fa-f0-9]{6})");

  private ComponentUtil() {
  }

  /**
   * Parse a string into BaseComponent array with color support.
   * Supports legacy codes (&a) and hex colors (&#FFFFFF, #FFFFFF).
   *
   * @param text The text to parse
   * @return The parsed BaseComponent array
   */
  public static BaseComponent[] parse(String text) {
    if (text == null || text.isEmpty()) {
      return new BaseComponent[]{new TextComponent("")};
    }

    String parsed = text;

    // Convert section symbols to ampersand
    parsed = parsed.replace("§", "&");

    // Convert hex colors
    parsed = convertHexColors(parsed);

    // Translate legacy color codes
    parsed = ChatColor.translateAlternateColorCodes('&', parsed);

    return TextComponent.fromLegacyText(parsed);
  }

  /**
   * Parse a string with placeholders replaced.
   *
   * @param text         The text to parse
   * @param placeholders Key-value pairs of placeholders (without %)
   * @return The parsed BaseComponent array
   */
  public static BaseComponent[] parse(String text, Object... placeholders) {
    if (text == null || text.isEmpty()) {
      return new BaseComponent[]{new TextComponent("")};
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
   * Convert hex colors to BungeeCord format
   *
   * @param text The text to convert
   * @return Text with hex colors converted
   */
  private static String convertHexColors(String text) {
    // Convert &#FFFFFF format
    Matcher matcher = HEX_PATTERN.matcher(text);
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      String hex = matcher.group(1);
      matcher.appendReplacement(result, convertHexToLegacy(hex));
    }
    matcher.appendTail(result);
    text = result.toString();

    // Convert standalone #FFFFFF format
    matcher = STANDALONE_HEX_PATTERN.matcher(text);
    result = new StringBuilder();
    while (matcher.find()) {
      String hex = matcher.group(1);
      matcher.appendReplacement(result, convertHexToLegacy(hex));
    }
    matcher.appendTail(result);

    return result.toString();
  }

  /**
   * Convert a hex color to BungeeCord's hex format
   *
   * @param hex The hex color (without #)
   * @return The BungeeCord hex format
   */
  private static String convertHexToLegacy(String hex) {
    StringBuilder builder = new StringBuilder("§x");
    for (char c : hex.toCharArray()) {
      builder.append("§").append(c);
    }
    return builder.toString();
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

    // Remove hex codes first
    String result = text.replaceAll("&#[A-Fa-f0-9]{6}", "");
    result = result.replaceAll("#[A-Fa-f0-9]{6}", "");

    // Use BungeeCord's strip method for legacy codes
    return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', result));
  }
}
