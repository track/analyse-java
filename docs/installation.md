# Installation

Getting Analyse running on your server takes about five minutes. You download a plugin, paste in an API key, and start the server. That's it.

Before you start, make sure you have access to:

- Your server files (or your host's control panel)
- An [Analyse account](https://analyse.net/register)

## Which plugin do I need

Analyse has one plugin per server software. Pick the one that matches your setup.

| Your server runs on | Plugin to use |
| --- | --- |
| Paper, Spigot, Purpur, Folia | `analyse-spigot` |
| Hytale | `analyse-hytale` |
| Velocity (proxy only) | `analyse-velocity` |
| BungeeCord (proxy only) | `analyse-bungeecord` |

> [!TIP]
> 99% of Minecraft server owners want the Spigot plugin on each of their backend gamemodes. Only reach for the proxy plugins when the backends are on server software Analyse doesn't support.

## Requirements

- **Java 21** or newer at runtime (the Spigot plugin still targets the 1.8 API for broad compatibility)
- **Outbound HTTPS access** to `api.analyse.net`

## Step 1: Create a Server in Analyse

1. Log in at [analyse.net/dashboard](https://analyse.net/dashboard).
2. Click **New Server** and give it a short, clean name like `LifeSteal` or `Skyblock`.
3. Pick the platform (Minecraft or Hytale).
4. Open the new Server, go to **Settings &rarr; API**, create a key if there isn't one already, and copy it. API keys start with `anl_`.

Keep the tab open &mdash; you'll paste the key into your config in a minute.

## Step 2: Install the plugin

### Spigot / Paper / Purpur / Folia

1. Download `analyse-spigot-<version>.jar` from [analyse.net/downloads](https://analyse.net/downloads).
2. Drop it into your server's `plugins/` folder.
3. Start the server once so the config file generates, then stop it.
4. Open `plugins/Analyse/config.yml`.
5. Replace the placeholder `api-key` with the key you copied from the dashboard.
6. Start the server again. Look for the `Analyse enabled` line in the console.

### BungeeCord

1. Download `analyse-bungeecord-<version>.jar` from [analyse.net/downloads](https://analyse.net/downloads).
2. Drop it into your proxy's `plugins/` folder.
3. Start the proxy once, then stop it.
4. Open `plugins/Analyse/config.yml`.
5. Add an API key entry under `servers:` for each backend you want to track (see the [configuration guide](configuration.md#bungeecord)).
6. Start the proxy.

### Velocity

1. Download `analyse-velocity-<version>.jar` from [analyse.net/downloads](https://analyse.net/downloads).
2. Drop it into your proxy's `plugins/` folder.
3. Start the proxy once, then stop it.
4. Open `plugins/analyse/config.json`.
5. Add an API key entry under `servers` for each backend you want to track (see the [configuration guide](configuration.md#velocity)).
6. Start the proxy.

### Hytale

1. Download `analyse-hytale-<version>.jar` from [analyse.net/downloads](https://analyse.net/downloads).
2. Install it the same way you install any Hytale plugin on your server.
3. Open the generated `config.json` in the Analyse data folder.
4. Paste in your API key.
5. Restart the server.

## Step 3: Verify it's working

Join your server. Within a few seconds, the new session should appear in your Analyse dashboard under **Players** and **Sessions**.

You can also check in-game or from the console:

```
/analyse status
```

A healthy server looks like this:

```
──────────────────────────────
  Analyse v1.0.0
──────────────────────────────
  Status: Connected
  API: api.analyse.net
  Players Tracked: 10
  Debug: Disabled
──────────────────────────────
```

If the status says **Disconnected**:

- Double-check the API key in your config for trailing spaces or typos.
- Make sure your server can reach `https://api.analyse.net` (some hosts block outgoing traffic by default).
- Enable debug mode (`/analyse debug`) and re-check the console for a clearer error.

> [!WARNING]
> Never share your API key. It's the equivalent of a password for your Server inside Analyse. Don't paste it into a support ticket, Discord message, or public repo. If you leak it, revoke it from the dashboard and generate a new one.

## Next steps

- Learn how to configure the plugin: [Configuration](configuration.md)
- Learn the in-game commands: [Commands](commands.md)
- Send your own events from another plugin: [SDK overview](sdk/README.md)
