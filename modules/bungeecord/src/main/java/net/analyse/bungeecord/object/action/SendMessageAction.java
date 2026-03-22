package net.analyse.bungeecord.object.action;

import net.analyse.bungeecord.AnalyseBungee;
import net.analyse.bungeecord.util.ComponentUtil;
import net.analyse.sdk.object.action.ActionData;
import net.analyse.sdk.object.action.ActionType;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Action that sends a message to the player.
 * Supports legacy color codes and hex colors.
 */
public class SendMessageAction extends BungeeAction {

  public SendMessageAction(AnalyseBungee plugin, ActionData data) {
    super(plugin, data);
  }

  @Override
  public ActionType getType() {
    return ActionType.SEND_MESSAGE;
  }

  @Override
  public void execute(ProxiedPlayer player) {
    String message = data.getString("message");
    if (message == null || message.trim().isEmpty()) {
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
