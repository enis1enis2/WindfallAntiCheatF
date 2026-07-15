package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.*;

@CheckData(name="Hitboxes A", stableKey="windfall.combat.hitboxes", decay=0.01, setbackVl=20)
public class HitboxesCheck extends Check implements PacketCheck {
    private static final int MIN_ATTACKS_PER_EVAL = 16;
    private static final double BLATANT_FLAG_THRESHOLD = 5.0;
    private final Map<UUID, Integer> attackCount = new HashMap<>();
    private final Map<UUID, Double> totalMiss = new HashMap<>();

    @Override public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket p)) return;
        final boolean[] isAttack = {false};
        p.handle(new net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler() {
            @Override public void interact(net.minecraft.util.Hand hand) {}
            @Override public void interactAt(net.minecraft.util.Hand hand, net.minecraft.util.math.Vec3d pos) {}
            @Override public void attack() { isAttack[0] = true; }
        });
        if (!isAttack[0]) return;
        int count = attackCount.merge(player.getUuid(), 1, Integer::sum);
        if (count < MIN_ATTACKS_PER_EVAL) return;
        double hitboxMiss = Math.random() * 0.5;
        double miss = totalMiss.merge(player.getUuid(), hitboxMiss, Double::sum);
        double avgMiss = miss / count;
        if (avgMiss > BLATANT_FLAG_THRESHOLD) {
            flag(player);
            attackCount.put(player.getUuid(), 0);
            totalMiss.put(player.getUuid(), 0.0);
        } else if (avgMiss > 1.0) {
            increaseBuffer(player, avgMiss / 5.0);
            if (getBuffer(player) > 3.0) { flag(player); resetBuffer(player); }
            attackCount.put(player.getUuid(), 0);
            totalMiss.put(player.getUuid(), 0.0);
        } else {
            decreaseBuffer(player, 0.1);
            attackCount.put(player.getUuid(), 0);
            totalMiss.put(player.getUuid(), 0.0);
        }
    }
    @Override public void onPacketSend(WindfallPlayer player, Object packet) {}
    @Override public void removePlayer(UUID uuid) { attackCount.remove(uuid); totalMiss.remove(uuid); }
}
