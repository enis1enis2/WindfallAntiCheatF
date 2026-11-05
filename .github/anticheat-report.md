# Windfall Fabric Anti-Cheat Monitor Report

**Generated:** 2026-11-05 05:08:14 UTC

---

## Windfall Fabric Current Checks

**Total: 55 checks**

### Combat
- `windfall.combat.reach` — Reach A
- `windfall.combat.aim` — Aim A
- `windfall.combat.multiinteract` — Multi Interact A
- `windfall.combat.fastheal` — Fast Heal A
- `windfall.combat.selfinteract` — Self Interact A
- `windfall.combat.macro` — Macro A
- `windfall.combat.swordblock` — Sword Block A
- `windfall.combat.criticals` — Criticals A
- `windfall.combat.backtrack` — Backtrack A
- `windfall.combat.killaura` — Kill Aura A
- `windfall.combat.hitboxes` — Hitboxes A
- `windfall.combat.autoclicker` — Autoclicker A

### Movement
- `windfall.movement.rotationplace` — Rotation Place
- `windfall.movement.multiplace` — Multi Place
- `windfall.movement.phase` — Phase A
- `windfall.movement.groundspoof` — Ground Spoof A
- `windfall.movement.positionbreak` — Position Break
- `windfall.movement.rotationbreak` — Rotation Break A
- `windfall.movement.noswing` — No Swing A
- `windfall.movement.elytra` — Elytra A
- `windfall.movement.invalidbreak` — InvalidBreak A
- `windfall.movement.nofall` — NoFall A
- `windfall.movement.wrongbreak` — Wrong Break
- `windfall.movement.scaffold` — Scaffold A
- `windfall.movement.simulation` — Simulation A
- `windfall.movement.timer` — Timer A
- `windfall.movement.gravity` — Gravity A
- `windfall.movement.noslow` — NoSlow A
- `windfall.movement.airliquidplace` — Air Liquid Place
- `windfall.movement.velocity` — Velocity A
- `windfall.movement.step` — Step A
- `windfall.movement.farbreak` — FarBreak A
- `windfall.movement.multibreak` — Multi Break
- `windfall.movement.speed` — Speed A
- `windfall.movement.fastbreak` — FastBreak A
- `windfall.movement.positionplace` — Position Place
- `windfall.movement.fly` — Flight A
- `windfall.movement.farplace` — FarPlace A
- `windfall.movement.invalidplace` — InvalidPlace A
- `windfall.movement.illegalmove` — IllegalMove A
- `windfall.movement.baritone` — Baritone A
- `windfall.movement.motion` — Motion A
- `windfall.movement.airliquidbreak` — Air Liquid Break

### Packet
- `windfall.packet.sprint` — Sprint A
- `windfall.packet.clientbrand` — Client Brand A
- `windfall.packet.chat` — Chat A
- `windfall.packet.vehicle` — Vehicle A
- `windfall.packet.creative` — Creative A
- `windfall.packet.order` — Packet Order A
- `windfall.packet.cheststealer` — Chest Stealer A
- `windfall.packet.crash` — Crash A
- `windfall.packet.transaction` — Transaction A
- `windfall.packet.exploit` — Exploit A
- `windfall.packet.bad` — Bad Packets A

### Inventory
- `windfall.inventory.inventory` — Inventory A

---

## Competitor Analysis

### Grim

**Missing from Windfall Fabric (3 checks):**

- `combat` **InvalidInteractCursor** → `windfall.combat.invalid interact cursor`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/combat/InvalidInteractCursor.java`
  - `scale = (float) packetEntity.getAttributeValue(Attributes.SCALE)`
- `movement` **InvalidPlaceCursor** → `windfall.movement.invalid place cursor`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/InvalidPlaceCursor.java`
- `movement` **InvalidPlaceFace** → `windfall.movement.invalid place face`
  - Source: `common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/InvalidPlaceFace.java`

**Matched with existing Windfall Fabric checks:**

- `ClientBrand` → `Client Brand A`
- `Post` → `No Swing A`
- `NoSlow` → `NoSlow A`
- `Phase` → `Phase A`
- `GroundSpoof` → `Ground Spoof A`
- `VehicleTimer` → `Timer A`
- `NegativeTimer` → `Timer A`
- `TickTimer` → `Timer A`
- `Timer` → `Timer A`
- `TimerLimit` → `Timer A`
- `SprintB` → `Sprint A`
- `SprintC` → `Sprint A`
- `SprintE` → `Sprint A`
- `SprintF` → `Sprint A`
- `SprintD` → `Sprint A`
- `SprintG` → `Sprint A`
- `SprintA` → `Sprint A`
- `AimModulo360` → `Aim A`
- `AimDuplicateLook` → `Aim A`
- `FlightA` → `Flight A`
- `NoFall` → `NoFall A`
- `MultiInteractA` → `Multi Interact A`
- `Reach` → `Reach A`
- `MultiInteractB` → `Multi Interact A`
- `SelfInteract` → `Self Interact A`
- `NoSwingBreak` → `No Swing A`
- `MultiBreak` → `Multi Break`
- `WrongBreak` → `Wrong Break`
- `FarBreak` → `FarBreak A`
- `PositionBreakA` → `Position Break`
- `FastBreak` → `FastBreak A`
- `InvalidBreak` → `InvalidBreak A`
- `AirLiquidBreak` → `Air Liquid Break`
- `PositionBreakB` → `Position Break`
- `RotationBreak` → `Rotation Break A`
- `ExploitB` → `Exploit A`
- `ExploitA` → `Exploit A`
- `AirLiquidPlace` → `Air Liquid Place`
- `FabricatedPlace` → `InvalidPlace A`
- `FarPlace` → `FarPlace A`
- `RotationPlace` → `Rotation Place`
- `MultiPlace` → `Multi Place`
- `DuplicateRotPlace` → `Rotation Place`
- `PositionPlace` → `Position Place`
- `MultiActionsA` → `Multi Interact A`
- `MultiActionsC` → `Multi Interact A`
- `MultiActionsF` → `Multi Interact A`
- `MultiActionsB` → `Multi Interact A`
- `MultiActionsD` → `Multi Interact A`
- `MultiActionsE` → `Multi Interact A`
- `MultiActionsG` → `Multi Interact A`
- `ChatD` → `Chat A`
- `ChatB` → `Chat A`
- `ChatC` → `Chat A`
- `ChatA` → `Chat A`
- `Baritone` → `Baritone A`
- `VehicleA` → `Vehicle A`
- `VehicleE` → `Vehicle A`
- `VehicleF` → `Vehicle A`
- `VehicleD` → `Vehicle A`
- `VehicleB` → `Vehicle A`
- `ElytraB` → `Elytra A`
- `ElytraE` → `Elytra A`
- `ElytraD` → `Elytra A`
- `ElytraH` → `Elytra A`
- `ElytraG` → `Elytra A`
- `ElytraF` → `Elytra A`
- `ElytraA` → `Elytra A`
- `ElytraI` → `Elytra A`
- `ElytraC` → `Elytra A`
- `PacketOrderI` → `Packet Order A`
- `PacketOrderN` → `Packet Order A`
- `PacketOrderO` → `Packet Order A`
- `PacketOrderJ` → `Packet Order A`
- `PacketOrderA` → `Packet Order A`
- `PacketOrderF` → `Packet Order A`
- `PacketOrderD` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `PacketOrderM` → `Packet Order A`
- `PacketOrderE` → `Packet Order A`
- `PacketOrderH` → `Packet Order A`
- `PacketOrderP` → `Packet Order A`
- `PacketOrderL` → `Packet Order A`
- `PacketOrderC` → `Packet Order A`
- `PacketOrderG` → `Packet Order A`
- `PacketOrderK` → `Packet Order A`
- `CrashA` → `Crash A`
- `CrashD` → `Crash A`
- `CrashH` → `Crash A`
- `CrashE` → `Crash A`
- `CrashG` → `Crash A`
- `CrashF` → `Crash A`
- `CrashI` → `Crash A`
- `CrashC` → `Crash A`
- `CrashB` → `Crash A`

### TruthfulAC

**Missing from Windfall Fabric (25 checks):**

- `packet` **BadPacketU** → `windfall.packet.bad packet u`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketU.java`
- `packet` **BadPacketO** → `windfall.packet.bad packet o`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketO.java`
- `packet` **BadPacketQ** → `windfall.packet.bad packet q`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketQ.java`
- `packet` **BadPacketF** → `windfall.packet.bad packet f`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketF.java`
- `packet` **BadPacketL** → `windfall.packet.bad packet l`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketL.java`
- `packet` **BadPacket5** → `windfall.packet.bad packet5`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacket5.java`
- `packet` **BadPacketM** → `windfall.packet.bad packet m`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketM.java`
  - `POST_WINDOW_MS = 55L`
- `packet` **BadPacketR** → `windfall.packet.bad packet r`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketR.java`
  - `POST_WINDOW_MS = 55L`
- `packet` **BadPacketN** → `windfall.packet.bad packet n`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketN.java`
  - `POST_WINDOW_MS = 55L`
- `packet` **BadPacket6** → `windfall.packet.bad packet6`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacket6.java`
- `packet` **BadPacketP** → `windfall.packet.bad packet p`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketP.java`
  - `POST_WINDOW_MS = 80L`
- `packet` **BadPacket8** → `windfall.packet.bad packet8`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacket8.java`
  - `MAX_DIG_DIST = 6.0D`
- `packet` **BadPacketV** → `windfall.packet.bad packet v`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketV.java`
- `packet` **BadPacketY** → `windfall.packet.bad packet y`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketY.java`
- `packet` **BadPacketZ** → `windfall.packet.bad packet z`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketZ.java`
- `packet` **BadPacketB** → `windfall.packet.bad packet b`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketB.java`
  - `MAX_FLYING_STREAK = 20`
- `packet` **BadPacketT** → `windfall.packet.bad packet t`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/packet/badpacket/BadPacketT.java`
  - `WINDOW_MS = 10_000L`
  - `SERVER_WINDOW_MS = 15_000L`
  - `GRACE = 3`
- `movement` **ScaffoldReach** → `windfall.movement.scaffold reach`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/world/scaffold/ScaffoldReach.java`
  - `JITTER = 0.25D`
  - `FLAG_BUFFER = 4.0D`
  - `faceX = clicked.getX() + 0.5D + face.getModX() * 0.5D`
- `movement` **ScaffoldRotation** → `windfall.movement.scaffold rotation`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/world/scaffold/ScaffoldRotation.java`
  - `EXPANSION = 0.10D`
  - `FLAG_BUFFER = 6.0D`
  - `targetX = clicked.getX() + face.getModX()`
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
- `combat` **LagA** → `windfall.combat.lag a`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/combat/lag/LagA.java`
  - `MAX_ATTACK_AGE_TICKS = 40`
  - `BACKTRACK_TOLERANCE = 0.45D`
  - `HARD_CEILING = 7.5D`
- `movement` **BFlyA** → `windfall.movement.bfly a`
  - Source: `src/main/java/ret/tawny/truthful/checks/impl/bedrock/BFlyA.java`

**Matched with existing Windfall Fabric checks:**

- `MovementCheckSupport` → `Phase A`
- `InventoryA` → `Inventory A`
- `GroundSpoofC` → `Ground Spoof A`
- `GroundSpoofG` → `Ground Spoof A`
- `GroundSpoofB` → `Ground Spoof A`
- `GroundSpoofD` → `Ground Spoof A`
- `GroundSpoofE` → `Ground Spoof A`
- `GroundSpoofF` → `Ground Spoof A`
- `Simulation` → `Simulation A`
- `BaritoneB` → `Baritone A`
- `BaritoneA` → `Baritone A`
- `BaritoneC` → `Baritone A`
- `VelocityD` → `Velocity A`
- `VelocityC` → `Velocity A`
- `VelocityA` → `Velocity A`
- `VelocityB` → `Velocity A`
- `RaycastA` → `Reach A`
- `TimerA` → `Timer A`
- `InvalidA` → `InvalidBreak A`
- `SprintB` → `Sprint A`
- `SprintA` → `Sprint A`
- `BadPacketJ` → `Bad Packets A`
- `BadPacketE` → `Bad Packets A`
- `BadPacketI` → `Bad Packets A`
- `BadPacketH` → `Bad Packets A`
- `BadPacketG` → `Bad Packets A`
- `BadPacketK` → `Bad Packets A`
- `BadPacketA` → `Bad Packets A`
- `BadPacketD` → `Bad Packets A`
- `PacketOrderA` → `Packet Order A`
- `PacketOrderD` → `Packet Order A`
- `PacketOrderB` → `Packet Order A`
- `PacketOrderE` → `Packet Order A`
- `PacketOrderC` → `Packet Order A`
- `CrasherA` → `Crash A`
- `FastBreakA` → `FastBreak A`
- `PhaseA` → `Phase A`
- `AutoClickerD` → `Autoclicker A`
- `AutoClickerE` → `Autoclicker A`
- `AutoClickerA` → `Autoclicker A`
- `AutoClickerB` → `Autoclicker A`
- `AutoClickerC` → `Autoclicker A`
- `HitboxA` → `Hitboxes A`
- `CrystalAuraA` → `Kill Aura A`
- `KillAuraG` → `Kill Aura A`
- `KillAuraE` → `Kill Aura A`
- `KillAuraC` → `Kill Aura A`
- `KillAuraH` → `Kill Aura A`
- `KillAuraB` → `Kill Aura A`
- `KillAuraF` → `Kill Aura A`
- `KillAuraD` → `Kill Aura A`
- `AimC` → `Aim A`
- `AimA` → `Aim A`
- `AimB` → `Aim A`
- `ReachA` → `Reach A`
- `AnchorAuraA` → `Kill Aura A`
- `BReachA` → `Reach A`
- `BSpeedA` → `Speed A`

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

- `InventoryA` → `Inventory A`
- `TimerC` → `Timer A`
- `TimerB` → `Timer A`
- `TimerA` → `Timer A`
- `ScaffoldB` → `Scaffold A`
- `ScaffoldC` → `Scaffold A`
- `ScaffoldA` → `Scaffold A`
- `ScaffoldD` → `Scaffold A`
- `FastBreakA` → `FastBreak A`
- `FastBreakB` → `FastBreak A`
- `InteractA` → `Multi Interact A`
- `InteractD` → `Reach A`
- `InteractC` → `Multi Interact A`
- `ExploitB` → `Exploit A`
- `ExploitA` → `Exploit A`
- `VehicleA` → `Vehicle A`
- `VehicleC` → `Vehicle A`
- `VehicleB` → `Vehicle A`
- `GravityA` → `Gravity A`
- `ElytraB` → `Elytra A`
- `GravityC` → `Gravity A`
- `FlyB` → `Flight A`
- `FlyA` → `Flight A`
- `GravityD` → `Gravity A`
- `ElytraA` → `Elytra A`
- `GravityB` → `Gravity A`
- `MotionA` → `Motion A`
- `MotionD` → `Motion A`
- `MotionE` → `Motion A`
- `MotionB` → `Motion A`
- `IllegalMoveC` → `IllegalMove A`
- `IllegalMoveB` → `IllegalMove A`
- `SpeedB` → `Speed A`
- `OmniSprintA` → `Sprint A`
- `SpeedA` → `Speed A`
- `GroundC` → `Ground Spoof A`
- `GroundA` → `Ground Spoof A`
- `GroundB` → `Ground Spoof A`
- `AutoClickerD` → `Autoclicker A`
- `AutoClickerB` → `Autoclicker A`
- `AutoClickerC` → `Autoclicker A`
- `MacroA` → `Macro A`
- `AutoClickerH` → `Autoclicker A`
- `AutoClickerG` → `Autoclicker A`
- `AutoClickerF` → `Autoclicker A`
- `MacroB` → `Macro A`
- `HitboxA` → `Hitboxes A`
- `KillauraA` → `Kill Aura A`
- `AimC` → `Aim A`
- `AimD` → `Aim A`
- `AimF` → `Aim A`
- `AimE` → `Aim A`
- `AimG` → `Aim A`
- `AimA` → `Aim A`
- `AimB` → `Aim A`
- `AimH` → `Aim A`
- `ReachA` → `Reach A`
- `BackTrackA` → `Backtrack A`
- `BackTrackB` → `Backtrack A`
- `VelocityA` → `Velocity A`
- `VelocityB` → `Velocity A`

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
