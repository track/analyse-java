package net.analyse.hytale.object.action;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import net.analyse.hytale.HytalePlugin;
import net.analyse.sdk.object.action.ActionData;
import net.analyse.sdk.object.action.ActionType;

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

    if (command.startsWith("/")) {
      command = command.substring(1);
    }

    plugin.getLogger()
        .atWarning()
        .log("RUN_COMMAND action is not yet supported on Hytale; skipping command: %s", command);
  }
}
