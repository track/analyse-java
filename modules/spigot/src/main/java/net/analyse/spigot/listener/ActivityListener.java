package net.analyse.spigot.listener;

import net.analyse.api.Analyse;
import net.analyse.api.object.builder.EventBuilder;
import net.analyse.spigot.AnalysePlugin;
import net.analyse.spigot.config.AnalyseSpigotConfig;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import java.util.UUID;

/**
 * Listens for common player actions and sends built-in analytics events
 */
public class ActivityListener implements Listener {

  private final AnalysePlugin plugin;

  public ActivityListener(AnalysePlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onChat(AsyncPlayerChatEvent event) {
    if (!isEnabled("chat")) {
      return;
    }

    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();
    String username = player.getName();
    String message = event.getMessage();

    Analyse.trackEvent("plugin.chat")
        .withPlayer(uuid, username)
        .withData("message", message)
        .send();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onCommand(PlayerCommandPreprocessEvent event) {
    if (!isEnabled("command")) {
      return;
    }

    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();
    String username = player.getName();

    // Strip leading slash and extract base command
    String rawCommand = event.getMessage().substring(1);
    String command = rawCommand.split(" ")[0];

    Analyse.trackEvent("plugin.command")
        .withPlayer(uuid, username)
        .withData("command", command)
        .withData("rawCommand", rawCommand)
        .send();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    if (!isEnabled("block-place")) {
      return;
    }

    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();
    String username = player.getName();
    String blockType = event.getBlock().getType().name();

    Analyse.trackEvent("plugin.block_place")
        .withPlayer(uuid, username)
        .withData("block_type", blockType)
        .send();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    if (!isEnabled("block-break")) {
      return;
    }

    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();
    String username = player.getName();
    String blockType = event.getBlock().getType().name();

    Analyse.trackEvent("plugin.block_break")
        .withPlayer(uuid, username)
        .withData("block_type", blockType)
        .send();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onDeath(PlayerDeathEvent event) {
    if (!isEnabled("death")) {
      return;
    }

    Player player = event.getEntity();
    UUID uuid = player.getUniqueId();
    String username = player.getName();

    EntityDamageEvent lastDamage = player.getLastDamageCause();
    String cause = lastDamage != null ? lastDamage.getCause().name() : "UNKNOWN";

    EventBuilder builder = Analyse.trackEvent("plugin.death")
        .withPlayer(uuid, username)
        .withData("cause", cause);

    // Include killer info if killed by an entity
    if (lastDamage instanceof EntityDamageByEntityEvent) {
      EntityDamageByEntityEvent entityDamage = (EntityDamageByEntityEvent) lastDamage;
      Entity killer = entityDamage.getDamager();
      builder.withData("killer", killer instanceof Player ? killer.getName() : killer.getType().name());
    }

    builder.send();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onEntityDeath(EntityDeathEvent event) {
    if (!isEnabled("kill-entity")) {
      return;
    }

    Player killer = event.getEntity().getKiller();
    if (killer == null) {
      return;
    }

    UUID uuid = killer.getUniqueId();
    String username = killer.getName();
    Entity victim = event.getEntity();
    String entityType = victim.getType().name();

    EventBuilder builder = Analyse.trackEvent("plugin.kill_entity")
        .withPlayer(uuid, username)
        .withData("entity_type", entityType);

    // Include name if the victim is a player
    if (victim instanceof Player) {
      Player victimPlayer = (Player) victim;
      builder.withData("entity_name", victimPlayer.getName());
    }

    builder.send();
  }

  /**
   * Check if a built-in event type is enabled and the plugin is initialized
   *
   * @param key The event config key
   * @return true if the event should be tracked
   */
  private boolean isEnabled(String key) {
    if (!plugin.isAnalyseReady()) {
      return false;
    }

    AnalyseSpigotConfig config = plugin.getPluginConfig();
    return config.isEventEnabled(key);
  }
}
