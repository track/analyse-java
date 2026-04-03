package net.analyse.spigot.util;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/**
 * Messaging utility that uses adventure-platform-bukkit for cross-version support.
 * On Paper: uses native Adventure rendering (full RGB, gradients, hover events).
 * On Spigot: automatically converts to closest legacy ChatColor equivalent.
 */
public final class MessageUtil {

    private static BukkitAudiences audiences;

    private MessageUtil() {
    }

    /**
     * Initialize the messaging system. Must be called in onEnable().
     *
     * @param plugin The plugin instance
     */
    public static void init(Plugin plugin) {
        audiences = BukkitAudiences.create(plugin);
    }

    /**
     * Send a parsed Component to a command sender
     *
     * @param sender The command sender
     * @param component The component to send
     */
    public static void sendMessage(CommandSender sender, Component component) {
        if (audiences == null) {
            return;
        }

        audiences.sender(sender).sendMessage(component);
    }

    /**
     * Send a message with color code support to a command sender.
     * Supports MiniMessage tags, legacy &amp; codes, and hex colors.
     *
     * @param sender The command sender
     * @param text The text to parse and send
     */
    public static void sendMessage(CommandSender sender, String text) {
        sendMessage(sender, ComponentUtil.parse(text));
    }

    /**
     * Send a message with color code and placeholder support to a command sender
     *
     * @param sender The command sender
     * @param text The text to parse and send
     * @param placeholders Key-value pairs of placeholders (without %)
     */
    public static void sendMessage(CommandSender sender, String text, Object... placeholders) {
        sendMessage(sender, ComponentUtil.parse(text, placeholders));
    }

    /**
     * Shut down the messaging system. Must be called in onDisable().
     */
    public static void close() {
        if (audiences != null) {
            audiences.close();
            audiences = null;
        }
    }
}
