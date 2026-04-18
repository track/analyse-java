# A/B testing

Running an A/B test from your plugin is two steps: read which variant a player is assigned to, and track a conversion when they do the winning action.

## Read a variant

```java
String variant = Analyse.getVariant(player.getUniqueId(), "new-spawn");

if ("B".equals(variant)) {
    player.teleport(newSpawnLocation);
} else {
    player.teleport(oldSpawnLocation);
}
```

`getVariant` is **deterministic**: the same player always gets the same variant for a given test. You can call it as many times as you want without accidentally flipping their experience mid-session.

If the test doesn't exist, is inactive, or hasn't synced yet, `getVariant` returns `null`. Always treat `null` as "variant A" (the control) so your code still works.

## Track a conversion

```java
Analyse.trackConversion(
    player.getUniqueId(),
    player.getName(),
    "new-spawn",      // the test key
    "first_purchase"  // the conversion event
);
```

A conversion means "this player did the thing we're testing for". Call it from wherever that action happens in your plugin.

You can call `trackConversion` multiple times for the same player (for example on every purchase). Analyse records all of them and the dashboard handles deduplication for the conversion rate.

## A full example

```java
@EventHandler
public void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();

    String variant = Analyse.getVariant(uuid, "new-spawn");
    if ("B".equals(variant)) {
        player.teleport(newSpawn);
    } else {
        player.teleport(oldSpawn);
    }
}

@EventHandler
public void onPurchase(PurchaseEvent event) {
    Analyse.trackConversion(
        event.getPlayer().getUniqueId(),
        event.getPlayer().getName(),
        "new-spawn",
        "first_purchase"
    );
}
```

## Check if a test is active

```java
if (Analyse.isTestActive("new-spawn")) {
    // run the test-aware code path
} else {
    // run your default behavior
}
```

Useful when you want to clean up test-specific code gracefully after a test ends.

## List all active tests

```java
for (ABTest test : Analyse.getActiveTests()) {
    getLogger().info("Active test: " + test.getKey());
}
```

## Trigger types

A/B tests can be triggered from inside or outside your plugin:

| Trigger | When it fires |
| --- | --- |
| `FIRST_JOIN` | The player joins for the first time. |
| `EVERY_JOIN` | Every join. |
| `ON_COMMAND` | A specific command is run. |
| `ON_EVENT` | A specific custom event is tracked. |

The `ON_EVENT` trigger hooks directly into `Analyse.trackEvent`. Any test configured for `ON_EVENT` with a matching event name will execute its variant actions automatically:

```java
// Any test with trigger ON_EVENT + event name "tutorial_completed"
// will fire its variant actions when this runs.
Analyse.trackEvent("tutorial_completed")
    .withPlayer(player.getUniqueId(), player.getName())
    .withData("duration_seconds", 300)
    .send();
```

## Variant actions

When a test triggers, it can execute these actions for the assigned variant:

| Action | What it does |
| --- | --- |
| `SEND_MESSAGE` | Sends a colored message to the player. |
| `RUN_COMMAND` | Runs a command as console or as the player. |

Variant actions are configured in the Analyse dashboard, not in code.

## Tips

- **Keep `getVariant` in a stable place.** If you call it from too many spots, it's harder to reason about. Call it once on join, cache the result, use it everywhere.
- **Don't modify conversion logic mid-test.** Changing what "counts" as a conversion invalidates the stats.
- **Give the test plenty of time.** Most tests need at least a few hundred assignments per variant before the confidence hits 95%. That's days for a small server.

## Related

- [Custom events](events.md)
- [Reference](reference.md)
