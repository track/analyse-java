# Analyse Plugin Documentation

Welcome to the Analyse plugin documentation. This guide covers installation, configuration, commands, and API usage for developers.

## Table of Contents

- [Overview](#overview)
- [Installation](installation.md)
- [Configuration](configuration.md)
- [Commands](commands.md)
- [Developer API](api.md)
- [Platforms](#platforms)

## Overview

Analyse is a comprehensive analytics plugin for Minecraft servers. It provides:

- **Player Session Tracking** - Automatically tracks player joins, leaves, and playtime
- **Heartbeat System** - Regular server health checks with online player data
- **Custom Events API** - Fluent static API for other plugins to track custom analytics events
- **A/B Testing** - Built-in support for A/B tests with variant assignment and action execution
- **Multi-Platform Support** - Works on Paper, BungeeCord, and Velocity

## Platforms

| Platform | Server Type | Java Version |
|----------|-------------|--------------|
| **Paper** | Single server / backend | Java 21+ |
| **BungeeCord** | Proxy | Java 21+ |
| **Velocity** | Proxy | Java 21+ |

### Choosing the Right Version

- **Paper**: Use for standalone servers or backend servers behind a proxy
- **BungeeCord/Velocity**: Use for proxy servers to track cross-server analytics

### Feature Comparison

| Feature | Paper | BungeeCord | Velocity |
|---------|-------|------------|----------|
| Session Tracking | ✅ | ✅ | ✅ |
| Heartbeat | ✅ | ✅ | ✅ |
| Custom Events | ✅ | ✅ | ✅ |
| A/B Testing | ✅ | ✅ | ✅ |
| SEND_MESSAGE Action | ✅ | ✅ | ✅ |
| RUN_COMMAND Action | ✅ | ✅ | ✅ |

## Quick Start

1. Download the appropriate plugin jar for your platform
2. Place it in your `plugins` folder
3. Start the server to generate the config
4. Add your API key from [analyse.net](https://analyse.net)
5. Restart the server

```yaml
# Paper config.yml
api-key: "your-api-key-here"
debug: false
instance-id: "auto-generated"
```

## Support

- Website: [analyse.net](https://analyse.net)
- API Documentation: [api.analyse.net](https://api.analyse.net)
