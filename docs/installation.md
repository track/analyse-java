# Installation

This guide covers installing the Analyse plugin on different platforms.

## Requirements

- **Java**: 21 or higher
- **Server software**: Spigot 1.8+, Paper, BungeeCord, or Velocity 3.x

## Spigot / Paper installation

1. Download `analyse-spigot-x.x.x.jar` from the releases
2. Place the jar file in your server's `plugins` folder
3. Start the server to generate the configuration file
4. Edit `plugins/Analyse/config.yml` with your API key
5. Restart the server or run `/analyse reload`

### Spigot / Paper config location

```
plugins/
└── Analyse/
    └── config.yml
```

## BungeeCord installation

1. Download `analyse-bungeecord-x.x.x.jar` from the releases
2. Place the jar file in your proxy's `plugins` folder
3. Start the proxy to generate the configuration file
4. Edit `plugins/Analyse/config.yml` with your server API keys
5. Restart the proxy

### BungeeCord config location

```
plugins/
└── Analyse/
    └── config.yml
```

## Velocity installation

1. Download `analyse-velocity-x.x.x.jar` from the releases
2. Place the jar file in your proxy's `plugins` folder
3. Start the proxy to generate the configuration file
4. Edit `plugins/analyse/config.yml` with your server API keys
5. Restart the proxy

### Velocity config location

```
plugins/
└── analyse/
    └── config.yml
```

## Hytale installation

1. Download `analyse-hytale-x.x.x.jar` from the releases
2. Install per Hytale server mod/plugin instructions for your environment
3. Configure using the generated config and your API key from [analyse.net](https://analyse.net)

## Getting your API key

1. Log in to your [Analyse dashboard](https://analyse.net)
2. Navigate to your server settings
3. Copy your API key
4. Paste it in your configuration file

## Verifying installation

After configuration, run `/analyse status` in-game or console to verify:

```
──────────────────────────────
  Analyse v1.0.0
──────────────────────────────
  Status: ● Connected
  API: api.analyse.net
  Players Tracked: 10
  Debug: Disabled
──────────────────────────────
```

If the status shows "Disconnected", check:

- Your API key is correct
- The server has internet access
- Firewall allows outbound HTTPS connections
