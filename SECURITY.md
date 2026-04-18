# Security policy

## Supported versions

Only the latest minor release of the Analyse plugins receives security updates. Older versions should be upgraded.

| Version | Supported |
| --- | --- |
| 1.x (latest) | Yes |
| &lt; 1.0 | No |

## Reporting a vulnerability

**Please do not open a public GitHub issue for security vulnerabilities.**

Instead, email us at **security@analyse.net** with:

- A description of the issue and where you found it (file, class, endpoint, etc.).
- The impact (what can an attacker do with this?).
- Steps to reproduce, ideally including a minimal proof of concept.
- Your preferred credit name if the report leads to a fix and you want public credit.

You should receive an acknowledgement within **2 business days**. We aim to have a triaged response with either a fix, a workaround, or a timeline within **14 days**.

## Scope

Reports about the following are in scope:

- The Analyse plugins in this repository (Spigot, BungeeCord, Velocity, Hytale).
- The `analyse-api` SDK.

The Analyse dashboard, API, and website are covered by a separate security policy at [analyse.net/security](https://analyse.net/security).

## Out of scope

- Denial-of-service caused by a server operator running the plugin with obviously invalid configuration.
- Vulnerabilities that require the attacker to already have operator access on the same Minecraft server.
- Social engineering of Analyse staff.
- Self-XSS or similar client-side issues that require the victim to paste attacker-controlled code into their own console.

## Disclosure

We follow coordinated disclosure. Once a fix has shipped, and after a reasonable window for operators to update (typically 7 days for low-severity, 30 days for high-severity), we publish a GitHub Security Advisory crediting the reporter.

Thank you for helping keep Analyse users safe.
