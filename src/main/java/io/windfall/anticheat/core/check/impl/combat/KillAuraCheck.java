package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.*;

@CheckData(name="KillAura A", stableKey="windfall.combat.killaura", decay=0.02, setbackVl=20)
public class KillAuraCheck extends Check implements PacketCheck {
    private final Map<UUID, List<Long>> attackIntervals = new HashMap<>();

    @Override public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket p)) return;
        final boolean[] isAttack = {false};
        p.handle(new net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler() {
            @Override public void interact(net.minecraft.util.Hand hand) {}
            @Override public void interactAt(net.minecraft.util.Hand hand, net.minecraft.util.math.Vec3d pos) {}
            @Override public void attack() { isAttack[0] = true; }
        });
        if (!isAttack[0]) return;
        List<Long> intervals = attackIntervals.computeIfAbsent(player.getUuid(), k -> new ArrayList<>());
        long now = System.currentTimeMillis();
        if (!intervals.isEmpty()) {
            long last = intervals.get(intervals.size() - 1);
            long diff = now - last;
            if (diff > 0 && diff < 10) {
                increaseBuffer(player, 0.5);
                if (getBuffer(player) > 2.0) { flag(player); resetBuffer(player); }
            }
        }
        intervals.add(now);
        if (intervals.size() > 50) intervals.remove(0);
    }
    @Override public void onPacketSend(WindfallPlayer player, Object packet) {}
    @Override public void removePlayer(UUID uuid) { attackIntervals.remove(uuid); }
}
