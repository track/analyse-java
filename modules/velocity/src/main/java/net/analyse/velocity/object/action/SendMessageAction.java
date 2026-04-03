package net.analyse.velocity.object.action;

import com.velocitypowered.api.proxy.Player;
import net.analyse.sdk.object.action.ActionData;
import net.analyse.sdk.object.action.ActionType;
import net.analyse.velocity.AnalyseVelocity;
import net.analyse.velocity.util.ComponentUtil;

/**
 * Action that sends a message to the player.
 * Supports MiniMessage, legacy color codes, and hex colors.
 */
public class SendMessageAction extends VelocityAction {

  public SendMessageAction(AnalyseVelocity plugin, ActionData data) {
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
