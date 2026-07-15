package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.*;

@CheckData(name="FastHeal A", stableKey="windfall.combat.fastheal", decay=0.02, setbackVl=20)
public class FastHealCheck extends Check implements PacketCheck {
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();

    @Override public void onPacketReceive(WindfallPlayer player, Object packet) {}
    @Override public void onPacketSend(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket)) return;
        long now = System.currentTimeMillis();
        Long last = lastDamageTime.get(player.getUuid());
        if (last != null && (now - last) < 500) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 2.0) { flag(player); resetBuffer(player); }
        }
        lastDamageTime.put(player.getUuid(), now);
    }
    @Override public void removePlayer(UUID uuid) { lastDamageTime.remove(uuid); }
}
