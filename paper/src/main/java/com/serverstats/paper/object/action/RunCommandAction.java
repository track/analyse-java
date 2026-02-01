package com.serverstats.paper.object.action;

import com.serverstats.paper.ServerStatsPlugin;
import com.serverstats.paper.util.SchedulerUtil;
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

    // Execute on appropriate thread (entity scheduler for player commands in Folia)
    if (asConsole) {
      // Console commands can run on global region
      SchedulerUtil.runSync(plugin, () -> {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
        debug("Executed console command for %s: %s", player.getName(), finalCommand);
      });
    } else {
      // Player commands must run on the player's region thread
      SchedulerUtil.runForEntity(plugin, player, () -> {
        player.performCommand(finalCommand);
        debug("Executed player command for %s: %s", player.getName(), finalCommand);
      }, () -> {
        // Player left before command could be executed
        debug("Player %s left before command could be executed: %s", player.getName(), finalCommand);
      });
    }
  }
}
