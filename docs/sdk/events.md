# Custom events

Custom events are how you teach Analyse about the things that make your server unique. The SDK has a small fluent builder for them.

## Quick example

```java
import net.analyse.api.Analyse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class QuestListener implements Listener {

    @EventHandler
    public void onQuestComplete(QuestCompleteEvent event) {
        Player player = event.getPlayer();

        Analyse.trackEvent("quest_completed")
            .withPlayer(player.getUniqueId(), player.getName())
            .withData("quest_id", event.getQuestId())
            .withData("difficulty", event.getDifficulty())
            .withValue(event.getRewardValue())
            .send();
    }
}
```

Every time a quest completes, Analyse gets an event with the quest ID, difficulty, and reward value, linked to the player.

## The builder

```java
Analyse.trackEvent(String name)
    .withPlayer(UUID uuid, String username)
    .withData(String key, Object value)
    .withData(Map<String, Object> values)
    .withValue(double value)
    .send();
```

- `name` is the event name. Lowercase with underscores: `quest_completed`, `first_kill`, `crate_opened`.
- `withPlayer` links the event to a player. Optional but almost always useful.
- `withData` adds a property. Call it as many times as you want.
- `withValue` is a numeric "main metric" for the event (reward amount, damage dealt, price paid). Used for sum/average widgets in Dashboards.
- `send` fires the event. Fire-and-forget.

## Send with a callback

If you need to know whether the event was delivered:

```java
Analyse.trackEvent("important_action")
    .withPlayer(player.getUniqueId(), player.getName())
    .send(success -> {
        if (success) {
            getLogger().fine("Event tracked");
        } else {
            getLogger().warning("Failed to track event");
        }
    });
```

The callback receives a `Boolean`: `true` on success, `false` on failure.

> [!NOTE]
> `send` is non-blocking. Don't do heavy work in the callback &mdash; it runs on a networking thread.

## Rules

- **Name stability matters.** Don't rename events. If you have to, treat it as a new event and keep the old name for a while.
- **Limit properties.** 10 or fewer per event is the sweet spot. Overdoing it makes filtering confusing.
- **Primitive values only.** Numbers, booleans, strings. Objects get stringified, which is rarely what you want.
- **Don't send PII.** No email, no IP, no real names. Minecraft username and UUID are fine.

## Checking availability

If your plugin should also work when Analyse is missing, wrap the calls:

```java
if (Analyse.isAvailable()) {
    Analyse.trackEvent("quest_completed")
        .withPlayer(player.getUniqueId(), player.getName())
        .send();
}
```

Or let it throw and catch silently:

```java
try {
    Analyse.trackEvent("quest_completed")
        .withPlayer(player.getUniqueId(), player.getName())
        .send();
} catch (IllegalStateException ignored) {
    // Analyse not initialized yet, that's fine
}
```

## Good events to add first

- `tutorial_finished` with `duration_seconds`, `skipped_steps`
- `first_kill` with `weapon`, `target_type`
- `rank_activated` with `rank_name`, `price`
- `store_viewed` with `source` (what triggered the open)
- `match_finished` with `result`, `duration_seconds`

## Style

Use descriptive, consistent naming. Lowercase with underscores for names and keys:

```java
// Good
.withData("item_id", "diamond_sword")
.withData("quantity", 1)

// Bad
.withData("i", "diamond_sword")
.withData("q", 1)
.withData("ItemID", "diamond_sword")
```

Use `withValue` for the one numeric "main metric" the event is about &mdash; the amount you'd want to sum or average in a dashboard widget:

```java
.withValue(coinsEarned)
.withValue(damageDealt)
.withValue(secondsPlayed)
```

## Full example

```java
public void onTransaction(Player player, double amount, String type) {
    Analyse.trackEvent("economy_transaction")
        .withPlayer(player.getUniqueId(), player.getName())
        .withData("type", type)
        .withData("currency", "coins")
        .withValue(amount)
        .send();
}

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

## Events from a backend (with a proxy)

If you run Velocity or BungeeCord in front of your backends, and you want a backend to send events through the proxy without its own API key, use `AnalyseMessaging`. See [plugin messaging](plugin-messaging.md).

## Related

- [A/B testing](ab-testing.md)
- [Sessions](sessions.md)
- [Reference](reference.md)
