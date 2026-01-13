# Developer API

The Analyse plugin provides a simple, fluent static API for other plugins to track custom analytics events and interact with A/B tests.

## Maven/Gradle Setup

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven("https://repo.analyse.net/releases")
}

dependencies {
    compileOnly("net.analyse:api:0.1.0")
}
```

### Gradle (Groovy DSL)

```groovy
repositories {
    maven { url 'https://repo.analyse.net/releases' }
}

dependencies {
    compileOnly 'net.analyse:api:0.1.0'
}
```

### Maven

```xml
<repository>
    <id>analyse</id>
    <url>https://repo.analyse.net/releases</url>
</repository>

<dependency>
    <groupId>net.analyse</groupId>
    <artifactId>api</artifactId>
    <version>0.1.0</version>
    <scope>provided</scope>
</dependency>
```

## Quick Start

```java
import net.analyse.api.Analyse;

// Simple event
Analyse.trackEvent("my_event").send();

// Event with player
Analyse.trackEvent("player_action")
    .withPlayer(player.getUniqueId(), player.getName())
    .send();

// Event with data
Analyse.trackEvent("shop_purchase")
    .withPlayer(player.getUniqueId(), player.getName())
    .withData("item", "diamond_sword")
    .withData("price", 500)
    .withValue(500.0)
    .send();
```

## Custom Events API

### Analyse Class

The main entry point for tracking events.

#### trackEvent(String name)

Creates a new event builder.

```java
EventBuilder builder = Analyse.trackEvent("event_name");
```

**Parameters:**
- `name` - The event name (lowercase with underscores recommended)

**Returns:** An `EventBuilder` for configuring the event

**Throws:** `IllegalArgumentException` if name is null or blank

#### isAvailable()

Checks if Analyse is ready to track events.

```java
if (Analyse.isAvailable()) {
    Analyse.trackEvent("my_event").send();
}
```

**Returns:** `true` if Analyse is initialized and connected

### EventBuilder Class

Fluent builder for configuring events.

#### withPlayer(UUID uuid, String username)

Associates the event with a player.

```java
Analyse.trackEvent("login")
    .withPlayer(player.getUniqueId(), player.getName())
    .send();
```

#### withPlayer(UUID uuid)

Associates the event with a player using only UUID.

```java
Analyse.trackEvent("achievement")
    .withPlayer(player.getUniqueId())
    .send();
```

#### withData(String key, Object value)

Adds a custom data field to the event.

```java
Analyse.trackEvent("purchase")
    .withData("item", "diamond_sword")
    .withData("quantity", 1)
    .withData("total_price", 500)
    .send();
```

#### withData(Map<String, Object> data)

Adds multiple data fields at once.

```java
Map<String, Object> eventData = new HashMap<>();
eventData.put("quest_id", "dragon_slayer");
eventData.put("difficulty", "hard");
eventData.put("time_seconds", 3600);

Analyse.trackEvent("quest_completed")
    .withData(eventData)
    .send();
```

#### withValue(double value)

Sets a numeric value for aggregations (sum, average, etc.).

```java
Analyse.trackEvent("coins_earned")
    .withPlayer(player.getUniqueId(), player.getName())
    .withValue(1000.0)
    .send();
```

#### send()

Sends the event asynchronously (fire and forget).

```java
Analyse.trackEvent("server_start").send();
```

#### send(Consumer<EventResponse> callback)

Sends the event with a callback for the response.

```java
Analyse.trackEvent("important_event")
    .send(response -> {
        if (response != null && response.isSuccess()) {
            System.out.println("Event tracked: " + response.getEventId());
        } else {
            System.out.println("Failed to track event");
        }
    });
```

## A/B Testing API

The Analyse API provides built-in support for A/B testing.

### Get Active Tests

```java
List<ABTest> tests = Analyse.getActiveTests();
for (ABTest test : tests) {
    System.out.println("Test: " + test.getKey() + " - " + test.getName());
}
```

### Check if Test is Active

```java
if (Analyse.isTestActive("welcome_message_test")) {
    // Test is running
}
```

### Get Player's Variant

```java
String variant = Analyse.getVariant(player.getUniqueId(), "welcome_message_test");
if ("variant_a".equals(variant)) {
    // Show variant A experience
} else if ("variant_b".equals(variant)) {
    // Show variant B experience
}
```

### Track Conversion

```java
Analyse.trackConversion(
    player.getUniqueId(),
    player.getName(),
    "welcome_message_test",
    "clicked_shop"
);
```

### A/B Test Triggers

A/B tests can be triggered by:

| Trigger | Description |
|---------|-------------|
| `FIRST_JOIN` | Executes when a player joins for the first time |
| `EVERY_JOIN` | Executes on every player join |
| `ON_COMMAND` | Executes when a specific command is run |
| `ON_EVENT` | Executes when a specific custom event is tracked |

#### ON_EVENT Trigger

The `ON_EVENT` trigger allows A/B tests to activate when you track a custom event with `Analyse.trackEvent()`. This is useful for triggering experiments based on in-game milestones.

```java
// When this event is tracked, any A/B test configured 
// with trigger "ON_EVENT" and event name "tutorial_completed"
// will automatically execute its variant actions
Analyse.trackEvent("tutorial_completed")
    .withPlayer(player.getUniqueId(), player.getName())
    .withData("time_seconds", 300)
    .send();
```

**Example Use Case:** Reward players who complete the tutorial with different bonuses based on their A/B test variant.

### A/B Test Actions

When a test triggers, it can execute actions for the assigned variant:

| Action | Description | Platforms |
|--------|-------------|-----------|
| `SEND_MESSAGE` | Sends a colored message to the player | All |
| `RUN_COMMAND` | Executes a command as console or player | All |

## Complete Examples

### Track Player Quest Completion

```java
public void onQuestComplete(Player player, Quest quest) {
    if (!Analyse.isAvailable()) {
        return;
    }

    Analyse.trackEvent("quest_completed")
        .withPlayer(player.getUniqueId(), player.getName())
        .withData("quest_id", quest.getId())
        .withData("quest_name", quest.getName())
        .withData("difficulty", quest.getDifficulty().name())
        .withData("completion_time_seconds", quest.getCompletionTime())
        .withValue(quest.getRewardXP())
        .send();
}
```

### Track Economy Transactions

```java
public void onTransaction(Player player, double amount, String type) {
    Analyse.trackEvent("economy_transaction")
        .withPlayer(player.getUniqueId(), player.getName())
        .withData("type", type) // "deposit", "withdraw", "transfer"
        .withData("currency", "coins")
        .withValue(amount)
        .send();
}
```

### Track PvP Combat

```java
public void onPlayerKill(Player killer, Player victim) {
    Analyse.trackEvent("pvp_kill")
        .withPlayer(killer.getUniqueId(), killer.getName())
        .withData("victim_uuid", victim.getUniqueId().toString())
        .withData("victim_name", victim.getName())
        .withData("weapon", killer.getInventory().getItemInMainHand().getType().name())
        .withValue(1.0)
        .send();
}
```

### Implement A/B Tested Feature

```java
public void showWelcomeMessage(Player player) {
    if (!Analyse.isAvailable() || !Analyse.isTestActive("welcome_test")) {
        // Default behavior
        player.sendMessage("Welcome to the server!");
        return;
    }

    String variant = Analyse.getVariant(player.getUniqueId(), "welcome_test");
    
    switch (variant) {
        case "control" -> player.sendMessage("Welcome to the server!");
        case "variant_a" -> player.sendMessage("Hey " + player.getName() + "! Welcome back!");
        case "variant_b" -> {
            player.sendMessage("Welcome, " + player.getName() + "!");
            player.sendMessage("Check out /shop for daily deals!");
        }
    }
    
    // Track that the player saw the welcome message
    Analyse.trackConversion(
        player.getUniqueId(),
        player.getName(),
        "welcome_test",
        "message_shown"
    );
}
```

### Track With Callback

```java
public void trackImportantEvent(Player player, String action) {
    Analyse.trackEvent("important_action")
        .withPlayer(player.getUniqueId(), player.getName())
        .withData("action", action)
        .send(response -> {
            if (response != null && response.isSuccess()) {
                getLogger().info("Event tracked successfully: " + response.getEventId());
            } else {
                getLogger().warning("Failed to track event - will retry later");
                queueForRetry(player, action);
            }
        });
}
```

## Best Practices

### 1. Check Availability

Always check if Analyse is available before tracking:

```java
if (Analyse.isAvailable()) {
    Analyse.trackEvent("my_event").send();
}
```

### 2. Use Consistent Event Names

Use lowercase with underscores for event names:

```java
// Good
"quest_completed"
"shop_purchase"
"player_death"

// Bad
"QuestCompleted"
"Shop Purchase"
"PLAYER-DEATH"
```

### 3. Meaningful Data Keys

Use descriptive, consistent key names:

```java
// Good
.withData("item_id", "diamond_sword")
.withData("quantity", 1)

// Bad
.withData("i", "diamond_sword")
.withData("q", 1)
```

### 4. Use Values for Aggregations

Use `withValue()` for numeric data you want to aggregate (sum, average):

```java
// Track coins earned (can sum total coins earned)
.withValue(coinsEarned)

// Track damage dealt (can average damage per event)
.withValue(damageAmount)
```

### 5. Don't Block the Main Thread

The `send()` method is asynchronous and won't block. Don't add callbacks that do heavy processing:

```java
// Good - lightweight callback
.send(response -> {
    if (response != null) logger.fine("Tracked");
});

// Bad - heavy processing in callback
.send(response -> {
    database.insertAsync(response); // Don't do this
});
```

## Soft Dependency

To make Analyse a soft dependency, check for its presence:

```java
public class MyPlugin extends JavaPlugin {
    
    private boolean analyseEnabled = false;
    
    @Override
    public void onEnable() {
        // Check if Analyse is installed
        if (getServer().getPluginManager().getPlugin("Analyse") != null) {
            analyseEnabled = true;
            getLogger().info("Analyse integration enabled");
        }
    }
    
    public void trackEvent(String name) {
        if (analyseEnabled && Analyse.isAvailable()) {
            Analyse.trackEvent(name).send();
        }
    }
}
```

Or use `plugin.yml` / `paper-plugin.yml` soft dependency:

```yaml
name: MyPlugin
softdepend: [Analyse]
```
