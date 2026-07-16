package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.packet.VehicleCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VehicleDetectionTest extends CheckTestBase {

    private VehicleCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new VehicleCheck();
        player = createMockPlayer("TestPlayer");
    }

    private Object createVehicleMovePacket(double x, double y, double z) throws Exception {
        Class<?> clazz = Class.forName(
                "net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket",
                false, getClass().getClassLoader());
        Constructor<?> ctor = clazz.getDeclaredConstructor(
                Vec3d.class, float.class, float.class, boolean.class);
        ctor.setAccessible(true);
        return ctor.newInstance(new Vec3d(x, y, z), 0.0f, 0.0f, false);
    }

    @Test
    void noVehicle_noFlag() throws Exception {
        player.setPosition(0, 64, 0);

        Object vehiclePacket = createVehicleMovePacket(0, 64, 0);
        check.onPacketReceive(player, vehiclePacket);

        assertEquals(0.0, check.getBuffer(player), 0.001);
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        player.setPosition(0, 64, 0);

        Object vehiclePacket = createVehicleMovePacket(0, 64, 0);
        check.onPacketReceive(player, vehiclePacket);

        UUID uuid = player.getUuid();
        Field stateField = VehicleCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> states = (Map<?, ?>) stateField.get(check);
        assertTrue(states.containsKey(uuid));

        check.removePlayer(uuid);
        assertFalse(states.containsKey(uuid));
    }
}
