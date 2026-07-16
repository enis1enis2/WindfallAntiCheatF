package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;

import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="NoSlow A", stableKey="windfall.movement.noslow", decay=0.02, setbackVl=20)
public class NoSlowCheck extends Check implements PacketCheck {

    private static final double SNEAK_SPEED_FACTOR = 0.3;
    private static final double USING_SPEED_FACTOR = 0.2;
    private static final double SOUL_SAND_SPEED_FACTOR = 0.4;
    private static final double HONEY_BLOCK_SPEED_FACTOR = 0.4;
    private static final double BASE_WALK_SPEED = 0.2257;
    private static final double BASE_SPRINT_SPEED = 0.2873;
    private static final double TOLERANCE = 0.05;

    private final ConcurrentHashMap<String, PlayerState> playerStates = new ConcurrentHashMap<>();

    static final class PlayerState {
        boolean lastSneaking;
        boolean lastUsingItem;
        PlayerState() {}
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerMoveC2SPacket)) return;
        if (player.isFlying() || player.isGliding() || player.isCachedIsFallFlying()) return;
        if (!player.isOnGround()) return;

        PlayerState state = playerStates.computeIfAbsent(player.getUuid().toString(), k -> new PlayerState());

        ServerWorld world = (ServerWorld) player.getServerPlayer().getWorld();
        BlockPos blockPos = new BlockPos((int) Math.floor(player.getX()), (int) Math.floor(player.getY()), (int) Math.floor(player.getZ()));

        boolean sneaking = player.isSneaking();
        boolean usingItem = player.getActionData().isUsingItem();
        boolean onSoulSand = world.getBlockState(blockPos.down()).isOf(Blocks.SOUL_SAND)
                          || world.getBlockState(blockPos.down()).isOf(Blocks.SOUL_SOIL);
        boolean onHoney = world.getBlockState(blockPos.down()).isOf(Blocks.HONEY_BLOCK);

        if (!sneaking && !usingItem && !onSoulSand && !onHoney) {
            state.lastSneaking = sneaking;
            state.lastUsingItem = usingItem;
            decreaseBuffer(player, 0.1);
            return;
        }

        double baseSpeed = player.isSprinting() ? BASE_SPRINT_SPEED : BASE_WALK_SPEED;
        double maxAllowedSpeed = baseSpeed;

        if (sneaking) {
            maxAllowedSpeed *= SNEAK_SPEED_FACTOR;
        }
        if (usingItem) {
            maxAllowedSpeed *= USING_SPEED_FACTOR;
        }
        if (onSoulSand) {
            maxAllowedSpeed *= SOUL_SAND_SPEED_FACTOR;
        }
        if (onHoney) {
            maxAllowedSpeed *= HONEY_BLOCK_SPEED_FACTOR;
        }

        double speedMultiplier = player.getCachedSpeedMultiplier() * player.getCachedSlownessMultiplier();
        maxAllowedSpeed *= Math.max(0.1, speedMultiplier);
        maxAllowedSpeed += TOLERANCE;

        double horizontalSpeed = player.getHorizontalSpeed();

        if (horizontalSpeed > maxAllowedSpeed) {
            double over = horizontalSpeed - maxAllowedSpeed;
            increaseBuffer(player, over / Math.max(0.01, maxAllowedSpeed));
            if (getBuffer(player) > 2.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.1);
        }

        state.lastSneaking = sneaking;
        state.lastUsingItem = usingItem;
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    @Override
    public void removePlayer(java.util.UUID uuid) {
        playerStates.remove(uuid.toString());
    }
}
