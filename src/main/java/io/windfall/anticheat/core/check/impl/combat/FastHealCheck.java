package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.check.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Fast Heal A", stableKey = "windfall.combat.fastheal", decay = 0.02, setbackVl = 10, minVersion = 5, maxVersion = 107)
public class FastHealCheck extends Check implements PacketCheck {

    private static final double HEALTH_SWING_THRESHOLD = 3.0;
    private static final int HEAL_WINDOW_MS = 500;
    private static final int MIN_SWINGS_FOR_FLAG = 3;
    private static final double HEALTH_RATIO_THRESHOLD = 0.5;

    private static final class PlayerState {
        final ArrayDeque<HealthSnapshot> recentHealth = new ArrayDeque<>();
        double lastHealth;
        boolean hasLastHealth;
        int swingCount;
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
        if (packet instanceof net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket p) {
            final boolean[] isAttack = {false};
            p.handle(new net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler() {
                public void interact(net.minecraft.util.Hand hand) {}
                public void attack() { isAttack[0] = true; }
                public void interactAt(net.minecraft.util.Hand hand, net.minecraft.util.math.Vec3d location) {}
            });
            if (isAttack[0]) {
                handleAttack(player);
            }
        } else if (isMovementPacket(packet)) {
            handleMovement(player);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    private void handleAttack(WindfallPlayer player) {
        PlayerState state = getState(player.getUuid());
        state.swingCount++;

        double currentHealth = player.getServerPlayer().getHealth();

        if (state.hasLastHealth && state.lastHealth > 0) {
            double healthDelta = currentHealth - state.lastHealth;

            if (healthDelta > HEALTH_SWING_THRESHOLD) {
                long now = System.currentTimeMillis();
                state.recentHealth.addLast(new HealthSnapshot(currentHealth, healthDelta, now));

                while (!state.recentHealth.isEmpty() && now - state.recentHealth.peekFirst().timestamp > HEAL_WINDOW_MS) {
                    state.recentHealth.removeFirst();
                }

                checkFastHeal(player, state, healthDelta);
            }
        }

        state.lastHealth = currentHealth;
        state.hasLastHealth = true;
    }

    private void handleMovement(WindfallPlayer player) {
        PlayerState state = getState(player.getUuid());
        double currentHealth = player.getServerPlayer().getHealth();

        if (state.hasLastHealth && Math.abs(currentHealth - state.lastHealth) > 0.01) {
            state.lastHealth = currentHealth;
        }
    }

    private void checkFastHeal(WindfallPlayer player, PlayerState state, double healthDelta) {
        int recentHeals = state.recentHealth.size();

        if (recentHeals >= MIN_SWINGS_FOR_FLAG) {
            double avgHeal = state.recentHealth.stream()
                .mapToDouble(h -> h.healthDelta)
                .average()
                .orElse(0.0);

            double maxHealth = player.getServerPlayer().getMaxHealth();
            double healRatio = avgHeal / maxHealth;

            if (healRatio > HEALTH_RATIO_THRESHOLD) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        } else if (healthDelta > HEALTH_SWING_THRESHOLD * 2) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        }
    }

    private boolean isMovementPacket(Object packet) {
        return packet instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
    }

    private static final class HealthSnapshot {
        final double health;
        final double healthDelta;
        final long timestamp;

        HealthSnapshot(double health, double healthDelta, long timestamp) {
            this.health = health;
            this.healthDelta = healthDelta;
            this.timestamp = timestamp;
        }
    }
}
