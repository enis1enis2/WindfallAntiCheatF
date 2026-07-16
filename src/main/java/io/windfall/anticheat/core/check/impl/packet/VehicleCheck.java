package io.windfall.anticheat.core.check.impl.packet;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Vehicle A", stableKey="windfall.packet.vehicle", decay=0.02, setbackVl=20)
public class VehicleCheck extends Check implements PacketCheck {

    private static final double MAX_VEHICLE_SPEED = 1.5;
    private static final double MAX_VEHICLE_DISTANCE_SQ = 100.0;

    private final ConcurrentHashMap<java.util.UUID, PlayerState> playerStates = new ConcurrentHashMap<>();

    private static class PlayerState {
        boolean inVehicle = false;
        double lastVehicleX = 0;
        double lastVehicleY = 0;
        double lastVehicleZ = 0;
        long lastVehicleMoveTime = 0;
        int vehicleMoveCount = 0;
    }

    private PlayerState getState(WindfallPlayer player) {
        return playerStates.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!enabled) return;

        PlayerState state = getState(player);

        // Vehicle move detection
        if (packet instanceof VehicleMoveC2SPacket) {
            // Check if player is actually in a vehicle
            try {
                net.minecraft.server.network.ServerPlayerEntity sp = player.getServerPlayer();
                if (sp != null && !sp.hasVehicle()) {
                    state.inVehicle = false;
                    increaseBuffer(player, 2.0);
                    if (getBuffer(player) > 3.0) {
                        flag(player);
                        resetBuffer(player);
                        kickPlayer(player, "Vehicle move without vehicle");
                    }
                    return;
                }
            } catch (Exception ignored) {
            }

            VehicleMoveC2SPacket vehicleMove = (VehicleMoveC2SPacket) packet;
            net.minecraft.util.math.Vec3d pos = vehicleMove.position();
            double x = pos.x;
            double y = pos.y;
            double z = pos.z;

            // NaN/Infinite check
            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)
                    || Double.isInfinite(x) || Double.isInfinite(y) || Double.isInfinite(z)) {
                increaseBuffer(player, 2.0);
                flag(player);
                resetBuffer(player);
                kickPlayer(player, "Invalid vehicle coordinates");
                return;
            }

            // Vehicle speed check
            if (state.inVehicle && state.lastVehicleMoveTime > 0) {
                long now = System.currentTimeMillis();
                double dt = (now - state.lastVehicleMoveTime) / 1000.0;
                if (dt > 0 && dt < 1.0) {
                    double dx = x - state.lastVehicleX;
                    double dy = y - state.lastVehicleY;
                    double dz = z - state.lastVehicleZ;
                    double speed = Math.sqrt(dx * dx + dy * dy + dz * dz) / dt;

                    if (speed > MAX_VEHICLE_SPEED) {
                        increaseBuffer(player, 1.0);
                        if (getBuffer(player) > 3.0) {
                            flag(player);
                            resetBuffer(player);
                        }
                    }
                }
            }

            // Distance from player position check
            double distSq = player.getDistanceSq(x, y, z);
            if (distSq > MAX_VEHICLE_DISTANCE_SQ) {
                increaseBuffer(player, 1.5);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                    kickPlayer(player, "Vehicle too far from player");
                }
                return;
            }

            state.inVehicle = true;
            state.lastVehicleX = x;
            state.lastVehicleY = y;
            state.lastVehicleZ = z;
            state.lastVehicleMoveTime = System.currentTimeMillis();
            state.vehicleMoveCount++;
        }

        // Dismount detection (client command with vehicle-related action)
        if (packet instanceof ClientCommandC2SPacket) {
            ClientCommandC2SPacket cmd = (ClientCommandC2SPacket) packet;
            // If player sends a movement after dismount, reset vehicle state
            state.inVehicle = false;
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    @Override
    public void removePlayer(java.util.UUID uuid) {
        playerStates.remove(uuid);
    }

    private void kickPlayer(WindfallPlayer player, String reason) {
        net.minecraft.server.network.ServerPlayerEntity sp = player.getServerPlayer();
        if (sp != null && sp.networkHandler != null) {
            sp.networkHandler.disconnect(Text.literal("[Windfall] " + reason));
        }
    }
}
