package com.serverstats.bungeecord.util;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import java.util.ArrayList;
import java.util.List;
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

  // URL pattern for detecting links (with or without protocol)
  private static final Pattern URL_PATTERN = Pattern.compile(
      "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+|(?<![\\w@])([\\w\\-]+\\.)+(?:com|net|org|io|gg|me|co|dev|app|xyz|info|biz|us|uk|de|nl|be|fr|eu)[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]*)",
      Pattern.CASE_INSENSITIVE
  );

  private ComponentUtil() {
  }

  /**
   * Parse a string into BaseComponent array with color support.
   * Supports legacy codes (&a), hex colors (&#FFFFFF, #FFFFFF), and clickable URLs.
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

    // Convert URLs to clickable components
    return convertUrlsToComponents(parsed);
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
   * Convert text with URLs into clickable BaseComponent array
   *
   * @param text The text with legacy color codes already applied
   * @return BaseComponent array with clickable URLs
   */
  private static BaseComponent[] convertUrlsToComponents(String text) {
    Matcher matcher = URL_PATTERN.matcher(text);
    List<BaseComponent> components = new ArrayList<>();
    int lastEnd = 0;

    while (matcher.find()) {
      String url = matcher.group(0);
      int start = matcher.start();

      // Add text before the URL
      if (start > lastEnd) {
        String before = text.substring(lastEnd, start);
        for (BaseComponent comp : TextComponent.fromLegacyText(before)) {
          components.add(comp);
        }
      }

      // Add protocol if missing for the click action
      String clickUrl = url.startsWith("http://") || url.startsWith("https://") ? url : "https://" + url;

      // Create clickable URL component
      BaseComponent[] urlComponents = TextComponent.fromLegacyText(url);
      for (BaseComponent comp : urlComponents) {
        comp.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, clickUrl));
        comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to open")));
        components.add(comp);
      }

      lastEnd = matcher.end();
    }

    // Add remaining text after last URL
    if (lastEnd < text.length()) {
      String remaining = text.substring(lastEnd);
      for (BaseComponent comp : TextComponent.fromLegacyText(remaining)) {
        components.add(comp);
      }
    }

    // If no URLs found, return original text
    if (components.isEmpty()) {
      return TextComponent.fromLegacyText(text);
    }

    return components.toArray(new BaseComponent[0]);
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
