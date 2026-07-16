package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="WrongBreak A", stableKey="windfall.movement.wrongbreak", decay=0.02, setbackVl=20)
public class WrongBreakCheck extends Check implements PacketCheck {

    private static final long FLAG_COOLDOWN_MS = 1000;

    private final ConcurrentHashMap<String, PlayerState> playerStates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastFlagTime = new ConcurrentHashMap<>();

    static final class PlayerState {
        BlockPos startBlockPos;
        BlockState startBlockState;
        long startTimestamp;

        PlayerState() {}
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerActionC2SPacket)) return;

        PlayerActionC2SPacket action = (PlayerActionC2SPacket) packet;
        PlayerActionC2SPacket.Action actionType = action.getAction();
        PlayerState state = playerStates.computeIfAbsent(player.getUuid().toString(), k -> new PlayerState());

        if (actionType == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
            BlockPos blockPos = action.getPos();

            ServerWorld world;
            try {
                world = (ServerWorld) player.getServerPlayer().getWorld();
            } catch (Exception e) {
                return;
            }

            state.startBlockPos = blockPos;
            state.startBlockState = world.getBlockState(blockPos);
            state.startTimestamp = System.currentTimeMillis();

        } else if (actionType == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
            if (state.startBlockPos == null || state.startBlockState == null) return;

            long elapsed = System.currentTimeMillis() - state.startTimestamp;
            if (elapsed > 5000) {
                state.startBlockPos = null;
                state.startBlockState = null;
                return;
            }

            BlockPos stopPos = action.getPos();

            if (!stopPos.equals(state.startBlockPos)) {
                long now = System.currentTimeMillis();
                Long last = lastFlagTime.get(player.getUuid());
                if (last == null || now - last > FLAG_COOLDOWN_MS) {
                    increaseBuffer(player, 2.0);
                    if (getBuffer(player) > 2.0) {
                        flag(player);
                        resetBuffer(player);
                        lastFlagTime.put(player.getUuid(), now);
                    }
                }
                state.startBlockPos = null;
                state.startBlockState = null;
                return;
            }

            ServerWorld world;
            try {
                world = (ServerWorld) player.getServerPlayer().getWorld();
            } catch (Exception e) {
                return;
            }

            BlockState currentBlockState = world.getBlockState(stopPos);
            if (!currentBlockState.equals(state.startBlockState)) {
                long now = System.currentTimeMillis();
                Long last = lastFlagTime.get(player.getUuid());
                if (last == null || now - last > FLAG_COOLDOWN_MS) {
                    increaseBuffer(player, 1.5);
                    if (getBuffer(player) > 2.0) {
                        flag(player);
                        resetBuffer(player);
                        lastFlagTime.put(player.getUuid(), now);
                    }
                }
            } else {
                decreaseBuffer(player, 0.1);
            }

            state.startBlockPos = null;
            state.startBlockState = null;
        } else if (actionType == PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK) {
            state.startBlockPos = null;
            state.startBlockState = null;
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    @Override
    public void removePlayer(java.util.UUID uuid) {
        playerStates.remove(uuid.toString());
        lastFlagTime.remove(uuid);
    }
}
