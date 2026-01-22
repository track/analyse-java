package com.serverstats.hytale.object.action;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.serverstats.hytale.HytalePlugin;
import com.serverstats.hytale.util.ComponentUtil;
import com.serverstats.sdk.object.action.ActionData;
import com.serverstats.sdk.object.action.ActionType;
import net.kyori.adventure.text.Component;

/**
 * Action that sends a message to the player.
 * Supports MiniMessage, hex colors, and legacy color codes.
 */
public class SendMessageAction extends HytaleAction {

  public SendMessageAction(HytalePlugin plugin, ActionData data) {
    super(plugin, data);
  }

  @Override
  public ActionType getType() {
    return ActionType.SEND_MESSAGE;
  }

  @Override
  public void execute(PlayerRef playerRef) {
    String message = data.getString("message");
    if (message == null || message.isBlank()) {
      return;
    }

    playerRef.sendMessage(ComponentUtil.parseHytaleMessage(message,
        "player", playerRef.getUsername(),
        "uuid", playerRef.getUuid().toString()
    ));

    debug("Sent message to %s: %s", playerRef.getUsername(), message);
  }
}
