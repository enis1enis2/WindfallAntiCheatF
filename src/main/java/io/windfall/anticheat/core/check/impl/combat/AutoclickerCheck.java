package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.*;

@CheckData(name="Autoclicker A", stableKey="windfall.combat.autoclicker", decay=0.02, setbackVl=30)
public class AutoclickerCheck extends Check implements PacketCheck {
    private final Map<UUID, List<Long>> clickTimestamps = new HashMap<>();

    @Override public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket p)) return;
        final boolean[] isAttack = {false};
        p.handle(new net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler() {
            @Override public void interact(net.minecraft.util.Hand hand) {}
            @Override public void interactAt(net.minecraft.util.Hand hand, net.minecraft.util.math.Vec3d pos) {}
            @Override public void attack() { isAttack[0] = true; }
        });
        if (!isAttack[0]) return;
        List<Long> timestamps = clickTimestamps.computeIfAbsent(player.getUuid(), k -> new ArrayList<>());
        long now = System.currentTimeMillis();
        timestamps.add(now);
        timestamps.removeIf(t -> now - t > 1000);
        int cps = timestamps.size();
        if (cps > 20) {
            increaseBuffer(player, (cps - 20) / 10.0);
            if (getBuffer(player) > 3.0) { flag(player); resetBuffer(player); }
        } else {
            decreaseBuffer(player, 0.5);
        }
    }
    @Override public void onPacketSend(WindfallPlayer player, Object packet) {}
    @Override public void removePlayer(UUID uuid) { clickTimestamps.remove(uuid); }
}
