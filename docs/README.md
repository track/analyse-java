# Documentation

Welcome to the Analyse plugin documentation. These pages cover everything you need to install, configure, and extend the Analyse plugin on your server.

If you're looking for the product-side docs (dashboards, features, billing, campaigns, and so on), head to [analyse.net/docs](https://analyse.net/docs). This repository only documents the plugin and the SDK.

## Plugin

| Page | What's in it |
| --- | --- |
| [Installation](installation.md) | Installing the plugin on Spigot, BungeeCord, Velocity, and Hytale |
| [Configuration](configuration.md) | Every config option for every platform, with examples |
| [Commands](commands.md) | `/analyse` subcommands, permissions, and usage |

## SDK

The Analyse SDK (`analyse-api`) lets your own plugins talk to Analyse. Use it to send custom events, read A/B test variants, and look up the current session of a player.

| Page | What's in it |
| --- | --- |
| [Overview &amp; installation](sdk/README.md) | What the SDK is and how to add it to your project |
| [Custom events](sdk/events.md) | Send your own events with the fluent builder |
| [A/B testing](sdk/ab-testing.md) | Read variants and track conversions from code |
| [Sessions](sdk/sessions.md) | Read the live session of an online player |
| [Plugin messaging](sdk/plugin-messaging.md) | Send events from a backend server through a proxy |
| [Reference](sdk/reference.md) | A table of every public class and method |

## Quick links

- [analyse.net](https://analyse.net)
- [Dashboard](https://analyse.net/dashboard)
- [Downloads](https://analyse.net/downloads)
- [Product docs](https://analyse.net/docs)
