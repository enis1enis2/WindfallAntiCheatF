package io.windfall.anticheat.core.check.impl.packet;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name="Transaction A", stableKey="windfall.packet.transaction", decay=0.02, setbackVl=20)
public class TransactionCheck extends Check implements PacketCheck {
    private int skipCount = 0;

    @Override public void onPacketReceive(WindfallPlayer player, Object packet) {}
    @Override public void onPacketSend(WindfallPlayer player, Object packet) {}

    public void onTick(WindfallPlayer player) {
        int ping = player.getTransactionPing();
        if (ping < 0 || ping > 5000) {
            skipCount++;
            if (skipCount > 20) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        } else {
            skipCount = 0;
            decreaseBuffer(player, 0.5);
        }
    }
}
