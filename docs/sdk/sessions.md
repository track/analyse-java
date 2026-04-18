# Sessions

A session is the live state of a player while they're connected. The SDK gives you read-only access to session data, so your plugin can do things like "only do X if the player came from Campaign Y".

## Getting a session

```java
import net.analyse.api.Analyse;
import net.analyse.api.session.PlayerSession;

Analyse.sessions()
    .getSession(player.getUniqueId())
    .ifPresent(session -> {
        String hostname = session.getHostname();
        Instant joinedAt = session.getJoinTime();
        // ...
    });
```

Every online player has exactly one session. When they leave, the session is removed.

## What's on a session

| Method | Returns |
| --- | --- |
| `getPlayerUuid()` | The player's Minecraft UUID. |
| `getSessionId()` | The session ID from the Analyse API, or `null` if it hasn't synced yet. |
| `getHostname()` | The hostname they connected through. Use this to identify the Campaign. |
| `getIp()` | The IP address the player connected from. |
| `getJoinTime()` | An `Instant` for when they joined. |
| `hasActiveSession()` | `true` once the server has confirmed the session with Analyse. |

## Useful patterns

### Reward players based on their Campaign

Campaigns in Analyse are matched by the hostname a player joined through (for example, `pewds.yourserver.net`). The live session exposes that hostname, so you can reward players for showing up through a specific Campaign:

```java
Analyse.sessions()
    .getSession(player.getUniqueId())
    .ifPresent(session -> {
        String host = session.getHostname();
        if (host != null && host.startsWith("pewds.")) {
            rewardPewdsBonus(player);
        }
    });
```

### Track playtime locally

```java
long seconds = Duration.between(
    session.getJoinTime(),
    Instant.now()
).getSeconds();
```

### Only fire custom events once the session is ready

```java
if (session.hasActiveSession()) {
    Analyse.trackEvent("custom_thing")
        .withPlayer(session.getPlayerUuid(), player.getName())
        .send();
}
```

## All sessions

```java
Collection<? extends PlayerSession> online = Analyse.sessions().getAllSessions();
getLogger().info("Currently " + online.size() + " tracked sessions.");
```

Handy for in-game admin commands and health checks.

## What's not here

- You can't modify a session. It's read-only from the SDK.
- You can't see past sessions for a player. Use the Analyse dashboard or the HTTP API for history.

> [!CAUTION]
> `PlayerSession.getIp()` exposes the player's IP address. Never send this to third-party services and never log it without a good reason &mdash; it's personal data in most jurisdictions.

## Related

- [Custom events](events.md)
- [Reference](reference.md)
