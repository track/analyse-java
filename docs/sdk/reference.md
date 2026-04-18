# Reference

A quick reference of every public class and method the SDK exposes. Use this page to scan; use the other pages for worked examples.

- [`Analyse`](#analyse)
- [`EventBuilder`](#eventbuilder)
- [`SessionManager`](#sessionmanager)
- [`PlayerSession`](#playersession)
- [`ABTestManager`](#abtestmanager)
- [`ABTest`](#abtest)
- [`AnalyseMessaging`](#analysemessaging)

## `Analyse`

`net.analyse.api.Analyse`

Static entry point. You'll touch this class more than any other.

### Availability

| Method | Returns | What it does |
| --- | --- | --- |
| `isAvailable()` | `boolean` | `true` when the Analyse plugin is loaded. Always check this first. |
| `isConnected()` | `boolean` | `true` when the last API call succeeded (fresh heartbeat). |
| `getLastConnectionError()` | `String` | The reason the last call failed, or `null` if healthy. |

### Events

| Method | Returns | What it does |
| --- | --- | --- |
| `trackEvent(String name)` | `EventBuilder` | Start a fluent builder for a custom event. |
| `trackEvent(String name, UUID uuid, String username)` | `EventBuilder` | Same as above with the player preset. |
| `trackEvent(String name, Map<String, Object> data)` | `EventBuilder` | Same as above with properties preset. |

### A/B tests

| Method | Returns | What it does |
| --- | --- | --- |
| `getVariant(UUID uuid, String testKey)` | `String` | The variant key assigned to the player. `null` when the test isn't active. |
| `isTestActive(String testKey)` | `boolean` | `true` when the test is running. |
| `getActiveTests()` | `List<? extends ABTest>` | Every currently active test. |
| `getTest(String testKey)` | `ABTest` | The test by key, or `null`. |
| `trackConversion(UUID uuid, String username, String testKey, String eventName)` | `void` | Record a conversion for a test. |

### Managers

| Method | Returns | What it does |
| --- | --- | --- |
| `get()` | `AnalysePlatform` | The current platform (Spigot, Velocity, etc.). |
| `sessions()` | `SessionManager` | Live sessions. |
| `abTests()` | `ABTestManager` | A/B test manager. |

## `EventBuilder`

`net.analyse.api.object.builder.EventBuilder`

Returned by `Analyse.trackEvent(...)`.

| Method | Returns | What it does |
| --- | --- | --- |
| `withPlayer(UUID uuid, String username)` | `EventBuilder` | Link the event to a player. |
| `withPlayer(UUID uuid)` | `EventBuilder` | Link by UUID only. |
| `withData(String key, Object value)` | `EventBuilder` | Add a property. |
| `withData(Map<String, Object> values)` | `EventBuilder` | Add multiple properties at once. |
| `withValue(double value)` | `EventBuilder` | Set the numeric main value. |
| `send()` | `void` | Send the event. Non-blocking. |
| `send(Consumer<Boolean> callback)` | `void` | Send and receive `true` on success, `false` on failure. |

Getters (mostly for addon authors): `getName()`, `getPlayerUuid()`, `getPlayerUsername()`, `getData()`, `getValue()`.

## `SessionManager`

`net.analyse.api.manager.SessionManager`

| Method | Returns | What it does |
| --- | --- | --- |
| `getSession(UUID uuid)` | `Optional<? extends PlayerSession>` | The session for an online player. |
| `hasSession(UUID uuid)` | `boolean` | `true` when the player has an active session. |
| `getAllSessions()` | `Collection<? extends PlayerSession>` | Every active session. |
| `getSessionCount()` | `int` | Current session count. |

## `PlayerSession`

`net.analyse.api.session.PlayerSession`

| Method | Returns | What it does |
| --- | --- | --- |
| `getPlayerUuid()` | `UUID` | The Minecraft UUID. |
| `getSessionId()` | `String` | The Analyse session ID, or `null` if it hasn't synced yet. |
| `getHostname()` | `String` | The hostname they joined through. |
| `getIp()` | `String` | The IP address they connected from. |
| `getJoinTime()` | `Instant` | When they joined. |
| `hasActiveSession()` | `boolean` | `true` once Analyse has confirmed the session. |

## `ABTestManager`

`net.analyse.api.manager.ABTestManager`

| Method | Returns | What it does |
| --- | --- | --- |
| `getVariant(UUID uuid, String testKey)` | `String` | Variant key for the player. |
| `getActiveTests()` | `List<? extends ABTest>` | Active tests. |
| `getTest(String testKey)` | `ABTest` | A test by key. |
| `isTestActive(String testKey)` | `boolean` | Whether a test is live. |
| `trackConversion(UUID uuid, String username, String testKey, String eventName)` | `void` | Record a conversion. |

## `ABTest`

`net.analyse.api.object.abtest.ABTest`

| Method | Returns | What it does |
| --- | --- | --- |
| `getId()` | `String` | The internal test ID. |
| `getKey()` | `String` | The test key you use to look it up. |
| `getName()` | `String` | Human-readable name. |
| `getTrigger()` | `Trigger` | `FIRST_JOIN`, `EVERY_JOIN`, `ON_COMMAND`, or `ON_EVENT`. |
| `getTriggerCommand()` | `String` | Command that triggers the test (when `trigger == ON_COMMAND`), else `null`. |
| `getTriggerEvent()` | `String` | Event that triggers the test (when `trigger == ON_EVENT`), else `null`. |
| `getStatus()` | `Status` | `DRAFT`, `ACTIVE`, `PAUSED`, or `COMPLETED`. |
| `isActive()` | `boolean` | Shortcut for `getStatus() == ACTIVE`. |
| `getVariants()` | `List<? extends Variant>` | All configured variants. |
| `getPrimaryMetric()` | `String` | Primary conversion metric name. |
| `assignVariant(UUID uuid)` | `Variant` | Deterministically assign a variant to a player. |
| `getVariant(String variantKey)` | `Variant` | A single variant by key. |

### Enums

**`ABTest.Trigger`** &mdash; `FIRST_JOIN`, `EVERY_JOIN`, `ON_COMMAND`, `ON_EVENT`.

**`ABTest.Status`** &mdash; `DRAFT`, `ACTIVE`, `PAUSED`, `COMPLETED`.

## `AnalyseMessaging`

`net.analyse.api.messaging.AnalyseMessaging`

Used from backend servers behind a proxy to push events through plugin messaging.

| Member | Type | What it is |
| --- | --- | --- |
| `CHANNEL` | `String` | The plugin channel name (`analyse:events`). |
| `TYPE_EVENT` | `String` | Message type constant for custom events (`event`). |
| `TYPE_CONVERSION` | `String` | Message type constant for A/B conversions (`conversion`). |

| Method | Returns | What it does |
| --- | --- | --- |
| `event(String name)` | `EventMessageBuilder` | Fluent builder for an event message. |
| `createEventMessage(name, uuid, username, data, value)` | `byte[]` | Build the raw bytes directly. |
| `createConversionMessage(uuid, username, testKey, eventName)` | `byte[]` | Build a conversion message. |

### `AnalyseMessaging.EventMessageBuilder`

| Method | Returns | What it does |
| --- | --- | --- |
| `withPlayer(UUID uuid, String username)` | `EventMessageBuilder` | Link the message to a player. |
| `withData(String key, Object value)` | `EventMessageBuilder` | Add a property. |
| `withValue(double value)` | `EventMessageBuilder` | Set the numeric main value. |
| `build()` | `byte[]` | Encode the message. Throws `IllegalStateException` if no player was set. |

## Related

- [SDK overview](README.md)
- [Custom events](events.md)
- [A/B testing](ab-testing.md)
- [Sessions](sessions.md)
- [Plugin messaging](plugin-messaging.md)
