# Analyse SDK

The Analyse Java SDK (`analyse-api`) lets your own plugins talk to Analyse. It's what you use to track custom events, read A/B test variants, and look up the current session of a player.

You don't need the SDK for Analyse to work. The plugin does all the default tracking on its own. You only need the SDK when you want to teach Analyse about **your** server: your quests, your shops, your gamemodes, your custom features.

## When you need it

Reach for the SDK when you want to:

- Send a custom event from your plugin &mdash; see [events](events.md)
- Show one of two variants in an A/B test &mdash; see [A/B testing](ab-testing.md)
- Read the hostname a player joined through inside your own code &mdash; see [sessions](sessions.md)
- Send events from a backend through your proxy &mdash; see [plugin messaging](plugin-messaging.md)

If none of that applies, you don't need the SDK at all.

## The shape of the API

Everything lives under one static entry point: `net.analyse.api.Analyse`.

```java
import net.analyse.api.Analyse;

Analyse.trackEvent("quest_completed")
    .withPlayer(player.getUniqueId(), player.getName())
    .withData("quest_id", "dragon_slayer")
    .withValue(500.0)
    .send();

String variant = Analyse.getVariant(player.getUniqueId(), "welcome-rewards");

Analyse.trackConversion(
    player.getUniqueId(), player.getName(),
    "welcome-rewards", "first_purchase"
);

Analyse.sessions().getSession(player.getUniqueId());
```

That's about 90% of what most plugins ever use. See the [reference](reference.md) for the full surface area.

## Installation

The SDK ships on the public Analyse Maven repository. Add the repo and the dependency to your build file, and the Analyse plugin will provide the runtime implementation.

### Maven repository

**Gradle (Groovy DSL)**

```groovy
repositories {
    maven { url 'https://maven.analyse.net/maven-releases' }
}
```

**Gradle (Kotlin DSL)**

```kotlin
repositories {
    maven("https://maven.analyse.net/maven-releases")
}
```

**Maven**

```xml
<repositories>
  <repository>
    <id>analyse</id>
    <url>https://maven.analyse.net/maven-releases</url>
  </repository>
</repositories>
```

### Dependency

Replace `LATEST_VERSION` with the latest release shown on [analyse.net/downloads](https://analyse.net/downloads).

**Gradle (Groovy DSL)**

```groovy
dependencies {
    compileOnly 'net.analyse:analyse-api:LATEST_VERSION'
}
```

**Gradle (Kotlin DSL)**

```kotlin
dependencies {
    compileOnly("net.analyse:analyse-api:LATEST_VERSION")
}
```

**Maven**

```xml
<dependency>
  <groupId>net.analyse</groupId>
  <artifactId>analyse-api</artifactId>
  <version>LATEST_VERSION</version>
  <scope>provided</scope>
</dependency>
```

> [!WARNING]
> Don't shade the SDK into your jar. It's `compileOnly` on purpose &mdash; the Analyse plugin exposes the implementation at runtime. Shading leads to `NoSuchMethodError`s and other confusing crashes.

### Declare Analyse as a soft dependency

In your `plugin.yml`, declare Analyse as a soft dependency so your plugin still loads when Analyse isn't installed:

```yaml
name: YourPlugin
version: 1.0.0
main: com.example.YourPlugin
softdepend: [Analyse]
```

With a soft dependency, your code can use the SDK when it's there and gracefully no-op when it isn't. The idiomatic check is `Analyse.isAvailable()`:

```java
if (Analyse.isAvailable()) {
    Analyse.trackEvent("my_event")
        .withPlayer(player.getUniqueId(), player.getName())
        .send();
}
```

### Verify the install

In your plugin's `onEnable`:

```java
if (net.analyse.api.Analyse.isAvailable()) {
    getLogger().info("Analyse SDK is available.");
} else {
    getLogger().warning("Analyse SDK not found. Skipping analytics.");
}
```

If you see the "available" line, you're done.

## What lives where

| Class | Purpose |
| --- | --- |
| `net.analyse.api.Analyse` | Static entry point. Most of what you'll use. |
| `net.analyse.api.object.builder.EventBuilder` | Fluent builder returned by `Analyse.trackEvent`. |
| `net.analyse.api.manager.SessionManager` | Read access to live player sessions. |
| `net.analyse.api.manager.ABTestManager` | Variant lookup and conversion tracking. |
| `net.analyse.api.session.PlayerSession` | A single player's live session. |
| `net.analyse.api.object.abtest.ABTest` | A configured A/B test. |
| `net.analyse.api.messaging.AnalyseMessaging` | Plugin messaging helpers for backend &rarr; proxy events. |

## Next steps

- [Send your first custom event](events.md)
- [Run an A/B test](ab-testing.md)
- [Read a player's session](sessions.md)
- [Plugin messaging from a backend](plugin-messaging.md)
- [API reference](reference.md)
