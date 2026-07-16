# WINDFALL ANTICHEAT F — PROJECT MEMORY BANK
> Auto-generated for session continuity across compactions.
> Last updated: 2026-07-16 (v1.0.0 — Initial Fabric port from Spigot v2.3.2)

---

## §0 PROJECT GOALS (NON-NEGOTIABLE)

**Every decision, feature, and fix must serve these two goals:**

### Goal 1: Best Possible Detection Logic
- Physics engine must be pixel-accurate to vanilla Minecraft
- Checks must have zero false positives on legitimate players
- Detection confidence must be high before flagging
- Setbacks must be precise (teleport to last safe position, not origin)
- Edge cases (pistons, elytra, swimming, climbing, ice, soul sand) must be handled
- Each check must be version-aware (physics differ across MC versions)

### Goal 2: Maximum Compatibility
- **MC Versions:** 1.21.5+ (Fabric only)
- **Java Versions:** 17+ (no Java 12-16 features, Java 21 required for runtime)
- **Mod Loader:** Fabric Loader 0.16.x+
- **API:** Fabric API 0.119.2+
- **Mappings:** Mojmap (official Mojang mappings)
- **Never break:** backward compatibility, existing configs, existing API contracts
- **Never assume:** Java version, MC version, mod loader version

### RULE: MEMORYBANK PROTOCOL
1. **Read MEMORYBANK.md at the start of every session** before doing any work
2. **Update MEMORYBANK.md after every commit/push** — add entry to Session History, update version, update any affected sections
3. **Never skip the goals section** — every change must be justified against Goal 1 or Goal 2
4. **If a change conflicts with a goal, stop and discuss** — goals override features

### RULE: SESSION WORKFLOW (EVERY SESSION)
1. **Read MEMORYBANK.md** — understand current state, version, recent changes
2. **Identify the task** — what specifically needs to be done
3. **Plan the steps** — break task into atomic steps, list them in TODO
4. **Execute Goal 1** — implement detection logic improvement
5. **Test Goal 1** — verify accuracy, zero false positives
6. **If fail → fix → re-test** — never skip a failure
7. **Execute Goal 2** — verify compatibility
8. **If fail → fix → re-test** — never skip a failure
9. **Run lint/typecheck** — verify code quality
10. **Run tests** — verify all tests pass
11. **Commit** — atomic commit with clear message
12. **Update MEMORYBANK.md** — document what was done, update version
13. **Push** — only after everything is verified

### RULE: FEATURE WORKFLOW (ADDING NEW CHECKS)
1. **Research the check** — what cheat does it detect, how does the cheat work
2. **Check competitors** — does Grim/Arrow/TruthfulAC have it? How do they implement it?
3. **Define the physics** — what vanilla MC behavior does legitimate play look like
4. **Add `@CheckData` annotation** — name, stableKey, minVersion/maxVersion, decay, setbackVl
5. **Implement check class** — extend Check, implement onPacketReceive/onPacketSend
6. **Handle version differences** — use VersionBracket for physics that change across versions
7. **Add per-player state** — use WindfallPlayer fields, implement removePlayer() cleanup
8. **Write unit tests** — minimum 5 tests covering normal, edge case, cheat scenario, false positive
9. **Register in CheckManager** — add to the checks list
10. **Add to config** — default enabled/disabled, max-vl, setback-vl
11. **Update MEMORYBANK.md** — add to checks table, session history
12. **Commit** — V:x.y.z + Add [CheckName]Check

### RULE: FIX WORKFLOW (BUG FIXES)
1. **Reproduce the bug** — confirm it exists, document steps
2. **Find the root cause** — trace the code path, don't guess
3. **Fix the root cause** — not the symptom
4. **Verify the fix** — run tests, manual testing if needed
5. **Check for regressions** — does this fix break anything else?
6. **Check for similar issues** — if one check has the bug, check others
7. **Update MEMORYBANK.md** — document the fix in session history
8. **Commit** — V:x.y.z + Fix [description]

### RULE: COMMIT NAMING FORMAT
**Format:** `V:x.y.z + Small summary`
- `V:` prefix (capital V, colon)
- `x.y.z` = current version
- `+` separator
- Small summary = 3-8 words describing the change
- Examples:
  - `V:1.0.0 + Fix ReachCheck distance calculation`
  - `V:1.1.0 + Add new ScaffoldCheck`
  - `V:1.0.1 + Fix thread safety in MacroCheck`
- **ALWAYS** bump version BEFORE committing

### RULE: ERROR HANDLING
1. **Never swallow exceptions silently** — every catch must log at an appropriate level
2. **Fabric errors → FINE level** — known upstream issues, not server-breaking
3. **Our code errors → WARNING level** — something we can fix
4. **Critical errors → SEVERE level** — data loss, security, server crash
5. **Never use empty catch blocks** — at minimum log at FINE level with context
6. **Never catch Error or OutOfMemoryError** — let the JVM handle those
7. **Use specific exception types** — catch Exception only for top-level dispatchers

### RULE: TESTING
1. **All tests must pass** before any commit — no exceptions
2. **New checks MUST have unit tests** — minimum 5 tests per check
3. **Run `./gradlew test` locally** — don't rely solely on CI
4. **If a test is skipped, document why** — `@Disabled` must have reason string
5. **Mock external dependencies** — Fabric API must be mocked in tests
6. **Test edge cases** — null inputs, empty collections, boundary values, negative numbers

### RULE: DOCUMENTATION
1. **Every public class MUST have class-level Javadoc** — purpose, usage, thread safety
2. **Every public method MUST have method-level Javadoc** — params, return, throws, thread safety
3. **Every check MUST have `@CheckData` annotation** — name, stableKey, versions, decay, setback
4. **Complex algorithms MUST have inline comments** — explain the "why", not the "what"
5. **MEMORYBANK.md MUST be updated after every commit** — session history + affected sections
6. **Never document self-evident code** — `i++` doesn't need a comment
7. **Document deviations from vanilla** — if physics differ, explain why

### RULE: SECURITY
1. **Never log secrets** — no tokens, keys, passwords in logs
2. **Never commit secrets** — use Gradle properties for signing keys
3. **Never trust client input** — validate all packet data before processing
4. **Scan dependencies regularly** — Fabric Loom dependency resolution
5. **Update vulnerable dependencies** — when upstream fixes are available
6. **Document known CVEs** — if a dependency has a CVE we can't fix, document it

---

## §1 PROJECT OVERVIEW

**Windfall AntiCheat F** is a Fabric mod port of the Spigot/Paper Windfall anti-cheat plugin.

**Source:** https://github.com/enis1enis2/Windfall-AntiCheat (Fabric branch)
**Platform:** Fabric Mod (NOT Bukkit/Spigot/Paper)
**Base:** 1:1 port from Windfall Spigot (v2.3.2)
**License:** MIT — Copyright (c) 2026 Enis Polat
**Build:** Gradle, Java 17+, `./gradlew build` → `build/libs/windfall-fabric-1.0.0.jar`
**Core dependency:** Fabric API 0.119.2+ (mod loader dependency)
**Total checks:** 53 (12 combat + 29 movement + 11 packet + 1 inventory)
**Unit tests:** 76 (all passing, JUnit 5, NO Mockito — Unsafe + reflection only)

---

## §2 SOURCE CODE

- Package: `io.windfall.anticheat`
- Entry point: `WindfallMod.java` (implements `ModInitializer`)
- Config: JSON-based via Gson at `config/windfall.json`
- Commands: Brigadier (Fabric native)
- Networking: Fabric `ServerPlayNetworking` + Mixins
- Mappings: Mojmap (official Mojang mappings)

---

## §3 SOURCE STRUCTURE

```
src/main/java/io/windfall/anticheat/
├── WindfallMod.java              # Main entry point (ModInitializer)
├── api/
│   ├── WindfallAPI.java
│   ├── WindfallAPIImpl.java
│   ├── WindfallProvider.java
│   └── WindfallPlayerData.java
├── core/
│   ├── check/
│   │   ├── Check.java            # Base class
│   │   ├── CheckData.java        # Annotation
│   │   ├── CheckManager.java     # Registry + dispatch
│   │   ├── CompatFlag.java       # Compat flags
│   │   ├── PacketCheck.java      # Packet check base
│   │   └── impl/
│   │       ├── combat/           # 12 combat checks
│   │       ├── movement/         # 29 movement checks
│   │       ├── packet/           # 11 packet checks
│   │       └── inventory/        # 1 inventory check
│   ├── config/WindfallConfig.java
│   ├── network/PacketListener.java
│   ├── player/
│   │   ├── WindfallPlayer.java
│   │   ├── PlayerManager.java
│   │   ├── PlayerProfile.java
│   │   └── data/ActionData.java
│   ├── physics/
│   │   ├── PredictionEngine.java
│   │   ├── PredictionContext.java
│   │   ├── PhysicsConstants.java
│   │   ├── BoundingBox.java
│   │   └── VersionPhysics.java
│   ├── compensation/
│   │   ├── TransactionManager.java
│   │   ├── PingPongManager.java
│   │   ├── LatencyCompensator.java
│   │   ├── SimulationEngine.java
│   │   ├── CompensatedWorld.java
│   │   ├── CompensatedEntities.java
│   │   └── WorldChange.java
│   ├── severity/
│   │   ├── SeverityManager.java
│   │   └── ViolationPattern.java
│   ├── punishment/PunishmentEngine.java
│   ├── alert/
│   │   ├── AlertManager.java
│   │   └── DiscordWebhook.java
│   ├── adaptive/AdaptiveThreshold.java
│   ├── fingerprint/PacketFingerprint.java
│   ├── metrics/WindfallPrometheus.java
│   ├── command/CommandManager.java
│   ├── version/
│   │   ├── VersionManager.java
│   │   ├── VersionBracket.java
│   │   └── ServerFork.java
│   ├── util/
│   │   ├── MathUtil.java
│   │   └── MaterialUtils.java
│   └── compat/WorldGuardCompat.java (stub)
├── core/
│   ├── bedrock/
│   │   ├── BedrockInfo.java          # Device/input data
│   │   └── GeyserManager.java        # Geyser/Floodgate detection (Fabric: mod-based)
│   └── plugin/
│       └── ModDetector.java           # ViaVersion/Geyser/OCM/WorldGuard detection
└── mixin/
    ├── ServerPlayNetworkHandlerMixin.java  # 11 C2S packet injections
    ├── ClientConnectionMixin.java          # Generic S2C packet hook
    └── PlayerEntityMixin.java              # Sprint/gliding state sync
```

---

## §4 TEST COVERAGE

76 unit tests across 7 test classes (JUnit 5, NO Mockito — Unsafe + reflection only):

| Test Class | Tests | What it covers |
|---|---|---|
| CheckBaseTest | 19 | Check metadata, buffer ops, flag/reward/decay, removePlayer |
| PerPlayerStateTest | 13 | State map types, per-player independence, removePlayer clears state |
| BadPacketsDetectionTest | 16 | NaN, Y bounds, rotation, duplicate, auto-clicker, removePlayer |
| ChatDetectionTest | 7 | Burst, rate limit, removePlayer |
| CrashDetectionTest | 7 | Oversized chat, creative buffer state, removePlayer |
| NoFallDetectionTest | 5 | Fall + ground claim, flying, descent, removePlayer |
| CriticalsDetectionTest | 10 | Attack on ground/flying/gliding/sneaking, airborne no motion, valid motion, removePlayer |

**Run tests:** `./gradlew test`
**Test infrastructure:** `sun.misc.Unsafe.allocateInstance()` + reflection for all object creation; no mocking at all

---

## §5 KEY CHANGES FROM SPIGOT VERSION

1. **PacketEvents → Fabric Networking**: `ServerPlayNetworking.registerGlobalReceiver()` + Mixins
2. **Bukkit Player → ServerPlayerEntity**: Direct Fabric entity access
3. **Bukkit World → ServerWorld**: Direct Fabric world access
4. **Bukkit Config → JSON**: Gson-based config at `config/windfall.json`
5. **Bukkit Commands → Brigadier**: Native Fabric command system
6. **Bukkit Events → Fabric Events**: `ServerPlayConnectionEvents`, `ServerTickEvents`
7. **PlatformScheduler → ServerTickEvents**: `END_SERVER_TICK` for tick loop
8. **Geyser/Bedrock → ModDetector**: `FabricLoader.getInstance().isModLoaded()` instead of plugin detection
9. **ViaVersion → REMOVED**: Fabric has native version support
10. **Folia/Purpur → REMOVED**: Fabric has its own threading model
11. **WorldGuard → REMOVED**: Not available on Fabric
12. **bStats → REMOVED**: Can use Fabric Metrics API instead
13. **Prometheus → SIMPLIFIED**: In-memory counters only
14. **ServerFork → REMOVED**: No fork detection needed (Fabric only)
15. **CompatFlag → Updated**: Removed FOLIA_UNSAFE, PURPUR_KB_DEPENDENT; kept VIAVERSION_SENSITIVE (stub), GEYSEIR_SENSITIVE, RELAX_ON_MISMATCH, VERSION_* flags
16. **ModDetector**: Fabric-based detection for ViaVersion (stub), Geyser, OCM mods via `FabricLoader.getInstance().isModLoaded()`

---

## §6 ALL 53 CHECKS — Detection Details

### COMBAT (12)

| # | Key | Name | Detects | Versions | Decay | Setback |
|---|-----|------|---------|----------|-------|---------|
| 1 | windfall.combat.aim | Aim A | Inhuman aim patterns — instant-snap detection (180° threshold) + yaw/pitch variance-ratio analysis (aimbot variance < 0.5) | All | 0.01 | 10 |
| 2 | windfall.combat.autoclicker | Autoclicker A | Automated click-rate — standard deviation of click intervals < 3ms over 3s window (human > 15ms). CPS bounds: legacy 6-20, modern 1-8 | All | 0.01 | 20 |
| 3 | windfall.combat.backtrack | Backtrack A | Delayed/frozen position updates — time gap between movement and attack packets > 500ms | All | 0.01 | 15 |
| 4 | windfall.combat.criticals | Criticals A | Critical-hit exploits — attacks while airborne without valid fall motion (vanilla requires deltaY 0.11-0.5) | All | 0.01 | 10 |
| 5 | windfall.combat.fastheal | Fast Heal A | Fast-heal exploits — health regen faster than vanilla, frequency-based (3+ swings in 500ms) and spike analysis (>6 hearts instant) | **1.7-1.8 only** | 0.02 | 10 |
| 6 | windfall.combat.hitboxes | Hitboxes A | Hitbox expansion — projection to max reach, hit-ratio > 80% at 3.5 blocks distance indicates expanded hitboxes | All | 0.01 | 15 |
| 7 | windfall.combat.killaura | Kill Aura A | Target-switching speed (multi-aura: >4 targets/sec modern, >6 legacy) + rotation symmetry analysis (bot rotation symmetry > 0.95) | All | 0.005 | 25 |
| 8 | windfall.combat.macro | Macro A | Movement macros — encodes packets as P/R/M/F characters, detects repetitive substrings (90% repetition over 50-char window) | All | 0.01 | 20 |
| 9 | windfall.combat.multiinteract | Multi Interact A | Multi-aura — >2 distinct entities attacked within single tick window (~60ms) | All | 0.01 | 15 |
| 10 | windfall.combat.reach | Reach A | Reach extension — Euclidean distance from eye to target AABB, lag-compensated (ping * 0.001, max 0.3), legacy 4.0, modern 3.0 | All | 0.05 | 10 |
| 11 | windfall.combat.selfinteract | Self Interact A | Self-attack packets — impossible from vanilla client, instant flag + kick | All | 0.0 | 5 |
| 12 | windfall.combat.swordblock | Sword Block A | Impossible sword-block-while-attacking on pre-1.9 — block-attack timing < 200ms or speed ratio > 0.7 | **1.7-1.8 only** | 0.015 | 10 |

### MOVEMENT (29)

| # | Key | Name | Detects | Versions | Decay | Setback |
|---|-----|------|---------|----------|-------|---------|
| 13 | windfall.movement.airliquidbreak | Air Liquid Break | Breaking blocks while airborne (deltaY < -0.5) or in liquid | All | 0.02 | 10 |
| 14 | windfall.movement.airliquidplace | Air Liquid Place | Placing blocks while airborne or in liquid | All | 0.02 | 10 |
| 15 | windfall.movement.baritone | Baritone A | Automated pathfinding — unnaturally straight lines (tolerance < 0.02 rad over 20 ticks) + perfectly constant speed (95% consistency over 40 samples) | All | 0.01 | 20 |
| 16 | windfall.movement.elytra | Elytra A | Illegal elytra flight — horizontal > 1.5 blocks/tick, sustained hovering (>40 ticks delta < 0.005), impossible upward boosts | All | 0.01 | 20 |
| 17 | windfall.movement.farbreak | Far Break A | Breaking blocks from > 5.0 blocks reach (Euclidean eye-to-center), 0.3 tolerance | All | 0.01 | 15 |
| 18 | windfall.movement.farplace | Far Place A | Placing blocks from > 5.0 blocks reach, 0.3 tolerance | All | 0.01 | 15 |
| 19 | windfall.movement.fastbreak | Fast Break A | Breaking blocks faster than vanilla survival — per-material break time table (obsidian=50s, diamond=5s, stone=1.5s, dirt=0.5s), 15% tolerance | All | 0.02 | 20 |
| 20 | windfall.movement.fly | Fly A | Unnatural vertical movement — flight, hover, upward motion without valid cause, uses PredictionEngine physics prediction. Bypass resistance: `VERTICAL_TOLERANCE_UNCONFIRMED = 0.15` | All | 0.01 | 15 |
| 21 | windfall.movement.groundspoof | Ground Spoof A | False on-ground state — claims ground while airborne > 3s with falling velocity > 0.3 | All | 0.01 | 20 |
| 22 | windfall.movement.invalidbreak | Invalid Break A | Breaking air or indestructible blocks (bedrock, barriers, end portals, command blocks) | All | 0.02 | 10 |
| 23 | windfall.movement.invalidplace | Invalid Place A | Rate-limit violations (>4/tick), placing into occupied blocks, self-intersection | All | 0.02 | 10 |
| 24 | windfall.movement.motion | Motion A | Impossible horizontal/vertical motion — ground max 0.28, sprint 0.36, airborne max 1.0, vertical max 1.5 | All | 0.01 | 20 |
| 25 | windfall.movement.multibreak | Multi Break | Multiple block breaks (START_DIGGING) within single tick — Nuker/FastBreak | All | 0.02 | 10 |
| 26 | windfall.movement.multiplace | Multi Place | Multiple block placements within single tick — hacked client | All | 0.02 | 10 |
| 27 | windfall.movement.nofall | NoFall A | onGround=true while falling with velocity > 0.3 and distance > 2.0 blocks | All | 0.01 | 15 |
| 28 | windfall.movement.noslow | NoSlow A | Bypassing vanilla item-use slowdown — speed > 90% sprint cap while using bow/shield/food | All | 0.01 | 15 |
| 29 | windfall.movement.noswing | No Swing A | Breaking/placing without arm-swing animation — 300ms timeout, 3 consecutive missing swings | All | 0.02 | 10 |
| 30 | windfall.movement.phase | Phase A | Noclip — moving inside/through solid blocks, > 0.1 blocks clipping over 3 ticks | All | 0.01 | 20 |
| 31 | windfall.movement.positionbreak | Position Break | Breaking blocks from > 5.0 blocks (squared-distance, 0.5 tolerance) | All | 0.01 | 15 |
| 32 | windfall.movement.positionplace | Position Place | Placing blocks from > 5.0 blocks (squared-distance, 0.5 tolerance) | All | 0.01 | 15 |
| 33 | windfall.movement.rotationbreak | Rotation Break A | Excessive view rotation change (> 45°) between break start and finish | All | 0.02 | 15 |
| 34 | windfall.movement.rotationplace | Rotation Place A | Player rotation doesn't face target block (> 45° yaw or pitch deviation) | All | 0.02 | 10 |
| 35 | windfall.movement.scaffold | Scaffold A | Auto-bridge — block placement speed > 12/sec, 4/sec while sprinting | All | 0.005 | 30 |
| 36 | windfall.movement.simulation | Simulation A | Movement simulation mismatch — vertical movement not matching vanilla gravity + drag, deviation > 0.15 over 10 samples. Bypass resistance: `SimulationEngine.simulate()` multi-scenario | All | 0.01 | 20 |
| 37 | windfall.movement.speed | Speed A | Horizontal speed exceeding predicted max for state (sprint, potions, ice, soul sand). 5% headroom normal, 20% when unconfirmed. Bypass resistance: `SPEED_TOLERANCE_UNCONFIRMED = 1.20` | All | 0.01 | 20 |
| 38 | windfall.movement.step | Step A | Step-height violation — instant upward > 0.6 blocks (sneak), > 2.0 (ladder) without jumping | All | 0.01 | 15 |
| 39 | windfall.movement.timer | Timer A | Timer speed/slow hacks — movement packets per tick over 1s window, speedhack > 1.2x or slowhack < 0.5x sustained | All | 0.005 | 25 |
| 40 | windfall.movement.velocity | Velocity A | Knockback rejection — entity velocity received but movement doesn't reflect it, version-aware physics (legacy 0.4, modern 0.28 horizontal, 0.4 vertical) | All | 0.01 | 30 |
| 41 | windfall.movement.wrongbreak | Wrong Break A | Spatially inconsistent breaks — > 2.0 blocks Y deviation or > 10.0 blocks horizontal teleport between breaks | All | 0.02 | 10 |

### PACKET (11)

| # | Key | Name | Detects | Versions | Decay | Setback |
|---|-----|------|---------|----------|-------|---------|
| 42 | windfall.packet.bad | Bad Packets A | Malformed packets — NaN/Infinite coords (instant kick), Y out of bounds (modern -64 to 400, legacy 0-256), invalid rotation, > 20 attacks/tick, pre-login movement | All | 0.0 | 5 |
| 43 | windfall.packet.clientbrand | Client Brand A | Known hacked clients — brand string matched against 26 cheat clients (wurst, impact, moon, liquidbounce, meteor, etc.) | All | 0.0 | 10 |
| 44 | windfall.packet.chat | Chat A | Chat flooding — dual sliding window: 60/min sustained + 4/2s burst | All | 0.01 | 15 |
| 45 | windfall.packet.cheststealer | Chest Stealer A | Automated chest stealing — > 40 clicks/window, > 6 fast clicks in 500ms, > 15 items/sec | All | 0.01 | 15 |
| 46 | windfall.packet.crash | Crash A | Crash packets — oversized chat (> 32767 chars), suspicious creative inventory actions | All | 0.0 | 5 |
| 47 | windfall.packet.creative | Creative A | Creative inventory exploits — actions in non-creative mode, > 5 creative actions/tick | All | 0.0 | 5 |
| 48 | windfall.packet.exploit | Exploit A | Invalid/out-of-range values — window clicks (id > 200, slot > 60), entity interactions (id > 20000), held item (0-8 only) | All | 0.0 | 5 |
| 49 | windfall.packet.order | Packet Order A | Out-of-order/duplicate packets — > 5 duplicates, > 15 packets in 100ms burst, movement before login | All | 0.01 | 15 |
| 50 | windfall.packet.sprint | Sprint A | Abnormal sprint toggling — > 4 toggles/sec sustained over 3 consecutive windows (kill aura bots) | All | 0.01 | 15 |
| 51 | windfall.packet.vehicle | Vehicle A | Vehicle exploits — INTERACT_AT when not mounted, > 3 steer/tick, vehicle speed > 2.0 | All | 0.01 | 15 |
| 52 | windfall.packet.transaction | Transaction A | Delayed packet injection — transaction packet ordering violations (1.21.5: keepalive-based) | All | 0.01 | 15 |

### INVENTORY (1)

| # | Key | Name | Detects | Versions | Decay | Setback |
|---|-----|------|---------|----------|-------|---------|
| 53 | windfall.inventory.inventory | Inventory A | Inhuman inventory clicks — > 20 clicks/sec, rapid burst (5+ clicks in 50ms), creative access in survival | All | 0.02 | 15 |

---

## §7 ARCHITECTURE

### Packet Flow (Fabric)
```
Fabric Mixins (ServerPlayNetworkHandlerMixin, ClientConnectionMixin)
  → WindfallPlayer state update (position, rotation, velocity, ground, etc.)
  → TransactionManager (keepalive → ping)
  → PingPongManager (dual-ping sandwich → confirmed tick)
  → CheckManager.onPacketReceive/Send()
    → For each enabled check: check.onPacketReceive(player, event)
```

### Bypass Resistance Engine
```
Per-Tick:
  → PingPongManager.onTickStart() — start dual-ping sandwich
  → LatencyCompensator.processDeferredChanges() — apply queued world changes
  → [checks run with confirmed tick + compensated world]
  → PingPongManager.onTickEnd() — close dual-ping, fire callbacks

Per-Packet (movement):
  → SimulationEngine.needsSimulation() — check if state changes unconfirmed
  → If unconfirmed: widen tolerances (Speed +20%, Flight +0.15)
  → SimulationEngine.simulate() — multi-scenario prediction (up to 16 scenarios)
  → If any scenario matches: decrease buffer (not flag)
```

### Flagging
```
check.flag(player)
  → SeverityManager.getScaledVlIncrement() [1x to 2x based on cumulative VL]
  → player.violationLevels.merge(stableKey, increment)
  → AlertManager.sendAlert() [in-game + Discord]
  → PunishmentEngine.evaluate() [warn → kick → tempban → permban]
  → If VL >= setbackVl: performSetback() + reset VL
```

### Tick System (50ms)
```
ServerTickEvents.END_SERVER_TICK → CheckManager.onTick()
  → player.resetTickState()
  → check.reward(player) [VL decay + buffer decay]
  → PunishmentEngine.decayTierIfNeeded(player)
```

### @CheckData Annotation
```java
@Retention(RUNTIME) @Target(TYPE)
public @interface CheckData {
    String name();
    String stableKey();
    double decay() default 0.02;
    int setbackVl() default 20;
    int minVersion() default 767;    // MC 1.21+
    int maxVersion() default 99999;
}
```

### Version Protocol Map (Fabric — Mojmap)
| MC | Protocol | Notes |
|----|----------|-------|
| 1.21 | 767 | Minimum supported |
| 1.21.1 | 767 | |
| 1.21.2/1.21.3 | 768 | |
| 1.21.4 | 769 | |
| 1.21.5+ | 770+ | Latest |

---

## §8 PLATFORM COMPATIBILITY

| Platform | Supported |
|----------|-----------|
| Fabric 1.21.5+ | Yes (primary target) |
| Java 17+ | Yes |
| Fabric Loader 0.16.x+ | Yes |
| Fabric API 0.119.2+ | Yes |
| Mojmap mappings | Yes |
| ViaVersion | No (Fabric has native version support) |
| Geyser/Floodgate | Optional (mod detection via FabricLoader) |
| Folia | No (Fabric has its own threading model) |
| Purpur | No |
| WorldGuard | No (stub only) |

---

## §9 BUILD & DEPLOY

### Build Command
```bash
./gradlew build
```

### Output
- `build/libs/windfall-fabric-1.0.0.jar`

### Install
- Place jar in `mods/` directory
- Requires Fabric Loader and Fabric API
- Server start command: `"C:\path\to\java.exe" -Xmx2G -Xms512M -jar fabric-server-launch.jar nogui`

---

## §10 CONFIGURATION

Config file: `config/windfall.json`

### Sections
- `alerts`: In-game chat notifications
- `discord`: Webhook integration
- `severity`: VL escalation tiers
- `punishments`: warn → kick → tempban → permban
- `adaptive`: TPS-aware tolerance scaling
- `prometheus`: Metrics endpoint
- `checks`: Per-check enable/disable, maxVl, decay, punishable

### Commands
- `/windfall help` — Show help
- `/windfall reload` — Reload config
- `/windfall toggle <check>` — Enable/disable a check
- `/windfall alerts` — Toggle alert delivery

---

## §11 DEPENDENCIES

### Runtime
- Fabric Loader 0.16.x
- Fabric API 0.119.2+
- Minecraft 1.21.5
- Gson 2.11.0 (bundled with MC)

### Compile
- Fabric Loom 1.9
- Yarn mappings 1.21.5+build.1 (or Mojmap)

### Test
- JUnit 5 (NO Mockito — Unsafe + reflection only)

---

## §12 KEY FILE PATHS

| File | Purpose |
|------|---------|
| `build.gradle` | Gradle build config, dependencies, Fabric Loom |
| `gradle.properties` | Fabric metadata (mod_id, version, MC version) |
| `src/main/resources/fabric.mod.json` | Fabric mod descriptor |
| `src/main/resources/windfall-fabric.mixins.json` | Mixin configuration |
| `src/main/resources/config/windfall.json` | Default configuration |
| `src/main/java/io/windfall/anticheat/WindfallMod.java` | Entry point |
| `src/main/java/io/windfall/anticheat/core/check/CheckManager.java` | Check registry + dispatch |
| `src/main/java/io/windfall/anticheat/mixin/` | Packet interception Mixins |
| `MEMORYBANK_F.md` | This file — local only, excluded from git |

---

## §13 VERSIONING POLICY

- **MAJOR** (X.0.0): Breaking API changes, config format changes
- **MINOR** (0.X.0): New checks, new features, new subsystems
- **PATCH** (0.0.X): Bug fixes, thread safety, threshold tuning
- **NO BUMP**: Trivial changes (comments, formatting, documentation)
- **Commit format**: `V:x.y.z + Summary`

### Decision flowchart
```
Is it a breaking change?
  → YES → MAJOR

Is it a new feature, new check, or significant enhancement?
  → YES → MINOR

Is it a bug fix, dependency update, or meaningful improvement?
  → YES → PATCH

Is it trivial (typo, format, docs-only, small test)?
  → YES → NO BUMP
```

---

## §14 STATE STORAGE — What's Kept in Memory

### Per-Player State (WindfallPlayer — lives until disconnect)
| State | Type | Thread Safe | Purpose |
|-------|------|-------------|---------|
| `position` / `prevPosition` / `lastSafePosition` | `volatile PositionState` | ✅ Atomic snapshot | Position tracking, setback target |
| `serverVelocityX/Y/Z` | `volatile double` | ✅ Volatile | Last server-sent velocity |
| `onGround` / `prevOnGround` | `volatile GroundState` | ✅ Atomic snapshot | Ground state tracking |
| `yaw` / `pitch` / `prevYaw` / `prevPitch` | `volatile RotationState` | ✅ Atomic snapshot | Rotation tracking |
| `protocolVersion` | `int` | Immutable | Player's MC protocol version |
| `isBedrock` | `boolean` | Immutable | Geyser/Floodgate detection |
| `pose` | `Pose` enum | ✅ Volatile | STANDING/SNEAKING/FALL_FLYING/etc. |
| `sneaking` / `swimming` / `gliding` | `boolean` | Synced with pose | Legacy boolean setters |
| `gliding` / `elytraMomentum` / `glideStartTick` | `boolean/int/int` | ✅ Volatile | Elytra state tracking |
| `respawned` | `boolean` | ✅ Volatile | Skip checks after respawn |
| `velocityReceived` | `boolean` | ✅ Volatile | Velocity packet received this tick |
| `movedSinceTick` | `boolean` | ✅ Volatile | Position packet received this tick |
| `lastAttackTime` | `long` | ✅ Volatile | Combat timing |
| `lastInteractTime` | `long` | ✅ Volatile | Interaction timing |
| `actionData` | `ActionData` | ✅ ConcurrentHashMap | Block break/place/piston tracking |
| `violationLevels` | `ConcurrentHashMap<String, Double>` | ✅ ConcurrentHashMap | Per-check VL tracking |
| `transactionPing` | `short` | ✅ Volatile | Current ping (from keepalive) |
| `transactionPingMap` | `ConcurrentHashMap<Short, Long>` | ✅ ConcurrentHashMap | Pending transaction timestamps |

### Per-Check State (lives until removePlayer())
| Check | State Type | Cleanup |
|-------|-----------|---------|
| `VelocityCheck` | `ConcurrentLinkedDeque<VelocitySnapshot>` per player | `removePlayer()` — clear deque |
| `MacroCheck` | `ConcurrentHashMap<UUID, List<Long>>` per player | `removePlayer()` — remove entry |
| `PacketOrderCheck` | `ConcurrentLinkedDeque<PacketTypeCommon>` per player | `removePlayer()` — clear deque |
| `NoSlowCheck` | `boolean usingItem` + `int usingItemTick` per player | `removePlayer()` — reset |
| `ReachCheck` | `ConcurrentHashMap<Integer, EntitySnapshot>` tracked entities | `cleanup()` every 200 ticks — evict stale |
| `BacktrackCheck` | `List<EntitySnapshot>` per player | `removePlayer()` — clear list |
| `HitboxesCheck` | `List<EntitySnapshot>` per player | `removePlayer()` — clear list |
| `InventoryCheck` | `InventoryState` per player | `removePlayer()` — reset |
| `TimerCheck` | `int tickCounter` per player | `removePlayer()` — reset |

### Global Caches (plugin lifecycle)
| Cache | Type | Size Limit | Eviction |
|-------|------|-----------|----------|
| `PlayerManager.players` | `ConcurrentHashMap<UUID, WindfallPlayer>` | Unlimited (players) | `removePlayer()` on quit |
| `MaterialUtils.isFluidCache` | `ConcurrentHashMap<Material, Boolean>` | ~760 (enum size) | `clearCaches()` on reload |
| `CompensatedWorld.blockCache` | `ConcurrentHashMap<Long, MaterialData>` | Per-chunk | Chunk unload |
| `CompensatedEntities.entityCache` | `ConcurrentHashMap<Integer, EntitySnapshot>` | Per-entity | 40-tick expiry |
| `DiscordWebhook.cooldownMap` | `ConcurrentHashMap<String, Long>` | Per-alert-type | 5-minute stale eviction |
| `TransactionManager.transactionMap` | `ConcurrentHashMap<Short, Long>` | Per-player | Transaction response |

### Data Lifecycle
```
PlayerJoinEvent
  → WindfallPlayer created (empty state)
  → PlayerManager.add(uuid, wp)
  → CheckManager creates per-check state

Per-Tick (50ms)
  → ServerTickEvents.END_SERVER_TICK → CheckManager.onTick()
  → player.resetTickState() — cleared every tick
  → check.reward(player) — VL decay + buffer decay
  → ActionData.tick() — block tracking expiry

Per-Packet
  → Fabric Mixins → PacketListener.onPacketReceive/Send
  → Update WindfallPlayer state (position, rotation, velocity, etc.)
  → CheckManager dispatches to all enabled checks
  → Checks may flag (VL increment) or reward (VL decay)

PlayerDisconnectEvent
  → PlayerManager.remove(uuid)
  → CheckManager.removePlayer(uuid) — clears all per-check state
  → TransactionManager.onPlayerQuit() — clears transaction map
```

### Thread Safety Model
- **Immutable snapshots** for position/ground/rotation state — published via `volatile` references
- **ConcurrentHashMap** for all per-player maps — lock-free, thread-safe
- **Volatile fields** for simple booleans/ints — atomic reads/writes
- **No synchronized blocks** — avoids deadlocks and contention
- **No locks** — prefer atomic operations and volatile over locks
- **One writer per field** — only one thread writes to a given field at a time

---

## §15 PERFORMANCE NOTES

### Check Performance Budget
- **Per-tick budget**: ~2ms for 50 players (40 checks each) — must complete before next tick
- **Per-packet budget**: ~0.1ms — must not block Mixin thread
- **Memory per player**: ~2KB (WindfallPlayer state + per-check states)
- **Memory per server**: ~100KB for 50 players

### Expensive Operations to Avoid
- `Material.name().contains()` — string-based material checks. Use `MaterialUtils` cached lookups instead.
- `player.getBukkitPlayer()` in packet handlers — hits main thread. Cache on WindfallPlayer.
- Synchronized blocks — avoid entirely. Use volatile + ConcurrentHashMap.
- Reflection in hot paths — cache reflection results in static fields.

### Optimization Patterns
- **MaterialUtils caches**: ConcurrentHashMap per material type, ~760 entries max (enum size)
- **CompensatedWorld**: Per-chunk block cache, evicted on chunk unload
- **CompensatedEntities**: 40-tick entity position buffer, stale entries evicted every 200 ticks
- **ActionData**: Block tracking with expiry — old entries cleaned up per tick
- **TransactionManager**: Transaction timestamps cleaned up per-player on quit

---

## §16 TROUBLESHOOTING — Common Issues & Fixes

| Symptom | Cause | Fix |
|---------|-------|-----|
| Build fails with Java 26 | Fabric Mixin incompatible with Java 26 | Use Java 21 for server runtime (`C:\Users\Enis Polat\.jdks\jdk-21.0.11+10`) |
| `ClassCastException` in Mixin | Mixin target class not found | Verify `windfall-fabric.mixins.json` includes correct class paths |
| Tests fail with Mockito | Fabric Loom poisons ByteBuddy | Use Unsafe + reflection only (no Mockito) |
| Checks not working | Config file missing or checks disabled | Verify `config/windfall.json` has checks enabled |
| Server crashes on startup | Missing Fabric API | Ensure `fabric-api` mod is installed in `mods/` |
| `NullPointerException` in Check | WindfallPlayer fields not initialized | Initialize `pos`, `ground`, `rotation` via reflection in tests |
| Performance issues | Too many checks enabled | Disable non-essential checks in config, reduce check frequency |
| `UnsupportedClassVersionError` | Wrong Java version | Use Java 17+ for compilation, Java 21 for runtime |
| Thread safety issues | Non-concurrent collections | Use ConcurrentHashMap for per-player state maps |
| Config not reloading | JSON parse error | Check `config/windfall.json` syntax, verify Gson version |

---

## §17 SECURITY PATTERNS — Anti-Cheat Attack Vectors & Defenses

### Why Anti-Cheat Security is Different
Anti-cheat mods are **attack targets** — cheaters actively try to crash, bypass, or exploit them. Unlike typical mods, Windfall processes untrusted client data at the packet level, making input validation and attack surface reduction critical.

### Attack Vectors

| # | Vector | Threat | Windfall Defense |
|---|--------|--------|-----------------|
| 1 | **Packet injection** | Client sends forged/out-of-order packets | `BadPacketsCheck`: NaN/Inf coords → instant kick. `PacketOrderCheck`: duplicate detection (>5), burst detection (>15/100ms), pre-login movement |
| 2 | **Server crash (DoS)** | Oversized payloads, malformed data | `CrashCheck`: chat >32767 chars → kick. `ExploitCheck`: window id >200, entity id >20000, slot >60 → reject. `BadPacketsCheck`: all NaN/Inf values → immediate disconnect |
| 3 | **Command injection** | Chat messages containing server commands | `ChatCheck`: dual sliding window (60/min + 4/2s). Chat processed as DATA, never as COMMAND. `PunishmentEngine`: uses Fabric ban API, never `dispatchCommand()` |
| 4 | **Config manipulation** | File edit exploits, JSON deserialization | Config loaded once via Gson. No user-controlled YAML parsing. `WindfallConfig` is server-side only. |
| 5 | **Memory exhaustion** | Sending rapid packets to grow state | `ConcurrentHashMap` bounded by player count (auto-cleanup on quit). `ReachCheck`: entity cache evicted every 200 ticks. `ActionData`: block tracking expired per tick. |
| 6 | **Thread starvation** | Overwhelming Mixin thread with expensive checks | Per-packet budget ~0.1ms. `CheckManager` catch blocks log at FINE (not SEVERE) to avoid console spam. Expensive operations deferred to tick thread. |
| 7 | **Bypass tool evasion** | Client hides cheat packets, sends legitimate-looking data | **5-layer defense**: (1) Single check detection, (2) Cross-check correlation (aim + reach + velocity), (3) Latency compensation (PingPongManager dual-ping), (4) Multi-scenario simulation (SimulationEngine), (5) Deferred world changes (LatencyCompensator) |
| 8 | **False positive abuse** | Legitimate players flagged, admin trust eroded | Decay system (auto-recovery), severity scaling (1x-2x cumulative VL), per-check max-vl, version-aware physics |

### Security Checklist for New Checks

When implementing any new check, verify:

| # | Check | How to Verify |
|---|-------|---------------|
| 1 | **No `Runtime.exec()`** | Grep for `Runtime.getRuntime().exec()` — NEVER use |
| 2 | **No hardcoded secrets** | Grep for `password`, `secret`, `token`, `key` in string literals |
| 3 | **Input validation** | Every packet field received from client must be bounds-checked before use |
| 4 | **NaN/Inf guard** | Any `double` from client packets must check `Double.isNaN()` / `Double.isInfinite()` |
| 5 | **Thread safety** | Per-check state: ConcurrentHashMap or volatile. Never plain HashMap/ArrayList in concurrent context |
| 6 | **Resource cleanup** | `removePlayer()` must clear ALL per-player state for the check |
| 7 | **Catch specificity** | Never `catch (Exception e)` — catch specific exceptions. Never swallow silently. |
| 8 | **No main thread calls** | Never call heavy API in Mixin callbacks (packet handlers). Cache state on WindfallPlayer. |
| 9 | **Version bounds** | `@CheckData` minVersion/maxVersion must be correct — don't run checks on versions they don't understand |

---

## §18 SEARCH TOOLS & REFERENCE

### Offline Search Tools

#### Codebase Search (grep/glob)
```bash
# Find all check classes
grep -r "extends Check" src/main/java/ --include="*.java"

# Find all @CheckData annotations
grep -r "@CheckData" src/main/java/ --include="*.java"

# Find all Mixin injections
grep -r "@Inject\|@Redirect\|@ModifyVariable" src/main/java/ --include="*.java"

# Find all thread-safety issues (non-concurrent collections)
grep -r "new HashMap\|new ArrayList\|new LinkedList" src/main/java/ --include="*.java"

# Find all catch blocks
grep -r "catch\s*(" src/main/java/ --include="*.java"

# Find all reflection usage
grep -r "Method\.invoke\|getDeclaredMethod\|getDeclaredField" src/main/java/ --include="*.java"

# Count lines of code
find src -name "*.java" -exec cat {} \; | wc -l
```

#### MEMORYBANK Search
```bash
# Search MEMORYBANK for specific topics
grep "Section Name" MEMORYBANK_F.md

# Find all check keys
grep "windfall\." MEMORYBANK_F.md

# Find all version constraints
grep "minVersion\|maxVersion\|protocol" MEMORYBANK_F.md

# Find all known issues
grep "TROUBLESHOOTING\|Known Issue\|False Positive" MEMORYBANK_F.md
```

### Online Search Tools

#### Competitor Analysis
- **Grim AC**: https://github.com/GrimAnticheat/Grim — Java, most active
- **TruthfulAC**: https://github.com/Articdive/TruthfulAC — Java, clean architecture
- **Arrow AC**: https://github.com/Articdive/Arrow — Java, predecessor
- **CloudAC**: https://github.com/Articdive/CloudAC — Go, alternative approach

#### Minecraft Protocol & Wiki
- **MC Protocol wiki**: https://wiki.vg/Protocol — all packet structures
- **MC Wiki**: https://minecraft.wiki/ — vanilla behavior reference
- **Fabric API Javadoc**: https://maven.fabricmc.net/docs/

#### Security & CVE Databases
- **NVD (NIST)**: https://nvd.nist.gov/ — CVE search
- **GitHub Advisory Database**: https://github.com/advisories — dependency vulnerabilities

### Cross-Validation Protocol

When research yields conflicting results:
1. **Trust official docs first** (MC Wiki, Fabric API docs, Mojmap source)
2. **Trust competitor implementations second** (Grim/TruthfulAC are battle-tested)
3. **Trust Stack Overflow last** (often outdated or version-specific)
4. **When in doubt, test locally** — create a minimal reproduction on test server

---

## §19 SESSION HISTORY

### This session (complete chronological):
1. Created Fabric mod project structure from Spigot Windfall v2.3.2
2. Ported all 53 checks from Spigot Windfall to Fabric
3. Ported core framework (Check, CheckManager, WindfallPlayer, Config)
4. Ported physics engine (PredictionEngine, PhysicsConstants, BoundingBox)
5. Ported compensation engine (TransactionManager, PingPongManager, LatencyCompensator)
6. Ported severity/punishment/alert systems
7. Created Fabric Mixins for packet interception (ServerPlayNetworkHandlerMixin, ClientConnectionMixin, PlayerEntityMixin)
8. Created Brigadier command system
9. Fixed 94 Yarn mapping compilation errors (1.21.5+build.1)
10. Added missing package declarations to 14 files
11. Added missing UUID imports to 5 files
12. Rewrote ClientBrandCheck for BrandCustomPayload (1.21.5)
13. Rewrote NoSwingCheck (swing modes removed in 1.21.5)
14. Gutted TransactionCheck (no c2s transaction packet in 1.21.5)
15. Wired Mixin → CheckManager → Checks packet flow
16. Fixed ChatCheck — added `onChatMessage` injection to Mixin
17. Created ClientConnectionMixin for S2C packet interception
18. Fully ported VelocityCheck (S2C Mixin for EntityVelocityUpdateS2CPacket)
19. Implemented InventoryCheck (rapid-click burst detection)
20. Expanded CompatFlag enum (RELAX_ON_MISMATCH, PURPUR_KB_DEPENDENT)
21. Created ModDetector.java (Fabric mod detection via FabricLoader)
22. Created GeyserManager + BedrockInfo for Fabric
23. Removed dead code (empty Mixins, unused imports)
24. Set up test infrastructure (Unsafe + reflection, no Mockito)
25. Created CheckTestBase (real WindfallMod + WindfallConfig via reflection)
26. Created CheckBaseTest (19 tests: metadata, buffer ops, flag/reward/decay)
27. Created PerPlayerStateTest (13 tests: state map types, per-player independence)
28. Fixed Check.decreaseBuffer() ConcurrentHashMap.merge() bug
29. Created BadPacketsDetectionTest (16 tests)
30. Created ChatDetectionTest (7 tests)
31. Created CrashDetectionTest (7 tests)
32. Created NoFallDetectionTest (5 tests)
33. Created CriticalsDetectionTest (10 tests)
34. Fixed NoFallCheck dead-code bug (airborneTicks reset before threshold check)
35. Rewrote FlightCheck (identical if/else branches, hover detection, bypass resistance)
36. Rewrote ReachCheck (AABB closest-point distance, thread-safety fix, AttackerSnapshot)
37. Rewrote HitboxesCheck (hit-ratio analysis, BLATANT_FLAG_THRESHOLD)
38. Rewrote BacktrackCheck (time-gap detection, lastMovementTimestamp)
39. Improved SpeedCheck (soul sand, soul soil, cobweb multipliers, multiplicative tolerance)
40. Removed stale ReachCheck.cleanup() call
41. Java 26 → Java 21 migration (Eclipse Temurin JDK 21.0.11+10)
42. Boot test PASSED (53/53 checks registered, 64ms startup)
43. Created README.md with same banner + Turkish flag as Spigot edition
44. Pushed to GitHub (commit 766e2c3)
45. Ported MEMORYBANK.md from Spigot to Fabric (this file)

---

*Last updated: 2026-07-16*
*Current version: 1.0.0*
*Total checks: 53*
*Total tests: 76 (all passing)*
*Boot test: PASSED (Java 21 + MC 1.21.5)*
