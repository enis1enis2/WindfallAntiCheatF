package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;

import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="MultiBreak A", stableKey="windfall.movement.multibreak", decay=0.02, setbackVl=20)
public class MultiBreakCheck extends Check implements PacketCheck {

    private static final int MAX_BREAKS_PER_TICK = 3;
    private static final long TICK_WINDOW_MS = 50;
    private static final int MAX_CONSECUTIVE_NO_MOVE = 5;

    private final ConcurrentHashMap<String, PlayerState> playerStates = new ConcurrentHashMap<>();

    static final class PlayerState {
        int breaksThisTick;
        long lastBreakTimestamp;
        long currentTickStart;
        int consecutiveNoMove;
        boolean movedSinceLastBreak;

        PlayerState() {}
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerActionC2SPacket)) return;

        PlayerActionC2SPacket action = (PlayerActionC2SPacket) packet;
        if (action.getAction() != PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) return;

        PlayerState state = playerStates.computeIfAbsent(player.getUuid().toString(), k -> new PlayerState());
        long now = System.currentTimeMillis();

        if (now - state.currentTickStart > TICK_WINDOW_MS) {
            state.breaksThisTick = 0;
            state.currentTickStart = now;
        }

        state.breaksThisTick++;

        if (state.breaksThisTick > MAX_BREAKS_PER_TICK) {
            increaseBuffer(player, (state.breaksThisTick - MAX_BREAKS_PER_TICK) * 1.5);
            if (getBuffer(player) > 2.0) {
                flag(player);
                resetBuffer(player);
            }
        }

        boolean moved = Math.abs(player.getDeltaX()) > 0.01 || Math.abs(player.getDeltaZ()) > 0.01;
        if (!moved) {
            state.consecutiveNoMove++;
            if (state.consecutiveNoMove > MAX_CONSECUTIVE_NO_MOVE) {
                increaseBuffer(player, 0.5);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        } else {
            state.consecutiveNoMove = 0;
        }

        state.lastBreakTimestamp = now;
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    @Override
    public void removePlayer(java.util.UUID uuid) {
        playerStates.remove(uuid.toString());
    }
}
