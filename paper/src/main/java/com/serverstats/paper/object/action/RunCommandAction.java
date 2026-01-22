package com.serverstats.paper.object.action;

import com.serverstats.paper.ServerStatsPlugin;
import com.serverstats.sdk.object.action.ActionData;
import com.serverstats.sdk.object.action.ActionType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Action that runs a command.
 * Can be executed as console or as the player.
 */
public class RunCommandAction extends PaperAction {

  public RunCommandAction(ServerStatsPlugin plugin, ActionData data) {
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
    String finalCommand = command;

    // Execute on main thread
    Bukkit.getScheduler().runTask(plugin, () -> {
      if (asConsole) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
      } else {
        player.performCommand(finalCommand);
      }

      debug("Executed command for %s: %s (console=%s)", player.getName(), finalCommand, asConsole);
    });
  }
}
