package io.windfall.anticheat.core.check.impl.packet;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Sprint A", stableKey="windfall.packet.sprint", decay=0.02, setbackVl=20)
public class SprintCheck extends Check implements PacketCheck {

    private static final double SPRINT_SPEED = 0.1573;
    private static final double WALK_SPEED = 0.13;
    private static final double SPEED_TOLERANCE = 0.05;

    private final ConcurrentHashMap<java.util.UUID, PlayerState> playerStates = new ConcurrentHashMap<>();

    private static class PlayerState {
        boolean sprinting = false;
        boolean lastSprinting = false;
        int sprintToggleCountThisTick = 0;
        long lastSprintToggleTime = 0;
    }

    private PlayerState getState(WindfallPlayer player) {
        return playerStates.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!enabled) return;
        if (!(packet instanceof ClientCommandC2SPacket)) return;

        ClientCommandC2SPacket cmd = (ClientCommandC2SPacket) packet;
        PlayerState state = getState(player);

        if (cmd.getMode() == ClientCommandC2SPacket.Mode.START_SPRINTING) {
            state.lastSprinting = state.sprinting;
            state.sprinting = true;
            player.setSprinting(true);
            state.sprintToggleCountThisTick++;
            state.lastSprintToggleTime = System.currentTimeMillis();

            // Detect impossible sprint transition: if already sprinting
            if (state.lastSprinting) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }

            // Detect impossible sprint: player is sneaking or on cooldown
            if (player.isSneaking()) {
                increaseBuffer(player, 2.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                    kickPlayer(player, "Sprint while sneaking");
                }
            }
        } else if (cmd.getMode() == ClientCommandC2SPacket.Mode.STOP_SPRINTING) {
            state.lastSprinting = state.sprinting;
            state.sprinting = false;
            player.setSprinting(false);
            state.sprintToggleCountThisTick++;
            state.lastSprintToggleTime = System.currentTimeMillis();

            // Detect impossible sprint transition: if not sprinting
            if (!state.lastSprinting) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    public void onTick(WindfallPlayer player) {
        PlayerState state = getState(player);
        // Multiple sprint toggles per tick is suspicious
        if (state.sprintToggleCountThisTick > 3) {
            increaseBuffer(player, 1.5);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        }
        state.sprintToggleCountThisTick = 0;
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
