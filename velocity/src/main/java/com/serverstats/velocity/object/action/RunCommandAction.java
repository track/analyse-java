package com.serverstats.velocity.object.action;

import com.velocitypowered.api.proxy.Player;
import com.serverstats.sdk.object.action.ActionData;
import com.serverstats.sdk.object.action.ActionType;
import com.serverstats.velocity.ServerStatsVelocity;

/**
 * Action that runs a command on the proxy.
 * Can be executed as console or as the player.
 */
public class RunCommandAction extends VelocityAction {

  public RunCommandAction(ServerStatsVelocity plugin, ActionData data) {
    super(plugin, data);
  }

  @Override
  public ActionType getType() {
    return ActionType.RUN_COMMAND;
  }

  @Override
  public void execute(Player player) {
    String command = data.getString("command");
    if (command == null || command.isBlank()) {
      return;
    }

    // Replace placeholders
    command = replacePlaceholders(command, player);

    // Remove leading slash if present
    if (command.startsWith("/")) {
      command = command.substring(1);
    }

    boolean asConsole = data.getBoolean("console", true);

    if (asConsole) {
      plugin.getServer().getCommandManager().executeAsync(
          plugin.getServer().getConsoleCommandSource(),
          command
      );
    } else {
      plugin.getServer().getCommandManager().executeAsync(player, command);
    }

    debug("Executed command for %s: %s (console=%s)", player.getUsername(), command, asConsole);
  }
}
