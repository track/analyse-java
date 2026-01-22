package com.serverstats.paper.object.action;

import com.serverstats.paper.ServerStatsPlugin;
import com.serverstats.paper.util.ComponentUtil;
import com.serverstats.sdk.object.action.ActionData;
import com.serverstats.sdk.object.action.ActionType;
import org.bukkit.entity.Player;

/**
 * Action that sends a message to the player.
 * Supports MiniMessage, hex colors, and legacy color codes.
 */
public class SendMessageAction extends PaperAction {

  public SendMessageAction(ServerStatsPlugin plugin, ActionData data) {
    super(plugin, data);
  }

  @Override
  public ActionType getType() {
    return ActionType.SEND_MESSAGE;
  }

  @Override
  public void execute(Player player) {
    String message = data.getString("message");
    if (message == null || message.isBlank()) {
      return;
    }

    // Parse with full color support and placeholders
    player.sendMessage(ComponentUtil.parse(message,
        "player", player.getName(),
        "uuid", player.getUniqueId().toString()
    ));

    debug("Sent message to %s: %s", player.getName(), message);
  }
}
