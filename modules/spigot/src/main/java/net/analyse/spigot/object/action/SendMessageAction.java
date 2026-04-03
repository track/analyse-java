package net.analyse.spigot.object.action;

import net.analyse.spigot.AnalysePlugin;
import net.analyse.spigot.util.MessageUtil;
import net.analyse.sdk.object.action.ActionData;
import net.analyse.sdk.object.action.ActionType;
import org.bukkit.entity.Player;

/**
 * Action that sends a message to the player.
 * Supports MiniMessage, hex colors, and legacy color codes.
 */
public class SendMessageAction extends SpigotAction {

  public SendMessageAction(AnalysePlugin plugin, ActionData data) {
    super(plugin, data);
  }

  @Override
  public ActionType getType() {
    return ActionType.SEND_MESSAGE;
  }

  @Override
  public void execute(Player player) {
    String message = data.getString("message");
    if (message == null || message.trim().isEmpty()) {
      return;
    }

    // Parse with full color support and placeholders
    MessageUtil.sendMessage(player, message,
        "player", player.getName(),
        "uuid", player.getUniqueId().toString()
    );

    debug("Sent message to %s: %s", player.getName(), message);
  }
}
