package io.windfall.anticheat.core.check.impl.packet;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.text.Text;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Bad Packets A", stableKey="windfall.packet.bad", decay=0.0, setbackVl=5, compat={CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier=1.2)
public class BadPacketsCheck extends Check implements PacketCheck {

    private static final double MAX_Y = 400.0;
    private static final double MIN_Y = -64.0;
    private static final float MIN_YAW = -180.0f;
    private static final float MAX_YAW = 180.0f;
    private static final float MIN_PITCH = -90.0f;
    private static final float MAX_PITCH = 90.0f;
    private static final int MAX_ATTACKS_PER_TICK = 20;
    private static final int DUPLICATE_THRESHOLD = 10;
    private static final double DUPLICATE_EPSILON = 0.00001;

    private static final class PlayerState {
        double lastPosX, lastPosY, lastPosZ;
        float lastRotYaw, lastRotPitch;
        int duplicateCount;
        int attackCountThisTick;
        long currentTickStart;
        long lastAttackPacketTime;
        boolean loggedIn;
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

        if (now - state.currentTickStart > 50) {
            state.attackCountThisTick = 0;
            state.currentTickStart = now;
        }

        if (packet instanceof PlayerMoveC2SPacket move) {
            state.loggedIn = true;

            double x = move.getX(0.0);
            double y = move.getY(0.0);
            double z = move.getZ(0.0);

            validateCoordinates(player, x, y, z);

            float yaw = move.getYaw(0.0f);
            float pitch = move.getPitch(0.0f);

            validateRotation(player, yaw, pitch);
            checkDuplicate(player, x, y, z, yaw, pitch, state);
        }

        if (packet instanceof PlayerInteractEntityC2SPacket interact) {
            handleInteractEntity(player, interact, now, state);
        }

        if (packet instanceof HandSwingC2SPacket) {
            state.attackCountThisTick++;
            state.lastAttackPacketTime = now;

            if (state.attackCountThisTick > MAX_ATTACKS_PER_TICK) {
                flagDetail(player, "auto-clicker detected: " + state.attackCountThisTick + " attacks/tick");
            }
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    public void onLoginComplete(WindfallPlayer player) {
        getState(player).loggedIn = true;
    }

    private void handleInteractEntity(WindfallPlayer player, PlayerInteractEntityC2SPacket interact, long now, PlayerState state) {
        try {
            net.minecraft.server.world.ServerWorld world = player.getServerPlayer().getServerWorld();
            if (world == null) return;

            boolean[] isAttack = {false};
            interact.handle(new PlayerInteractEntityC2SPacket.Handler() {
                @Override
                public void attack() { isAttack[0] = true; }
                @Override
                public void interact(net.minecraft.util.Hand hand) {}
                @Override
                public void interactAt(net.minecraft.util.Hand hand, net.minecraft.util.math.Vec3d pos) {}
            });

            if (isAttack[0]) {
                state.attackCountThisTick++;
                state.lastAttackPacketTime = now;

                if (state.attackCountThisTick > MAX_ATTACKS_PER_TICK) {
                    flagDetail(player, "auto-clicker detected: " + state.attackCountThisTick + " attacks/tick");
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void validateCoordinates(WindfallPlayer player, double x, double y, double z) {
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)
                || Double.isInfinite(x) || Double.isInfinite(y) || Double.isInfinite(z)) {
            flagDetail(player, "NaN/Infinite coordinates, kicking");
            kickPlayer(player, "Invalid position data");
            return;
        }

        if (y > MAX_Y || y < MIN_Y) {
            flagDetail(player, "Y out of bounds: " + y);
        }
    }

    private void validateRotation(WindfallPlayer player, float yaw, float pitch) {
        if (Float.isNaN(yaw) || Float.isNaN(pitch)
                || Float.isInfinite(yaw) || Float.isInfinite(pitch)) {
            flagDetail(player, "NaN/Infinite rotation");
            return;
        }

        if (yaw < MIN_YAW || yaw > MAX_YAW) {
            flagDetail(player, "Yaw out of range: " + yaw);
        }

        if (pitch < MIN_PITCH || pitch > MAX_PITCH) {
            flagDetail(player, "Pitch out of range: " + pitch);
        }
    }

    private void checkDuplicate(WindfallPlayer player, double x, double y, double z, float yaw, float pitch, PlayerState state) {
        if (Math.abs(x - state.lastPosX) < DUPLICATE_EPSILON
                && Math.abs(y - state.lastPosY) < DUPLICATE_EPSILON
                && Math.abs(z - state.lastPosZ) < DUPLICATE_EPSILON
                && Math.abs(yaw - state.lastRotYaw) < DUPLICATE_EPSILON
                && Math.abs(pitch - state.lastRotPitch) < DUPLICATE_EPSILON) {
            state.duplicateCount++;
            if (state.duplicateCount > DUPLICATE_THRESHOLD) {
                increaseBuffer(player, 0.1);
            }
        } else {
            state.duplicateCount = Math.max(0, state.duplicateCount - 1);
        }

        state.lastPosX = x;
        state.lastPosY = y;
        state.lastPosZ = z;
        state.lastRotYaw = yaw;
        state.lastRotPitch = pitch;
    }

    private void flagDetail(WindfallPlayer player, String detail) {
        flag(player);
        WindfallMod.LOGGER.warn("[Bad Packets A] {}: {}", player.getName(), detail);
    }

    private void kickPlayer(WindfallPlayer player, String reason) {
        net.minecraft.server.network.ServerPlayerEntity sp = player.getServerPlayer();
        if (sp != null && sp.networkHandler != null) {
            sp.networkHandler.disconnect(Text.literal("[Windfall] " + reason));
        }
    }
}
