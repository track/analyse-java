package com.serverstats.sdk.util;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Converts Minecraft protocol version numbers to human-readable version strings (e.g. "1.21.4").
 * Protocol-to-version mapping mirrors ProtocolLib's MinecraftProtocolVersion (wiki.vg).
 * Used by Paper, BungeeCord, and Velocity to send playerVersion on join.
 */
public final class ProtocolVersionUtil {

  /**
   * Protocol version → "1.x.y" string. Based on ViaVersion's ProtocolVersion class.
   * When multiple game versions share one protocol number, we store the first version
   * it was introduced in. NavigableMap so we can use floorEntry for unknown/future protocols.
   */
  private static final NavigableMap<Integer, String> PROTOCOL_TO_VERSION = createLookup();

  private static NavigableMap<Integer, String> createLookup() {
    TreeMap<Integer, String> map = new TreeMap<>(Integer::compareTo);

    // Source: ViaVersion ProtocolVersion / wiki.vg Protocol_version_numbers
    // After Netty rewrite (post-1.7)
    map.put(4, "1.7.2");
    map.put(5, "1.7.6");
    map.put(47, "1.8");
    map.put(107, "1.9");
    map.put(108, "1.9.1");
    map.put(109, "1.9.2");
    map.put(110, "1.9.3");
    map.put(210, "1.10");
    map.put(315, "1.11");
    map.put(316, "1.11.1");
    map.put(335, "1.12");
    map.put(338, "1.12.1");
    map.put(340, "1.12.2");
    map.put(393, "1.13");
    map.put(401, "1.13.1");
    map.put(404, "1.13.2");
    map.put(477, "1.14");
    map.put(480, "1.14.1");
    map.put(485, "1.14.2");
    map.put(490, "1.14.3");
    map.put(498, "1.14.4");
    map.put(573, "1.15");
    map.put(575, "1.15.1");
    map.put(578, "1.15.2");
    map.put(735, "1.16");
    map.put(736, "1.16.1");
    map.put(751, "1.16.2");
    map.put(753, "1.16.3");
    map.put(754, "1.16.4");
    map.put(755, "1.17");
    map.put(756, "1.17.1");
    map.put(757, "1.18");
    map.put(758, "1.18.2");
    map.put(759, "1.19");
    map.put(760, "1.19.1");
    map.put(761, "1.19.3");
    map.put(762, "1.19.4");
    map.put(763, "1.20");
    map.put(764, "1.20.2");
    map.put(765, "1.20.3");
    map.put(766, "1.20.5");
    map.put(767, "1.21");
    map.put(768, "1.21.2");
    map.put(769, "1.21.4");
    map.put(770, "1.21.5");
    map.put(771, "1.21.6");
    map.put(772, "1.21.7");
    map.put(773, "1.21.9");
    map.put(774, "1.21.11");

    return map;
  }

  private ProtocolVersionUtil() {
    // Utility class
  }

  /**
   * Convert a protocol version number to a "1.x.y" style version string.
   *
   * @param protocolVersion The client protocol version (e.g. from BungeeCord
   *                        {@code getPendingConnection().getVersion()} or Velocity
   *                        {@code getProtocolVersion().getProtocol()}), or -1 if unknown
   * @return Version string (e.g. "1.21.0"), or "1.?" if unknown/invalid
   */
  public static String toVersionString(int protocolVersion) {
    if (protocolVersion <= 0) {
      return "1.?";
    }

    String known = PROTOCOL_TO_VERSION.get(protocolVersion);
    if (known != null) {
      return known;
    }

    // Fallback: nearest known version <= protocol (for future/new protocol numbers)
    Map.Entry<Integer, String> floor = PROTOCOL_TO_VERSION.floorEntry(protocolVersion);
    if (floor != null) {
      return floor.getValue();
    }

    return "1.?";
  }
}
