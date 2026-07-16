package io.windfall.anticheat.core.check.impl.packet;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="PacketOrder A", stableKey="windfall.packet.order", decay=0.02, setbackVl=20)
public class PacketOrderCheck extends Check implements PacketCheck {

    private final ConcurrentHashMap<java.util.UUID, PlayerState> playerStates = new ConcurrentHashMap<>();

    private static class PlayerState {
        boolean lastWasMovement = false;
        boolean lastWasAttack = false;
        boolean lastWasPlace = false;
        boolean lastWasClick = false;
        boolean movedSinceLastAttack = false;
        boolean clickedSinceLastPlace = false;
        int sequenceId = 0;
    }

    private PlayerState getState(WindfallPlayer player) {
        return playerStates.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!enabled) return;

        PlayerState state = getState(player);

        // Track movement packets
        if (packet instanceof PlayerMoveC2SPacket) {
            state.lastWasMovement = true;
            state.movedSinceLastAttack = true;
            state.lastWasAttack = false;
            return;
        }

        // Attack must come after movement (not before)
        if (packet instanceof HandSwingC2SPacket || packet instanceof net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket) {
            if (state.lastWasAttack && !state.movedSinceLastAttack) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
            state.lastWasAttack = true;
            state.movedSinceLastAttack = false;
            state.lastWasMovement = false;
            return;
        }

        // Place must come after corresponding click
        if (packet instanceof PlayerInteractBlockC2SPacket) {
            if (state.lastWasPlace && !state.clickedSinceLastPlace) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
            state.lastWasPlace = true;
            state.clickedSinceLastPlace = false;
            return;
        }

        // Click window tracking
        if (packet instanceof ClickSlotC2SPacket) {
            state.clickedSinceLastPlace = true;
            state.lastWasClick = true;
            return;
        }

        // Client command resets movement tracking
        if (packet instanceof ClientCommandC2SPacket) {
            state.lastWasMovement = false;
            state.lastWasAttack = false;
            return;
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
