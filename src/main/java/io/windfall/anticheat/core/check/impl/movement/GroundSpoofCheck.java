package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="GroundSpoof A", stableKey="windfall.movement.groundspoof", decay=0.02, setbackVl=20)
public class GroundSpoofCheck extends Check implements PacketCheck {

    private static final double MIN_DELTA_Y_TO_FLAG = 0.5;
    private static final double MAX_DELTA_Y_FOR_GROUND = 0.001;
    private static final double JUMP_TOLERANCE = 0.42;

    private final ConcurrentHashMap<String, PlayerState> playerStates = new ConcurrentHashMap<>();

    static final class PlayerState {
        double lastDeltaY;
        int spoofCount;
        PlayerState() {}
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerMoveC2SPacket)) return;
        if (player.isFlying() || player.isGliding() || player.isCachedIsFallFlying()) return;

        PlayerState state = playerStates.computeIfAbsent(player.getUuid().toString(), k -> new PlayerState());

        double deltaY = player.getY() - player.getLastY();
        double lastDeltaY = state.lastDeltaY;
        boolean claimedGround = player.isOnGround();
        boolean lastGround = player.isLastOnGround();

        if (claimedGround && Math.abs(deltaY) > MIN_DELTA_Y_TO_FLAG) {
            increaseBuffer(player, 0.5);
            state.spoofCount++;
            if (getBuffer(player) > 2.0 || state.spoofCount > 5) {
                flag(player);
                resetBuffer(player);
                state.spoofCount = 0;
            }
        } else if (claimedGround && !lastGround && deltaY > JUMP_TOLERANCE) {
            increaseBuffer(player, 0.3);
            if (getBuffer(player) > 2.0) {
                flag(player);
                resetBuffer(player);
            }
        } else if (!claimedGround && Math.abs(deltaY) < MAX_DELTA_Y_FOR_GROUND && lastGround) {
            decreaseBuffer(player, 0.2);
        } else {
            decreaseBuffer(player, 0.1);
            if (!claimedGround) {
                state.spoofCount = Math.max(0, state.spoofCount - 1);
            }
        }

        state.lastDeltaY = deltaY;
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    @Override
    public void removePlayer(java.util.UUID uuid) {
        playerStates.remove(uuid.toString());
    }
}
