package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name="Scaffold A", stableKey="windfall.movement.scaffold", decay=0.01, setbackVl=20)
public class ScaffoldCheck extends Check implements PacketCheck {

    private int towerTicks = 0;

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    public void onTick(WindfallPlayer player, long tickCounter) {
        if (player.getDeltaY() > 0 && !player.isOnGround()) {
            towerTicks++;
            if (towerTicks > 10) {
                increaseBuffer(player, 0.3);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        } else {
            towerTicks = 0;
            decreaseBuffer(player, 0.2);
        }
    }
}
