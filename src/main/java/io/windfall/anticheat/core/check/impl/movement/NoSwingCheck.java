package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="No Swing A", stableKey="windfall.movement.noswing", decay=0.02, setbackVl=10)
public class NoSwingCheck extends Check implements PacketCheck {

    private static final long SWING_TIMEOUT_MS = 300;
    private static final int BUFFER_THRESHOLD = 3;

    private static final class PlayerState {
        long lastSwingTime;
        int missingSwingCount;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!enabled) return;

        if (packet instanceof HandSwingC2SPacket) {
            getState(player).lastSwingTime = System.currentTimeMillis();
            return;
        }

        if (packet instanceof PlayerActionC2SPacket) {
            PlayerActionC2SPacket action = (PlayerActionC2SPacket) packet;
            if (action.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
                checkSwing(player);
            }
        } else if (packet instanceof PlayerInteractBlockC2SPacket) {
            PlayerInteractBlockC2SPacket interact = (PlayerInteractBlockC2SPacket) packet;
            if (interact.getBlockHitResult().getSide() != net.minecraft.util.math.Direction.UP) {
                checkSwing(player);
            }
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    private void checkSwing(WindfallPlayer player) {
        PlayerState state = getState(player);
        long now = System.currentTimeMillis();
        if (now - state.lastSwingTime > SWING_TIMEOUT_MS) {
            state.missingSwingCount++;
            if (state.missingSwingCount >= BUFFER_THRESHOLD) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        } else {
            state.missingSwingCount = Math.max(0, state.missingSwingCount - 1);
        }
    }
}
