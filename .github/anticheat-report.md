# Windfall Fabric Anti-Cheat Monitor Report

**Generated:** 2026-10-29 04:24:03 UTC

---

## Windfall Fabric Current Checks

**Total: 55 checks**

### Combat
- `windfall.combat.multiinteract` — Multi Interact A
- `windfall.combat.backtrack` — Backtrack A
- `windfall.combat.autoclicker` — Autoclicker A
- `windfall.combat.hitboxes` — Hitboxes A
- `windfall.combat.aim` — Aim A
- `windfall.combat.fastheal` — Fast Heal A
- `windfall.combat.swordblock` — Sword Block A
- `windfall.combat.killaura` — Kill Aura A
- `windfall.combat.selfinteract` — Self Interact A
- `windfall.combat.macro` — Macro A
- `windfall.combat.reach` — Reach A
- `windfall.combat.criticals` — Criticals A

### Movement
- `windfall.movement.positionplace` — Position Place
- `windfall.movement.speed` — Speed A
- `windfall.movement.multiplace` — Multi Place
- `windfall.movement.elytra` — Elytra A
- `windfall.movement.simulation` — Simulation A
- `windfall.movement.positionbreak` — Position Break
- `windfall.movement.rotationbreak` — Rotation Break A
- `windfall.movement.step` — Step A
- `windfall.movement.noswing` — No Swing A
- `windfall.movement.groundspoof` — Ground Spoof A
- `windfall.movement.timer` — Timer A
- `windfall.movement.airliquidbreak` — Air Liquid Break
- `windfall.movement.motion` — Motion A
- `windfall.movement.invalidplace` — InvalidPlace A
- `windfall.movement.fly` — Flight A
- `windfall.movement.farplace` — FarPlace A
- `windfall.movement.noslow` — NoSlow A
- `windfall.movement.gravity` — Gravity A
- `windfall.movement.airliquidplace` — Air Liquid Place
- `windfall.movement.wrongbreak` — Wrong Break
- `windfall.movement.nofall` — NoFall A
- `windfall.movement.farbreak` — FarBreak A
- `windfall.movement.rotationplace` — Rotation Place
- `windfall.movement.illegalmove` — IllegalMove A
- `windfall.movement.baritone` — Baritone A
- `windfall.movement.multibreak` — Multi Break
- `windfall.movement.velocity` — Velocity A
- `windfall.movement.phase` — Phase A
- `windfall.movement.scaffold` — Scaffold A
- `windfall.movement.invalidbreak` — InvalidBreak A
- `windfall.movement.fastbreak` — FastBreak A

### Packet
- `windfall.packet.clientbrand` — Client Brand A
- `windfall.packet.cheststealer` — Chest Stealer A
- `windfall.packet.transaction` — Transaction A
- `windfall.packet.chat` — Chat A
- `windfall.packet.sprint` — Sprint A
- `windfall.packet.bad` — Bad Packets A
- `windfall.packet.vehicle` — Vehicle A
- `windfall.packet.order` — Packet Order A
- `windfall.packet.creative` — Creative A
- `windfall.packet.exploit` — Exploit A
- `windfall.packet.crash` — Crash A

### Inventory
- `windfall.inventory.inventory` — Inventory A

---

## Competitor Analysis

### Grim

**Missing from Windfall Fabric (3 checks):**

- `movement` **InvalidPlaceFace** → `windfall.movement.invalid place face`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/InvalidPlaceFace.java`
- `movement` **InvalidPlaceCursor** → `windfall.movement.invalid place cursor`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/InvalidPlaceCursor.java`
- `combat` **InvalidInteractCursor** → `windfall.combat.invalid interact cursor`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/combat/InvalidInteractCursor.java`
  - `scale = (float) packetEntity.getAttributeValue(Attributes.SCALE)`

**Matched with existing Windfall Fabric checks:**

- `NoFall` → `NoFall A`
- `InvalidBreak` → `InvalidBreak A`
- `RotationBreak` → `Rotation Break A`
- `FarBreak` → `FarBreak A`
- `NoSwingBreak` → `No Swing A`
- `FastBreak` → `FastBreak A`
- `PositionBreakB` → `Position Break`
- `AirLiquidBreak` → `Air Liquid Break`
- `MultiBreak` → `Multi Break`
- `PositionBreakA` → `Position Break`
- `WrongBreak` → `Wrong Break`
- `MultiActionsE` → `Multi Interact A`
- `MultiActionsG` → `Multi Interact A`
- `MultiActionsB` → `Multi Interact A`
- `MultiActionsC` → `Multi Interact A`
- `MultiActionsD` → `Multi Interact A`
- `MultiActionsA` → `Multi Interact A`
- `MultiActionsF` → `Multi Interact A`
- `ChatC` → `Chat A`
- `ChatA` → `Chat A`
- `ChatD` → `Chat A`
- `ChatB` → `Chat A`
- `MultiPlace` → `Multi Place`
- `PositionPlace` → `Position Place`
- `FarPlace` → `FarPlace A`
- `AirLiquidPlace` → `Air Liquid Place`
- `FabricatedPlace` → `InvalidPlace A`
- `DuplicateRotPlace` → `Rotation Place`
- `RotationPlace` → `Rotation Place`
- `Reach` → `Reach A`
- `SelfInteract` → `Self Interact A`
- `MultiInteractA` → `Multi Interact A`
- `MultiInteractB` → `Multi Interact A`
- `ElytraD` → `Elytra A`
- `ElytraH` → `Elytra A`
- `ElytraE` → `Elytra A`
- `ElytraI` → `Elytra A`
- `ElytraG` → `Elytra A`
- `ElytraC` → `Elytra A`
- `ElytraF` → `Elytra A`
- `ElytraA` → `Elytra A`
- `ElytraB` → `Elytra A`
- `NoSlow` → `NoSlow A`
- `CrashA` → `Crash A`
- `CrashH` → `Crash A`
- `CrashE` → `Crash A`
- `CrashD` → `Crash A`
- `CrashF` → `Crash A`
- `CrashC` → `Crash A`
- `CrashI` → `Crash A`
- `CrashG` → `Crash A`
- `CrashB` → `Crash A`
- `AimDuplicateLook` → `Aim A`
- `AimModulo360` → `Aim A`
- `ExploitB` → `Exploit A`
- `ExploitA` → `Exploit A`
- `GroundSpoof` → `Ground Spoof A`
- `Phase` → `Phase A`
- `PacketOrderE` → `Packet Order A`
- `PacketOrderI` → `Packet Order A`
- `PacketOrderL` → `Packet Order A`
- `PacketOrderP` → `Packet Order A`
- `PacketOrderJ` → `Packet Order A`
- `PacketOrderD` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `PacketOrderN` → `Packet Order A`
- `PacketOrderK` → `Packet Order A`
- `PacketOrderF` → `Packet Order A`
- `PacketOrderG` → `Packet Order A`
- `PacketOrderA` → `Packet Order A`
- `PacketOrderC` → `Packet Order A`
- `PacketOrderO` → `Packet Order A`
- `PacketOrderM` → `Packet Order A`
- `PacketOrderH` → `Packet Order A`
- `Post` → `No Swing A`
- `ClientBrand` → `Client Brand A`
- `VehicleA` → `Vehicle A`
- `VehicleF` → `Vehicle A`
- `VehicleE` → `Vehicle A`
- `VehicleD` → `Vehicle A`
- `VehicleB` → `Vehicle A`
- `FlightA` → `Flight A`
- `SprintD` → `Sprint A`
- `SprintC` → `Sprint A`
- `SprintG` → `Sprint A`
- `SprintA` → `Sprint A`
- `SprintB` → `Sprint A`
- `SprintE` → `Sprint A`
- `SprintF` → `Sprint A`
- `TimerLimit` → `Timer A`
- `Timer` → `Timer A`
- `NegativeTimer` → `Timer A`
- `VehicleTimer` → `Timer A`
- `TickTimer` → `Timer A`
- `Baritone` → `Baritone A`

### TruthfulAC

**Missing from Windfall Fabric (25 checks):**

- `movement` **ScaffoldRotation** → `windfall.movement.scaffold rotation`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/world/scaffold/ScaffoldRotation.java`
  - `EXPANSION = 0.10D`
  - `FLAG_BUFFER = 6.0D`
  - `targetX = clicked.getX() + face.getModX()`
- `movement` **ScaffoldReach** → `windfall.movement.scaffold reach`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/world/scaffold/ScaffoldReach.java`
  - `JITTER = 0.25D`
  - `FLAG_BUFFER = 4.0D`
  - `faceX = clicked.getX() + 0.5D + face.getModX() * 0.5D`
- `movement` **ScaffoldMotion** → `windfall.movement.scaffold motion`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/world/scaffold/ScaffoldMotion.java`
  - `HISTORY_TICKS = 24`
  - `WINDOW_TICKS = 16`
  - `FLAG_BUFFER = 4.0D`
- `movement` **ScaffoldFace** → `windfall.movement.scaffold face`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/world/scaffold/ScaffoldFace.java`
  - `FLAG_BUFFER = 5.0D`
  - `REPEAT_TICKS = 20`
  - `clickedId = data.getWorldCache().getBlockId(clicked.getX(), clicked.getY(), clicked.getZ())`
- `combat` **LagA** → `windfall.combat.lag a`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/lag/LagA.java`
  - `MAX_ATTACK_AGE_TICKS = 40`
  - `BACKTRACK_TOLERANCE = 0.45D`
  - `HARD_CEILING = 7.5D`
- `combat` **LagB** → `windfall.combat.lag b`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/lag/LagB.java`
  - `MIN_REACH_TO_TRIGGER = 3.1D`
  - `WINDOW_MS = 8000`
  - `MIN_DESYNC_HITS = 3`
- `combat` **LagC** → `windfall.combat.lag c`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/lag/LagC.java`
  - `MIN_SAMPLES = 20`
  - `MIN_CORRELATION = 0.85D`
  - `PING_BIN_COUNT = 4`
- `packet` **BadPacket5** → `windfall.packet.bad packet5`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacket5.java`
- `packet` **BadPacketF** → `windfall.packet.bad packet f`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketF.java`
- `packet` **BadPacketO** → `windfall.packet.bad packet o`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketO.java`
- `packet` **BadPacketU** → `windfall.packet.bad packet u`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketU.java`
- `packet` **BadPacketT** → `windfall.packet.bad packet t`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketT.java`
  - `WINDOW_MS = 10_000L`
  - `SERVER_WINDOW_MS = 15_000L`
  - `GRACE = 3`
- `packet` **BadPacketQ** → `windfall.packet.bad packet q`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketQ.java`
- `packet` **BadPacket8** → `windfall.packet.bad packet8`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacket8.java`
  - `MAX_DIG_DIST = 6.0D`
- `packet` **BadPacketR** → `windfall.packet.bad packet r`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketR.java`
  - `POST_WINDOW_MS = 55L`
- `packet` **BadPacket6** → `windfall.packet.bad packet6`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacket6.java`
- `packet` **BadPacketN** → `windfall.packet.bad packet n`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketN.java`
  - `POST_WINDOW_MS = 55L`
- `packet` **BadPacketV** → `windfall.packet.bad packet v`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketV.java`
- `packet` **BadPacketY** → `windfall.packet.bad packet y`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketY.java`
- `packet` **BadPacketL** → `windfall.packet.bad packet l`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketL.java`
- `packet` **BadPacketM** → `windfall.packet.bad packet m`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketM.java`
  - `POST_WINDOW_MS = 55L`
- `packet` **BadPacketB** → `windfall.packet.bad packet b`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketB.java`
  - `MAX_FLYING_STREAK = 20`
- `packet` **BadPacketP** → `windfall.packet.bad packet p`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketP.java`
  - `POST_WINDOW_MS = 80L`
- `packet` **BadPacketZ** → `windfall.packet.bad packet z`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketZ.java`
- `movement` **BFlyA** → `windfall.movement.bfly a`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/bedrock/BFlyA.java`

**Matched with existing Windfall Fabric checks:**

- `FastBreakA` → `FastBreak A`
- `PhaseA` → `Phase A`
- `RaycastA` → `Reach A`
- `ReachA` → `Reach A`
- `KillAuraG` → `Kill Aura A`
- `KillAuraC` → `Kill Aura A`
- `KillAuraE` → `Kill Aura A`
- `KillAuraH` → `Kill Aura A`
- `KillAuraF` → `Kill Aura A`
- `KillAuraB` → `Kill Aura A`
- `KillAuraD` → `Kill Aura A`
- `HitboxA` → `Hitboxes A`
- `AutoClickerC` → `Autoclicker A`
- `AutoClickerA` → `Autoclicker A`
- `AutoClickerD` → `Autoclicker A`
- `AutoClickerE` → `Autoclicker A`
- `AutoClickerB` → `Autoclicker A`
- `AnchorAuraA` → `Kill Aura A`
- `AimB` → `Aim A`
- `AimA` → `Aim A`
- `AimC` → `Aim A`
- `CrystalAuraA` → `Kill Aura A`
- `BadPacketI` → `Bad Packets A`
- `BadPacketG` → `Bad Packets A`
- `BadPacketD` → `Bad Packets A`
- `BadPacketE` → `Bad Packets A`
- `BadPacketA` → `Bad Packets A`
- `BadPacketK` → `Bad Packets A`
- `BadPacketJ` → `Bad Packets A`
- `BadPacketH` → `Bad Packets A`
- `CrasherA` → `Crash A`
- `PacketOrderE` → `Packet Order A`
- `PacketOrderD` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `PacketOrderA` → `Packet Order A`
- `PacketOrderC` → `Packet Order A`
- `InvalidA` → `InvalidPlace A`
- `SprintA` → `Sprint A`
- `SprintB` → `Sprint A`
- `TimerA` → `Timer A`
- `MovementCheckSupport` → `Phase A`
- `VelocityB` → `Velocity A`
- `VelocityD` → `Velocity A`
- `VelocityC` → `Velocity A`
- `VelocityA` → `Velocity A`
- `GroundSpoofB` → `Ground Spoof A`
- `GroundSpoofD` → `Ground Spoof A`
- `GroundSpoofG` → `Ground Spoof A`
- `GroundSpoofC` → `Ground Spoof A`
- `GroundSpoofF` → `Ground Spoof A`
- `GroundSpoofE` → `Ground Spoof A`
- `Simulation` → `Simulation A`
- `InventoryA` → `Inventory A`
- `BaritoneB` → `Baritone A`
- `BaritoneC` → `Baritone A`
- `BaritoneA` → `Baritone A`
- `BSpeedA` → `Speed A`
- `BReachA` → `Reach A`

### Arrow

**Missing from Windfall Fabric (4 checks):**

- `movement` **Movement** → `windfall.movement.movement`
  - Source: `src/main/java/me/arrow/checks/impl/simulation/Movement.java`
- `combat` **AimH2** → `windfall.combat.aim h2`
  - Source: `src/main/java/me/arrow/checks/impl/combat/aimassist/AimH2.java`
- `combat` **LinearRegression** → `windfall.combat.linear regression`
  - Source: `src/main/java/me/arrow/checks/impl/combat/aimassist/aimassistUtil/LinearRegression.java`
  - `n = x.length`
  - `xbar = sumx / n`
  - `ybar = sumy / n`
- `packet` **Suspicion** → `windfall.packet.suspicion`
  - Source: `src/main/java/me/arrow/checks/impl/suspicion/Suspicion.java`
  - `WINDOW_SIZE = 30`
  - `EVAL_INTERVAL = 10`
  - `NUM_FEATURES = 15`

**Matched with existing Windfall Fabric checks:**

- `BackTrackA` → `Backtrack A`
- `BackTrackB` → `Backtrack A`
- `VelocityB` → `Velocity A`
- `VelocityA` → `Velocity A`
- `ReachA` → `Reach A`
- `KillauraA` → `Kill Aura A`
- `HitboxA` → `Hitboxes A`
- `AutoClickerH` → `Autoclicker A`
- `AutoClickerF` → `Autoclicker A`
- `MacroA` → `Macro A`
- `AutoClickerC` → `Autoclicker A`
- `MacroB` → `Macro A`
- `AutoClickerD` → `Autoclicker A`
- `AutoClickerB` → `Autoclicker A`
- `AutoClickerG` → `Autoclicker A`
- `AimH` → `Aim A`
- `AimE` → `Aim A`
- `AimB` → `Aim A`
- `AimA` → `Aim A`
- `AimF` → `Aim A`
- `AimG` → `Aim A`
- `AimC` → `Aim A`
- `AimD` → `Aim A`
- `IllegalMoveC` → `IllegalMove A`
- `IllegalMoveB` → `IllegalMove A`
- `SpeedA` → `Speed A`
- `OmniSprintA` → `Sprint A`
- `SpeedB` → `Speed A`
- `GroundA` → `Ground Spoof A`
- `GroundB` → `Ground Spoof A`
- `GroundC` → `Ground Spoof A`
- `MotionD` → `Motion A`
- `MotionA` → `Motion A`
- `MotionB` → `Motion A`
- `MotionE` → `Motion A`
- `FlyA` → `Flight A`
- `FlyB` → `Flight A`
- `GravityD` → `Gravity A`
- `GravityC` → `Gravity A`
- `GravityA` → `Gravity A`
- `ElytraA` → `Elytra A`
- `GravityB` → `Gravity A`
- `ElytraB` → `Elytra A`
- `ScaffoldB` → `Scaffold A`
- `ScaffoldD` → `Scaffold A`
- `ScaffoldA` → `Scaffold A`
- `ScaffoldC` → `Scaffold A`
- `FastBreakA` → `FastBreak A`
- `InteractD` → `Reach A`
- `InteractA` → `Multi Interact A`
- `InteractC` → `Multi Interact A`
- `FastBreakB` → `FastBreak A`
- `InventoryA` → `Inventory A`
- `ExploitB` → `Exploit A`
- `ExploitA` → `Exploit A`
- `VehicleC` → `Vehicle A`
- `VehicleA` → `Vehicle A`
- `VehicleB` → `Vehicle A`
- `TimerB` → `Timer A`
- `TimerA` → `Timer A`
- `TimerC` → `Timer A`

---

## Summary

- Windfall Fabric has **55 checks**
- Found **32 new checks** across competitors that Windfall Fabric doesn't have

## Recommendations

1. Review generated skeleton files in `src/main/java/io/windfall/anticheat/core/check/impl/`
2. Implement detection logic based on competitor reference
3. Tune thresholds and buffer values for each check
4. Register new checks in `CheckManager.java`
5. Add config entries to `config.yml`
6. Test on live server before enabling punishable mode
