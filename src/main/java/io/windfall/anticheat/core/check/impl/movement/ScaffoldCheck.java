package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;

import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Scaffold A", stableKey="windfall.movement.scaffold", decay=0.01, setbackVl=20)
public class ScaffoldCheck extends Check implements PacketCheck {

    private static final int TOWER_TICK_THRESHOLD = 10;
    private static final double SCAFFOLD_PITCH_THRESHOLD = -80.0;
    private static final int ROTATION_PATTERN_MIN = 5;
    private static final double MOVE_SPEED_MIN = 0.1;

    private final ConcurrentHashMap<String, PlayerState> playerStates = new ConcurrentHashMap<>();

    static final class PlayerState {
        int towerTicks;
        int lookDownCount;
        float lastYaw;
        float lastPitch;
        int placeCount;
        int moveWhilePlacing;
        boolean wasMoving;
        long lastPlaceTime;
        PlayerState() {}
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (player.isFlying() || player.isGliding() || player.isCachedIsFallFlying()) return;

        PlayerState state = playerStates.computeIfAbsent(player.getUuid().toString(), k -> new PlayerState());

        if (packet instanceof PlayerInteractBlockC2SPacket) {
            PlayerInteractBlockC2SPacket placePacket = (PlayerInteractBlockC2SPacket) packet;
            BlockHitResult hitResult = placePacket.getBlockHitResult();
            BlockPos blockPos = hitResult.getBlockPos();

            state.placeCount++;
            state.lastPlaceTime = System.currentTimeMillis();

            float pitch = player.getPitch();
            if (pitch < SCAFFOLD_PITCH_THRESHOLD) {
                state.lookDownCount++;
            } else {
                state.lookDownCount = Math.max(0, state.lookDownCount - 1);
            }

            double horizontalSpeed = player.getHorizontalSpeed();
            if (horizontalSpeed > MOVE_SPEED_MIN && !player.isOnGround()) {
                state.moveWhilePlacing++;
            }

            if (state.lookDownCount >= ROTATION_PATTERN_MIN && state.moveWhilePlacing >= 3) {
                increaseBuffer(player, 0.5);
                if (getBuffer(player) > 2.0) {
                    flag(player);
                    resetBuffer(player);
                }
                state.lookDownCount = 0;
                state.moveWhilePlacing = 0;
            }
        }

        if (packet instanceof PlayerMoveC2SPacket) {
            float pitch = player.getPitch();
            float yaw = player.getYaw();
            double horizontalSpeed = player.getHorizontalSpeed();

            if (pitch < SCAFFOLD_PITCH_THRESHOLD) {
                state.lookDownCount++;
            } else {
                state.lookDownCount = Math.max(0, state.lookDownCount - 1);
            }

            if (horizontalSpeed > MOVE_SPEED_MIN) {
                state.wasMoving = true;
            }

            state.lastYaw = yaw;
            state.lastPitch = pitch;
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    public void onTick(WindfallPlayer player, long tickCounter) {
        PlayerState state = playerStates.computeIfAbsent(player.getUuid().toString(), k -> new PlayerState());

        if (player.getDeltaY() > 0 && !player.isOnGround()) {
            state.towerTicks++;
            if (state.towerTicks > TOWER_TICK_THRESHOLD) {
                increaseBuffer(player, 0.3);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        } else {
            state.towerTicks = 0;
            decreaseBuffer(player, 0.2);
        }

        long now = System.currentTimeMillis();
        if (now - state.lastPlaceTime > 1000) {
            state.placeCount = 0;
            state.moveWhilePlacing = 0;
        }
    }

    @Override
    public void removePlayer(java.util.UUID uuid) {
        playerStates.remove(uuid.toString());
    }
}
