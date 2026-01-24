package com.serverstats.hytale.object.action;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import com.serverstats.hytale.HytalePlugin;
import com.serverstats.sdk.object.action.ActionData;
import com.serverstats.sdk.object.action.ActionType;

/**
 * Action that runs a command.
 * Can be executed as console or as the player.
 */
public class RunCommandAction extends HytaleAction {

  public RunCommandAction(HytalePlugin plugin, ActionData data) {
    super(plugin, data);
  }

  @Override
  public ActionType getType() {
    return ActionType.RUN_COMMAND;
  }

  @Override
  public void execute(PlayerRef playerRef) {
    String command = data.getString("command");
    if (command == null || command.isBlank()) {
      return;
    }

    // Replace placeholders
    command = replacePlaceholders(command, playerRef);

    // Remove leading slash if present
    if (command.startsWith("/")) {
      command = command.substring(1);
    }

    // TODO: Figure out how to execute commands in Hytale

  }
}
