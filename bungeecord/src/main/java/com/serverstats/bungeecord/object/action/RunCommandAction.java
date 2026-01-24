package com.serverstats.bungeecord.object.action;

import com.serverstats.bungeecord.ServerStatsBungee;
import com.serverstats.sdk.object.action.ActionData;
import com.serverstats.sdk.object.action.ActionType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Action that runs a command on the proxy.
 * Can be executed as console or as the player.
 */
public class RunCommandAction extends BungeeAction {

  public RunCommandAction(ServerStatsBungee plugin, ActionData data) {
    super(plugin, data);
  }

  @Override
  public ActionType getType() {
    return ActionType.RUN_COMMAND;
  }

  @Override
  public void execute(ProxiedPlayer player) {
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
      ProxyServer.getInstance().getPluginManager().dispatchCommand(
          ProxyServer.getInstance().getConsole(),
          command
      );
    } else {
      ProxyServer.getInstance().getPluginManager().dispatchCommand(player, command);
    }

    debug("Executed command for %s: %s (console=%s)", player.getName(), command, asConsole);
  }
}
