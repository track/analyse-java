package com.serverstats.hytale.util;

import com.hypixel.hytale.server.core.Message;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Utility for parsing text with colors into Adventure Components.
 * Supports MiniMessage, legacy color codes, and hex colors.
 */
public final class ComponentUtil {

  private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
    .strict(false)
    .build();

  private static final LegacyComponentSerializer LEGACY_SERIALIZER =
    LegacyComponentSerializer.builder().character('&').hexColors().build();

  // Hex color patterns
  private static final Pattern HEX_PATTERN = Pattern.compile(
    "#([A-Fa-f0-9]{6})"
  );
  private static final Pattern AMPERSAND_HEX_PATTERN = Pattern.compile(
    "&#([A-Fa-f0-9]{6})"
  );

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

  private ComponentUtil() {}

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
   * Serialize a Component to a plain text string (no formatting)
   *
   * @param component The component to serialize
   * @return The plain text string
   */
  public static String toPlainText(Component component) {
    return PlainTextComponentSerializer.plainText().serialize(component);
  }

  /**
   * Serialize a Component to a legacy string with color codes
   *
   * @param component The component to serialize
   * @return The legacy string with & color codes
   */
  public static String toLegacy(Component component) {
    return LEGACY_SERIALIZER.serialize(component);
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

  /**
   * Convert an Adventure Component to a Hytale Message
   *
   * @param component The component to convert
   * @return The Hytale Message
   */
  public static Message toHytaleMessage(Component component) {
    if (!(component instanceof TextComponent text)) {
      throw new UnsupportedOperationException(
        "Unsupported component type: " + component.getClass()
      );
    }

    Message message = Message.raw(text.content());
    TextColor color = text.color();
    if (color != null) {
      message.color(color.asHexString());
    }

    TextDecoration.State bold = text.decoration(TextDecoration.BOLD);
    if (bold != TextDecoration.State.NOT_SET) {
      message.bold(bold == TextDecoration.State.TRUE);
    }

    TextDecoration.State italic = text.decoration(TextDecoration.ITALIC);
    if (italic != TextDecoration.State.NOT_SET) {
      message.italic(italic == TextDecoration.State.TRUE);
    }

    ClickEvent clickEvent = text.clickEvent();
    if (
      clickEvent != null && clickEvent.action() == ClickEvent.Action.OPEN_URL
    ) {
      message.link(clickEvent.value());
    }

    message.insertAll(
      text.children().stream().map(ComponentUtil::toHytaleMessage).toList()
    );

    return message;
  }

  /**
   * Parse a Hytale message into a Hytale Message
   *
   * @param message The message to parse
   * @return The Hytale Message
   */
  public static Message parseHytaleMessage(String message, Object... placeholders) {
    Component component = parse(message, placeholders);

    return toHytaleMessage(component);
  }
  
}
