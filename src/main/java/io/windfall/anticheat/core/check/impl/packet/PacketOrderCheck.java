package io.windfall.anticheat.core.check.impl.packet;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.*;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@CheckData(name="Packet Order A", stableKey="windfall.packet.order", decay=0.01, setbackVl=15,
    compat={CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier=1.2)
public class PacketOrderCheck extends Check implements PacketCheck {

    private static final int MAX_MOVEMENT_BEFORE_LOGIN = 0;
    private static final int DUPLICATE_PACKET_THRESHOLD = 5;
    private static final long PACKET_BURST_WINDOW_MS = 100;
    private static final int MAX_PACKETS_IN_BURST = 15;

    private static final class PlayerState {
        boolean loginComplete;
        int movementCountBeforeLogin;
        int duplicatePacketCount;
        long lastPacketHash;
        final ConcurrentLinkedDeque<Long> packetBurst = new ConcurrentLinkedDeque<>();
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!enabled) return;

        long now = System.currentTimeMillis();
        PlayerState state = getState(player);

        // Burst detection
        state.packetBurst.addLast(now);
        while (!state.packetBurst.isEmpty() && now - state.packetBurst.peekFirst() > PACKET_BURST_WINDOW_MS) {
            state.packetBurst.removeFirst();
        }
        if (state.packetBurst.size() > MAX_PACKETS_IN_BURST) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        }

        // Consecutive duplicate detection
        long currentHash = packet.getClass().getName().hashCode();
        if (currentHash == state.lastPacketHash && isMovementPacket(packet)) {
            state.duplicatePacketCount++;
            if (state.duplicatePacketCount > DUPLICATE_PACKET_THRESHOLD) {
                flag(player);
                state.duplicatePacketCount = 0;
            }
        } else {
            state.duplicatePacketCount = 0;
        }
        state.lastPacketHash = currentHash;

        // Pre-login movement detection
        if (!state.loginComplete) {
            if (isMovementPacket(packet)) {
                state.movementCountBeforeLogin++;
            }
            if (packet instanceof PlayerMoveC2SPacket move) {
                if (move.changesPosition()) {
                    state.loginComplete = true;
                    if (state.movementCountBeforeLogin > MAX_MOVEMENT_BEFORE_LOGIN) {
                        flag(player);
                    }
                }
            }
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    public void onLoginComplete(WindfallPlayer player) {
        getState(player).loginComplete = true;
    }

    private boolean isMovementPacket(Object packet) {
        return packet instanceof PlayerMoveC2SPacket || packet instanceof HandSwingC2SPacket;
    }
}
