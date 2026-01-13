# Installation

This guide covers installing the Analyse plugin on different platforms.

## Requirements

- **Java**: 21 or higher
- **Server Software**: Paper 1.21+, BungeeCord, or Velocity 3.x

## Paper Installation

1. Download `analyse-paper-x.x.x.jar` from the releases
2. Place the jar file in your server's `plugins` folder
3. Start the server to generate the configuration file
4. Edit `plugins/Analyse/config.yml` with your API key
5. Restart the server or run `/analyse reload`

### Paper Config Location
```
plugins/
└── Analyse/
    └── config.yml
```

## BungeeCord Installation

1. Download `analyse-bungeecord-x.x.x.jar` from the releases
2. Place the jar file in your proxy's `plugins` folder
3. Start the proxy to generate the configuration file
4. Edit `plugins/Analyse/config.yml` with your server API keys
5. Restart the proxy

### BungeeCord Config Location
```
plugins/
└── Analyse/
    └── config.yml
```

## Velocity Installation

1. Download `analyse-velocity-x.x.x.jar` from the releases
2. Place the jar file in your proxy's `plugins` folder
3. Start the proxy to generate the configuration file
4. Edit `plugins/analyse/config.yml` with your server API keys
5. Restart the proxy

### Velocity Config Location
```
plugins/
└── analyse/
    └── config.yml
```

## Getting Your API Key

1. Log in to your [Analyse Dashboard](https://analyse.net)
2. Navigate to your server settings
3. Copy your API key
4. Paste it in your configuration file

## Verifying Installation

After configuration, run `/analyse status` in-game or console to verify:

```
──────────────────────────────
  Analyse v0.1.0
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
