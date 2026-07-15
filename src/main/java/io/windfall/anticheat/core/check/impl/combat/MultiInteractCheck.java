package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.*;

@CheckData(name="MultiInteract A", stableKey="windfall.combat.multiinteract", decay=0.02, setbackVl=20)
public class MultiInteractCheck extends Check implements PacketCheck {
    private final Map<UUID, Long> lastInteract = new HashMap<>();

    @Override public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.InteractEntityC2SPacket)) return;
        long now = System.currentTimeMillis();
        Long last = lastInteract.get(player.getUuid());
        if (last != null && (now - last) < 5) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 2.0) { flag(player); resetBuffer(player); }
        }
        lastInteract.put(player.getUuid(), now);
    }
    @Override public void onPacketSend(WindfallPlayer player, Object packet) {}
    @Override public void removePlayer(UUID uuid) { lastInteract.remove(uuid); }
}
