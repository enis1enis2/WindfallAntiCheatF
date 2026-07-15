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
├── WindfallMetrics.java          # Metrics (stub)
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
│   │   ├── type/PacketCheck.java
│   │   └── impl/
│   │       ├── combat/           # 12 combat checks
│   │       ├── movement/         # 29 movement checks
│   │       ├── packet/           # 10 packet checks
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
    ├── ServerPlayNetworkHandlerMixin.java
    ├── ServerPlayerEntityMixin.java
    ├── PlayerEntityMixin.java
    └── MinecraftServerMixin.java
```

---

## §4 Test Coverage

- **Target**: Unit tests for all 53 checks
- **Framework**: JUnit 5 + Mockito
- **Status**: Tests to be written (not yet ported)

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

### Packet (10)
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
- Ensure Java 17+ is installed
- Run `./gradlew clean build` to rebuild from scratch
- Check Fabric Loader version compatibility

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

*Last updated: 2025-07-15*
*Current version: 1.0.0*
*Total checks: 53*
