package com.serverstats.velocity.object.action;

import com.velocitypowered.api.proxy.Player;
import com.serverstats.sdk.object.action.ActionData;
import com.serverstats.sdk.object.action.ActionType;
import com.serverstats.velocity.ServerStatsVelocity;
import com.serverstats.velocity.util.ComponentUtil;

/**
 * Action that sends a message to the player.
 * Supports MiniMessage, legacy color codes, and hex colors.
 */
public class SendMessageAction extends VelocityAction {

  public SendMessageAction(ServerStatsVelocity plugin, ActionData data) {
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
        "player", player.getUsername(),
        "uuid", player.getUniqueId().toString()
    ));

    debug("Sent message to %s", player.getUsername());
  }
}
