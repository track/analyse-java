# Changelog

All notable changes to the Analyse plugins and SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [1.2.1] &mdash; 2026-05-02

### Changed

- Event tracking requests now include the configured instance ID across Spigot/Paper, Velocity, BungeeCord, and Hytale.
- Updated SDK README repository links.

### Fixed

- Ignored IDE-generated `bin/` output so compiled class files are not picked up as commit candidates.

## [1.2.0] &mdash; 2026-04-28

### Removed

- Removed all built-in A/B test variant actions from all platform plugins and the SDK action enum.

## [1.1.0] &mdash; 2026-04-19

### Added

- New SDK endpoint for recording purchases: `AnalyseClient#trackPurchase(PurchaseRequest, AnalyseCallback<PurchaseResponse>)` backed by `POST /v1/plugin/purchase`.
- New `PurchaseRequest` and `PurchaseResponse` models in `net.analyse.sdk.request` / `net.analyse.sdk.response`.
- New `/analyse purchase <player_uuid> <purchase_value> <product_name>` subcommand on Spigot/Paper, Velocity, BungeeCord, and Hytale. Remaining args after the value are joined with spaces so multi-word product names work.
- New `analyse.purchase` permission (declared in `plugin.yml` and `paper-plugin.yml`, included in `analyse.*`).
- Documented the new command and permission in [`docs/commands.md`](docs/commands.md).

## [1.0.0] &mdash; 2026-04-18

First public release of the source repository.

### Added

- Source repository published on GitHub under the [Analyse Proprietary License](LICENSE).
- Full documentation for the plugin and SDK under [`docs/`](docs/README.md).
- `CONTRIBUTING.md`, `SECURITY.md`, and GitHub issue / PR templates.
- CI workflow that builds every module on push and pull request.
- Release workflow that publishes jars as a GitHub Release on version tags.
- Dependabot updates for Gradle and GitHub Actions.

### Changed

- **BREAKING:** Command permissions renamed from `analyse.netmand.<sub>` to `analyse.<sub>` (for example, `analyse.netmand.reload` is now `analyse.reload`). The old names were a leftover from the `ServerStats` to `Analyse` rename and have been dropped entirely. Update your permission configs before upgrading.
- Staging API URL moved from `staging.analyse.net` to `api-staging.analyse.net`.
- Command alias cleaned up from `analyse|analyse|ss` to `analyse|ss`.
- Spigot `plugin.yml` and `paper-plugin.yml` now declare every command permission with a proper wildcard parent (`analyse.*` grants everything).

### Fixed

- Removed unreachable `RUN_COMMAND` action stub on Hytale &mdash; the action now logs a clear warning instead of silently doing nothing.

### Removed

- `install-nexus.sh` installer script. Nexus setup is now documented internally.

## Versioning

- **MAJOR** version bumps are reserved for breaking API changes on the `analyse-api` artifact.
- **MINOR** bumps add features in a backwards-compatible way.
- **PATCH** bumps are bug fixes and internal changes that don't affect the public API.
