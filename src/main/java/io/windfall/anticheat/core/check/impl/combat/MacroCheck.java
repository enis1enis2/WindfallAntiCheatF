package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.check.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Macro A", stableKey = "windfall.combat.macro", decay = 0.01, setbackVl = 20)
public class MacroCheck extends Check implements PacketCheck {

    private static final int PATTERN_WINDOW = 50;
    private static final int MIN_REPEAT_COUNT = 8;
    private static final double REPETITION_THRESHOLD = 0.9;

    private static final class PlayerState {
        final Map<String, Integer> movementPatterns = new ConcurrentHashMap<>();
        long lastPatternTime;
        StringBuilder patternBuffer = new StringBuilder();
        int totalPatterns;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(UUID uuid) {
        return stateMap.computeIfAbsent(uuid, k -> new PlayerState());
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket)) return;

        PlayerState state = getState(player.getUuid());
        long now = System.currentTimeMillis();

        if (now - state.lastPatternTime > 100) {
            flushPattern(state);
        }
        state.lastPatternTime = now;

        char movementCode = getMovementCode(player);
        state.patternBuffer.append(movementCode);
        state.totalPatterns++;

        if (state.patternBuffer.length() > PATTERN_WINDOW) {
            state.patternBuffer.deleteCharAt(0);
        }

        if (state.patternBuffer.length() >= 10) {
            detectRepetition(player, state);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    private void flushPattern(PlayerState state) {
        if (state.patternBuffer.length() >= 5) {
            String pattern = state.patternBuffer.toString();
            state.movementPatterns.merge(pattern, 1, Integer::sum);
        }
        state.patternBuffer = new StringBuilder();
    }

    private void detectRepetition(WindfallPlayer player, PlayerState state) {
        String current = state.patternBuffer.toString();
        if (current.length() < 10) return;

        int occurrences = 0;
        for (Map.Entry<String, Integer> entry : state.movementPatterns.entrySet()) {
            if (entry.getKey().contains(current.substring(current.length() - 5))) {
                occurrences += entry.getValue();
            }
        }

        double ratio = (double) occurrences / Math.max(state.totalPatterns, 1);
        if (ratio > REPETITION_THRESHOLD && occurrences >= MIN_REPEAT_COUNT) {
            increaseBuffer(player, 2.0);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
                state.movementPatterns.clear();
                state.totalPatterns = 0;
            }
        }
    }

    private char getMovementCode(WindfallPlayer player) {
        double dx = player.getX() - player.getLastX();
        double dz = player.getZ() - player.getLastZ();

        if (Math.abs(dx) < 0.001 && Math.abs(dz) < 0.001) {
            return player.isSprinting() ? 'F' : 'P';
        }

        boolean forward = dz < 0;
        boolean right = dx > 0;

        if (forward && right) return 'M';
        if (forward) return 'P';
        if (right) return 'R';
        return 'F';
    }
}
