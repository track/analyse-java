# Analyse

[![CI](https://github.com/Analyse-net/analyse-java/actions/workflows/ci.yml/badge.svg)](https://github.com/Analyse-net/analyse-java/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/Analyse-net/analyse-java?sort=semver)](https://github.com/Analyse-net/analyse-java/releases/latest)
[![Java 21](https://img.shields.io/badge/java-21-f89820?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![License](https://img.shields.io/badge/license-Proprietary-red)](LICENSE)

The source repository for the [Analyse](https://analyse.net) plugins and developer SDK &mdash; the analytics platform purpose-built for Minecraft and Hytale servers.

Analyse tells you **who** plays on your server, **where** they came from, and **what** keeps them coming back. Install the plugin, paste your API key, and start the server: sessions, retention, revenue, and everything in between show up in your dashboard automatically.

This repository is the home of:

- `analyse-spigot` &mdash; the plugin for Spigot, Paper, Purpur, and Folia
- `analyse-bungeecord` &mdash; the plugin for BungeeCord proxies
- `analyse-velocity` &mdash; the plugin for Velocity proxies
- `analyse-hytale` &mdash; the plugin for Hytale servers
- `analyse-api` &mdash; the public Java SDK for tracking custom events from your own plugins
- `analyse-sdk` &mdash; the internal HTTP client shared by all platform plugins

## Getting started

The shortest path from zero to data:

1. Sign up at [analyse.net](https://analyse.net) and create a Server.
2. Copy your API key from the dashboard (it starts with `anl_`).
3. Download the plugin for your server software from the [releases page](https://analyse.net/downloads).
4. Drop the jar in your `plugins/` folder and start your server once so the config generates.
5. Paste your API key into `plugins/Analyse/config.yml` (or `plugins/analyse/config.json` on Velocity).
6. Restart. Join your server. You should see the session in the dashboard within a few seconds.

Full walkthroughs live in [`docs/`](docs/README.md).

## Documentation

| Guide | What's in it |
| --- | --- |
| [Installation](docs/installation.md) | Install the plugin on Spigot, BungeeCord, Velocity, or Hytale |
| [Configuration](docs/configuration.md) | Every config option for every platform |
| [Commands](docs/commands.md) | `/analyse` subcommands, permissions, and examples |
| [SDK overview](docs/sdk/README.md) | Use the `analyse-api` artifact from your own plugin |
| [SDK reference](docs/sdk/reference.md) | Every public class and method |

## For plugin developers

Track your own events with a one-line static call:

```java
import net.analyse.api.Analyse;

Analyse.trackEvent("quest_completed")
    .withPlayer(player.getUniqueId(), player.getName())
    .withData("quest_id", "dragon_slayer")
    .withValue(500.0)
    .send();
```

Everything you need is documented in [`docs/sdk/`](docs/sdk/README.md).

## Supported platforms

| Platform | Role | Minimum version |
| --- | --- | --- |
| Spigot / Paper / Purpur / Folia | Backend game server | 1.8+ (Java 21 runtime) |
| BungeeCord | Proxy | 1.21+ |
| Velocity | Proxy | 3.4.0+ |
| Hytale | Game server | Current Hytale API |

The plugin only needs Java 21 at runtime; the Spigot module still targets the 1.8 API for broad compatibility.

## Building from source

```bash
./gradlew clean build
```

Output jars land in:

- `modules/spigot/build/libs/analyse-spigot-<version>.jar`
- `modules/bungeecord/build/libs/analyse-bungeecord-<version>.jar`
- `modules/velocity/build/libs/analyse-velocity-<version>.jar`
- `modules/hytale/build/libs/analyse-hytale-<version>.jar`

The `release.sh` script builds all four and bundles them into a single zip.

## Support

- Website: [analyse.net](https://analyse.net)
- Dashboard: [analyse.net/dashboard](https://analyse.net/dashboard)
- Documentation: [analyse.net/docs](https://analyse.net/docs)

## License

Copyright &copy; VertCode Development E.E. All rights reserved.

The source in this repository is published for transparency and reference. It is **not** open source; copying, modifying, redistributing, or running modified builds is not permitted. See [`LICENSE`](LICENSE) for the full terms.
