package com.serverstats.bungeecord.object.action;

import com.serverstats.bungeecord.ServerStatsBungee;
import com.serverstats.bungeecord.util.ComponentUtil;
import com.serverstats.sdk.object.action.ActionData;
import com.serverstats.sdk.object.action.ActionType;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Action that sends a message to the player.
 * Supports legacy color codes and hex colors.
 */
public class SendMessageAction extends BungeeAction {

  public SendMessageAction(ServerStatsBungee plugin, ActionData data) {
    super(plugin, data);
  }

  @Override
  public ActionType getType() {
    return ActionType.SEND_MESSAGE;
  }

  @Override
  public void execute(ProxiedPlayer player) {
    String message = data.getString("message");
    if (message == null || message.isBlank()) {
      return;
    }

    // Parse with full color support and placeholders
    player.sendMessage(ComponentUtil.parse(message,
        "player", player.getName(),
        "uuid", player.getUniqueId().toString()
    ));

    debug("Sent message to %s", player.getName());
  }
}
