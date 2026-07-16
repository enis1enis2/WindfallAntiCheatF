# Windfall AntiCheat F — MEMORYBANK
## Fabric Edition — 1:1 Port from Spigot/Paper Windfall

---

## §0 Goals

### Goal 1 — Best Possible Detection Logic
- Implement anti-cheat checks with minimal false positives and maximum cheat detection
- All 53 checks ported 1:1 from Spigot Windfall to Fabric
- Same physics engine, same compensation engine, same severity system

### Goal 2 — Maximum Compatibility
- Target: MC 1.21.5+ (Fabric only)
- Java 17+
- Fabric Loader 0.16.x+
- Fabric API 0.119.2+
- Mojmap mappings

---

## §1 Project Overview

- **Name**: Windfall AntiCheat F (Fabric Edition)
- **Platform**: Fabric Mod (NOT Bukkit/Spigot/Paper)
- **Base**: 1:1 port from Windfall Spigot (v2.3.2)
- **License**: MIT
- **Author**: Enis

---

## §2 Source Code

- Package: `io.windfall.anticheat`
- Entry point: `WindfallMod.java` (implements `ModInitializer`)
- Config: JSON-based via Gson at `config/windfall.json`
- Commands: Brigadier (Fabric native)
- Networking: Fabric `ServerPlayNetworking` + Mixins

---

## §3 Source Structure

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
└── mixin/
    ├── ServerPlayNetworkHandlerMixin.java  # 11 C2S packet injections
    ├── ClientConnectionMixin.java          # Generic S2C packet hook
    └── PlayerEntityMixin.java              # Sprint/gliding state sync
```

---

## §4 Test Coverage

- **Target**: Unit tests for all 53 checks
- **Framework**: JUnit 5 (NO Mockito — Fabric Loom incompatible)
- **Status**: 76 tests passing (19 CheckBaseTest + 13 PerPlayerStateTest + 44 detection tests)
- **Test infrastructure**: `sun.misc.Unsafe.allocateInstance()` + reflection for all object creation; no mocking at all

### Test files
- `CheckTestBase.java` — base class: creates real `WindfallMod` + `WindfallConfig` via reflection, packet creation helpers, mock player with reflection-based state init
- `CheckBaseTest.java` — 19 tests: metadata, buffer ops, flag/reward/decay, check instantiation, removePlayer
- `PerPlayerStateTest.java` — 13 tests: state map types, per-player independence, removePlayer clears state
- `BadPacketsDetectionTest.java` — 16 tests: NaN, Y bounds, rotation, duplicate, auto-clicker, removePlayer
- `ChatDetectionTest.java` — 7 tests: burst, rate limit, removePlayer
- `CrashDetectionTest.java` — 7 tests: oversized chat, creative buffer state, removePlayer
- `NoFallDetectionTest.java` — 5 tests: fall + ground claim, flying, descent, removePlayer
- `CriticalsDetectionTest.java` — 10 tests: attack on ground/flying/gliding/sneaking, airborne no motion, valid motion, removePlayer

---

## §5 Key Changes from Spigot Version

1. **PacketEvents → Fabric Networking**: `ServerPlayNetworking.registerGlobalReceiver()` + Mixins
2. **Bukkit Player → ServerPlayerEntity**: Direct Fabric entity access
3. **Bukkit World → ServerWorld**: Direct Fabric world access
4. **Bukkit Config → JSON**: Gson-based config at `config/windfall.json`
5. **Bukkit Commands → Brigadier**: Native Fabric command system
6. **Bukkit Events → Fabric Events**: `ServerPlayConnectionEvents`, `ServerTickEvents`
7. **PlatformScheduler → ServerTickEvents**: `END_SERVER_TICK` for tick loop
8. **Geyser/Bedrock → REMOVED**: Not needed in Fabric edition
9. **ViaVersion → REMOVED**: Fabric has native version support
10. **Folia/Purpur → REMOVED**: Fabric has its own threading model
11. **WorldGuard → REMOVED**: Not available on Fabric
12. **bStats → REMOVED**: Can use Fabric Metrics API instead
13. **Prometheus → SIMPLIFIED**: In-memory counters only

---

## §6 All 53 Checks (Ported)

### Combat (12)
1. AimCheck (Aim A) — `windfall.combat.aim`
2. AutoclickerCheck (Autoclicker A) — `windfall.combat.autoclicker`
3. BacktrackCheck (Backtrack A) — `windfall.combat.backtrack`
4. CriticalsCheck (Criticals A) — `windfall.combat.criticals`
5. FastHealCheck (FastHeal A) — `windfall.combat.fastheal`
6. HitboxesCheck (Hitboxes A) — `windfall.combat.hitboxes`
7. KillAuraCheck (KillAura A) — `windfall.combat.killaura`
8. MacroCheck (Macro A) — `windfall.combat.macro`
9. MultiInteractCheck (MultiInteract A) — `windfall.combat.multiinteract`
10. ReachCheck (Reach A) — `windfall.combat.reach`
11. SelfInteractCheck (SelfInteract A) — `windfall.combat.selfinteract`
12. SwordBlockCheck (SwordBlock A) — `windfall.combat.swordblock`

### Movement (29)
13. SpeedCheck — `windfall.movement.speed`
14. FlightCheck — `windfall.movement.fly`
15. VelocityCheck — `windfall.movement.velocity`
16. TimerCheck — `windfall.movement.timer`
17. NoFallCheck — `windfall.movement.nofall`
18. StepCheck — `windfall.movement.step`
19. ScaffoldCheck — `windfall.movement.scaffold`
20. ElytraCheck — `windfall.movement.elytra`
21. BaritoneCheck — `windfall.movement.baritone`
22. GroundSpoofCheck — `windfall.movement.groundspoof`
23. PhaseCheck — `windfall.movement.phase`
24. SimulationCheck — `windfall.movement.simulation`
25. NoSlowCheck — `windfall.movement.noslow`
26. MotionCheck — `windfall.movement.motion`
27. FastBreakCheck — `windfall.movement.fastbreak`
28. FarBreakCheck — `windfall.movement.farbreak`
29. FarPlaceCheck — `windfall.movement.farplace`
30. InvalidBreakCheck — `windfall.movement.invalidbreak`
31. InvalidPlaceCheck — `windfall.movement.invalidplace`
32. NoSwingCheck — `windfall.movement.noswing`
33. RotationBreakCheck — `windfall.movement.rotationbreak`
34. AirLiquidBreakCheck — `windfall.movement.airliquidbreak`
35. WrongBreakCheck — `windfall.movement.wrongbreak`
36. PositionBreakCheck — `windfall.movement.positionbreak`
37. MultiBreakCheck — `windfall.movement.multibreak`
38. AirLiquidPlaceCheck — `windfall.movement.airliquidplace`
39. RotationPlaceCheck — `windfall.movement.rotationplace`
40. PositionPlaceCheck — `windfall.movement.positionplace`
41. MultiPlaceCheck — `windfall.movement.multiplace`

### Packet (11)
42. BadPacketsCheck — `windfall.packet.bad`
43. ChestStealerCheck — `windfall.packet.cheststealer`
44. CreativeCheck — `windfall.packet.creative`
45. PacketOrderCheck — `windfall.packet.order`
46. ChatCheck — `windfall.packet.chat`
47. CrashCheck — `windfall.packet.crash`
48. SprintCheck — `windfall.packet.sprint`
49. ExploitCheck — `windfall.packet.exploit`
50. ClientBrandCheck — `windfall.packet.clientbrand`
51. VehicleCheck — `windfall.packet.vehicle`
52. TransactionCheck — `windfall.packet.transaction`

### Inventory (1)
53. InventoryCheck — `windfall.inventory.move`

---

## §7 Architecture

### Check Lifecycle
1. Construction: reads `@CheckData` annotation, loads config overrides
2. Per-packet: `onPacketReceive`/`onPacketSend` called for every matching packet
3. Per-tick: `reward()` decays buffer and VL for all online players
4. On quit: `removePlayer(UUID)` clears per-player state maps

### Violation System
- `increaseBuffer`/`decreaseBuffer`: accumulate detection confidence
- `flag()`: increment VL, send alerts, evaluate punishment
- `flagWithSetback()`: flag + immediate teleport setback
- `performSetback()`: teleport to last safe position

### Thread Safety
- Immutable inner classes (`PositionState`, `GroundState`, `RotationState`) published via `volatile` references
- `ConcurrentHashMap` for per-player check state
- Cross-thread reads from Mixin callbacks and tick loop

---

## §8 Platform Compatibility

- **Fabric Mod** — requires Fabric Loader 0.16.x+
- **Minecraft 1.21.5** — Mojmap mappings
- **Java 17+** — `var` keyword used (Java 10+)
- **No ViaVersion** — Fabric has native version support
- **No Geyser/Bedrock** — removed in Fabric edition
- **No Folia/Purpur** — Fabric has its own threading model

---

## §9 Build & Deploy

### Build Command
```bash
./gradlew build
```

### Output
- `build/libs/windfall-fabric-1.0.0.jar`

### Install
- Place jar in `mods/` directory
- Requires Fabric Loader and Fabric API

---

## §10 Configuration

Config file: `config/windfall.json`

### Sections
- `alerts`: In-game chat notifications
- `discord`: Webhook integration
- `severity`: VL escalation tiers
- `punishments`: warn → kick → tempban → permban
- `adaptive`: TPS-aware tolerance scaling
- `prometheus`: Metrics endpoint
- `checks`: Per-check enable/disable, maxVl, decay, punishable

---

## §11 Dependencies

### Runtime
- Fabric Loader 0.16.x
- Fabric API 0.119.2+
- Minecraft 1.21.5
- Gson 2.11.0 (bundled)

### Compile
- Fabric Loom 1.9
- Yarn mappings 1.21.5+build.1

---

## §12 Session History

### Item 1 — Initial Fabric Port (2025-07-15)
- Created Fabric mod project structure
- Ported all 53 checks from Spigot Windfall v2.3.2
- Ported core framework (Check, CheckManager, WindfallPlayer, Config)
- Ported physics engine (PredictionEngine, PhysicsConstants, BoundingBox)
- Ported compensation engine (TransactionManager, PingPongManager, LatencyCompensator)
- Ported severity/punishment/alert systems
- Created Fabric Mixins for packet interception
- Created Brigadier command system
- Created MEMORYBANK.md for Fabric edition

### Item 2 — Yarn Mapping Fixes (2025-07-15)
- Fixed all 94 compilation errors caused by Yarn 1.21.5+build.1 mapping changes
- **API changes resolved**:
  - `PlayerActionC2SPacket.getBlockPosition()` → `getPos()`
  - `PlayerInteractBlockC2SPacket.getBlockHit()` → `getBlockHitResult()`
  - `PlayerMoveC2SPacket.hasRotation()` → `changesLook()`
  - `ChatMessageC2SPacket.chat()` → `chatMessage()`
  - `ClickSlotC2SPacket.getSyncId()`/`getSlot()` → `syncId()`/`slot()` (Record class)
  - `CreativeInventoryActionC2SPacket.getSlot()` → `slot()` (Record class)
  - `VehicleMoveC2SPacket.getX()`/`getY()`/`getZ()` → `position().x/y/z` (Record class)
  - `InteractionHand` moved from `net.minecraft.entity` → `net.minecraft.util.Hand`
  - `PlayerInteractEntityC2SPacket.Handler` interface changed: `onInteract(Entity, Hand)` → `interact(Hand)`, `onAttack(Entity, Hand)` → `attack()`, `onInteractAt(Entity, Hand, Vec3d)` → `interactAt(Hand, Vec3d)`
  - `PlayerInteractEntityC2SPacket.getEntityId()` removed → use `getEntity(ServerWorld)`
  - `TransactionConfirmationC2SPacket` removed entirely (1.21.5 uses keepalive)
  - `CustomPayloadC2SPacket.getChannel()`/`getData()` → `payload()` returning `CustomPayload` subtype (`BrandCustomPayload` has `brand()`)
  - `ClientCommandC2SPacket.Mode.PERFORM_MAIN_HAND_SWING`/`PERFORM_OFF_HAND_SWING` removed
- **Package declarations**: Added missing `package` declarations to 14 files (SelfInteractCheck, CriticalsCheck, NoSwingCheck, BaritoneCheck, FarBreakCheck, FastBreakCheck, InvalidBreakCheck, InvalidPlaceCheck, FarPlaceCheck, VelocityCheck, ElytraCheck, MotionCheck, ClientBrandCheck, ExploitCheck, InventoryCheck)
- **UUID imports**: Added missing `java.util.UUID` imports to 5 files
- **Rewrites**: ClientBrandCheck rewritten to use `BrandCustomPayload`; NoSwingCheck rewritten (swing modes removed in 1.21.5); TransactionCheck gutted (no c2s transaction packet in 1.21.5)
- Build status: **SUCCESS** (0 errors, 0 tests yet)

### Item 3 — Mixin Wiring & Check Completion (2025-07-15)
- **Mixin wiring audit** — full trace of packet flow from Mixin → CheckManager → Checks
- **Critical fix: ChatCheck was dead** — added `onChatMessage` injection to `ServerPlayNetworkHandlerMixin`; ChatCheck and CrashCheck's chat branch now receive packets
- **Critical fix: S2C packets never reached checks** — created `ClientConnectionMixin` to intercept outgoing packets via `ClientConnection.send()`; registered in `windfall-fabric.mixins.json`
- **NoSwingCheck rewritten** — now properly tracks `HandSwingC2SPacket` (animation), `PlayerActionC2SPacket` (start dig), and `PlayerInteractBlockC2SPacket` (place); flags after 3 consecutive missing swings
- **VelocityCheck fully ported** — captures `EntityVelocityUpdateS2CPacket` via S2C Mixin, applies post-velocity physics (ground friction, air drag, water drag, climbing slowdown), compares expected vs actual movement with wall-near tolerance
- **InventoryCheck implemented** — ported from Spigot: rapid-click burst detection (50ms window), per-second rate limit (20 clicks/s), creative mode validation
- **CompatFlag enum expanded** — added `RELAX_ON_MISMATCH` and `PURPUR_KB_DEPENDENT`
- **Dead code cleanup** — removed unused `KeepAliveC2SPacket` import; removed empty `ServerPlayerEntityMixin` and `MinecraftServerMixin` from Mixin config
- **Yarn API confirmed**: `Entity.getId()` (not `getEntityId()`), `PlayerInteractBlockC2SPacket.getBlockHitResult()`, `ClientConnection.getPacketListener()`, `ServerPlayerEntity.getGameMode()`
- Build status: **SUCCESS** (0 errors)
- **Check audit**: 45 FULL, 2 PARTIAL (TransactionCheck partial by design), 0 STUB

### Item 4 — Test Infrastructure (2025-07-16)
- **Root problem discovered**: Mockito/ByteBuddy CANNOT instrument ANY class in Fabric Loom's test classpath under Java 26 — the remapped MC jar poisons ByteBuddy's `OpenedClassReader`, causing `IllegalArgumentException` for ALL classes (even plain POJOs like `WindfallPlayer`)
- **Failed approaches**: Adding `--add-opens`; removing `final` from `WindfallMod`; trying inline mock maker — all fail because ByteBuddy cannot read remapped MC class bytecode
- **Solution adopted**: Abandoned Mockito entirely. Uses `sun.misc.Unsafe.allocateInstance()` + reflection for all object creation in tests
- **CheckTestBase rewritten**: creates real `WindfallMod` instance (no-arg constructor), sets static `instance` field via reflection, creates `WindfallConfig(null)` and sets `JsonObject` config field via reflection, creates real `SeverityManager.fromConfig()`
- **WindfallPlayer creation**: `Unsafe.allocateInstance()` bypasses constructor (which requires `ServerPlayerEntity`), sets fields (`uuid`, `name`, `violationLevels`, `buffers`, etc.) via reflection
- **Config mutation**: `setConfigValue(path, value)` helper modifies config JSON at runtime for per-test config overrides
- **Test results**: 32/32 passing (19 CheckBaseTest + 13 PerPlayerStateTest)
- **Removed Mockito dependency** from `build.gradle` entirely
- Build status: **SUCCESS** (32 tests, 0 failures)

### Item 5 — Detection Tests & Bug Fixes (2025-07-16)
- **CheckTestBase fixed**: Unsafe skips field initializers; `pos` (PositionState), `ground` (GroundState), `rotation` (RotationState) must be initialized via reflection or NPEs occur
- **Packet creation helpers added**: `createMovePacket`, `createFullMovePacket`, `createSwingPacket`, `createChatPacket`, `createCreativePacket` (Unsafe-based for CreativeInventoryActionC2SPacket), `createAttackPacket` (Unsafe + ATTACK proxy fallback)
- **Check.decreaseBuffer() bug fixed**: `ConcurrentHashMap.merge()` with default value `-amount` inserted negative buffer when key absent; fixed to use `0.0` default with `(a, b) -> Math.max(0.0, a - amount)`
- **5 detection test files created** (44 test cases total): BadPacketsDetectionTest (16), ChatDetectionTest (7), CrashDetectionTest (7), NoFallDetectionTest (5), CriticalsDetectionTest (10)
- **Test results**: 76/76 passing (32 structural + 44 detection)
- Build status: **SUCCESS** (76 tests, 0 failures)

### Item 6 — Critical Check Logic Fixes (2025-07-16)
- **NoFallCheck dead-code bug fixed**: `airborneTicks` was reset to 0 in else-branch BEFORE the threshold check, making first detection condition unreachable; restructured so check happens before reset
- **FlightCheck rewritten**: Fixed identical if/else branches for predictedDeltaY; added proper `expectedDeltaY` tracking (seeds from jump momentum); added hover detection with proper buffer thresholds; added `CompatFlag.RELAX_ON_MISMATCH`
- **ReachCheck rewritten**: Changed from point-to-center distance (incorrect) to AABB closest-point distance (geometrically correct); fixed thread-safety bug (static HashMap → per-player ConcurrentHashMap); added `CompatFlag.VIAVERSION_SENSITIVE, RELAX_ON_MISMATCH`
- **HitboxesCheck rewritten**: Changed from miss-distance analysis (inverted — would never flag expanded hitboxes) to hit-ratio analysis (Spigot reference); projects look vector to MAX_REACH (3.5), counts on-target hits, flags at >80% ratio; added `CompatFlag.RELAX_ON_MISMATCH`
- **BacktrackCheck rewritten**: Changed from position-freeze detection (only catches frozen+high-ping) to time-gap detection (Spigot reference); tracks `lastMovementTimestamp` on movement packets, flags if attack delay > 500ms; added `CompatFlag.VIAVERSION_SENSITIVE, RELAX_ON_MISMATCH`
- **SpeedCheck improved**: Added soul sand (0.4x), soul soil, and cobweb (0.1x) speed multipliers; changed tolerance from flat additive (0.05) to multiplicative (1.05); added `CompatFlag.RELAX_ON_MISMATCH`
- **CheckManager cleanup**: Removed stale `ReachCheck.cleanup()` call for deleted entity tracking cache
- Build status: **SUCCESS** (76 tests, 0 failures)

### Item 7 — Boot Test & Java 21 Setup (2025-07-16)
- **Java 26 → Java 21 migration**: Java 26 (class file version 70) incompatible with Fabric Mixin 0.8.7; installed Eclipse Temurin JDK 21.0.11+10 to `C:\Users\Enis Polat\.jdks\jdk-21.0.11+10`
- **Boot test PASSED**: Fabric server started with Java 21 on MC 1.21.5
  - All 53/53 checks registered successfully
  - Windfall F v1.0.0 enabled in 64ms
  - Config loaded (Discord webhook, etc.)
  - Server Done in 6.992s
  - World generated in Creative mode
  - Server auto-paused after 60s empty (normal)
- **Java for tests**: Build uses Java 26 (Gradle daemon) for compilation; test server requires Java 21
- **Test server**: Located at `test-server/` with `fabric-server-launch.jar`
- **Server start command**: `"C:\Users\Enis Polat\.jdks\jdk-21.0.11+10\bin\java.exe" -Xmx2G -Xms512M -jar fabric-server-launch.jar nogui`
- Build status: **SUCCESS** (76 tests, 0 failures)

---

## §13 Versioning Policy

- **MAJOR**: Breaking change (incompatible API change, config format change)
- **MINOR**: New feature (new check, new subsystem)
- **PATCH**: Bug fix (FP fix, compilation fix)
- **NO BUMP**: Trivial changes (comments, formatting, documentation)
- **Commit format**: `V:x.y.z + Summary`

---

## §14 Troubleshooting

### Build fails
- Ensure Java 17+ is installed (Java 21 required for test server runtime)
- Run `./gradlew clean build` to rebuild from scratch
- Check Fabric Loader version compatibility
- Java 26+ is NOT compatible with Fabric Mixin — use Java 21 for server runtime

### Checks not working
- Verify `config/windfall.json` has checks enabled
- Check server logs for `[Windfall]` messages
- Ensure Fabric API is installed

### Performance issues
- Reduce check frequency in config
- Disable verbose logging
- Monitor Prometheus metrics endpoint

---

## §15 Search Tools

### Files
- `Glob: **/*.java` — find all Java source files
- `Grep: pattern` — search file contents
- `Read: filePath` — read file contents

### Web
- `WebSearch: query` — search the web
- `WebFetch: url` — fetch URL content

---

*Last updated: 2025-07-16*
*Current version: 1.0.0*
*Total checks: 53*
*Total tests: 76 (all passing)*
*Boot test: PASSED (Java 21 + MC 1.21.5)*
