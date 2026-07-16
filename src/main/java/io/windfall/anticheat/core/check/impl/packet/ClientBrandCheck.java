package io.windfall.anticheat.core.check.impl.packet;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.BrandCustomPayload;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@CheckData(name="ClientBrand A", stableKey="windfall.packet.clientbrand", decay=0.02, setbackVl=20)
public class ClientBrandCheck extends Check implements PacketCheck {

    private static final Set<String> BLOCKED_BRANDS = new HashSet<>(Arrays.asList(
            "wurst", "impact", "moon", "liquidbounce", "koks", "aristois",
            "vape", "rusherhack", "ghostly", "phobos", "fdp", "salhack",
            "rise", "astolfo", "meteor", "inertia"
    ));

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!enabled) return;
        if (!(packet instanceof net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket cp)) return;

        if (!(cp.payload() instanceof BrandCustomPayload brandPayload)) return;

        String brand = brandPayload.brand();
        if (brand == null || brand.isEmpty()) return;

        String normalized = brand.toLowerCase().replaceAll("[^a-z0-9]", "");

        for (String blocked : BLOCKED_BRANDS) {
            if (normalized.contains(blocked)) {
                increaseBuffer(player, 5.0);
                flag(player);
                resetBuffer(player);
                kickPlayer(player, "Unauthorized client: " + brand);
                return;
            }
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    private void kickPlayer(WindfallPlayer player, String reason) {
        net.minecraft.server.network.ServerPlayerEntity sp = player.getServerPlayer();
        if (sp != null && sp.networkHandler != null) {
            sp.networkHandler.disconnect(Text.literal("[Windfall] " + reason));
        }
    }
}
