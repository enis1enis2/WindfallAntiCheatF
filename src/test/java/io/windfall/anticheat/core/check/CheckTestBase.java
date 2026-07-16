package io.windfall.anticheat.core.check;

import com.google.gson.JsonObject;
import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.config.WindfallConfig;
import io.windfall.anticheat.core.player.WindfallPlayer;
import io.windfall.anticheat.core.severity.SeverityManager;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class CheckTestBase {
    protected WindfallMod testMod;
    protected WindfallConfig testConfig;
    protected SeverityManager testSeverityManager;

    @BeforeEach
    void setUpCheckBase() throws Exception {
        testMod = new WindfallMod();

        Field instanceField = WindfallMod.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, testMod);

        testConfig = new WindfallConfig(null);
        JsonObject json = new JsonObject();
        JsonObject checks = new JsonObject();
        JsonObject defaultCheck = new JsonObject();
        defaultCheck.addProperty("enabled", true);
        defaultCheck.addProperty("max_vl", 100);
        defaultCheck.addProperty("punishable", true);
        defaultCheck.addProperty("decay", 0.02);
        checks.add("default", defaultCheck);
        json.add("checks", checks);

        Field configField = WindfallConfig.class.getDeclaredField("config");
        configField.setAccessible(true);
        configField.set(testConfig, json);

        Field modConfigField = WindfallMod.class.getDeclaredField("config");
        modConfigField.setAccessible(true);
        modConfigField.set(testMod, testConfig);

        testSeverityManager = SeverityManager.fromConfig(testConfig);
        Field sevField = WindfallMod.class.getDeclaredField("severityManager");
        sevField.setAccessible(true);
        sevField.set(testMod, testSeverityManager);
    }

    @AfterEach
    void tearDownCheckBase() throws Exception {
        Field instanceField = WindfallMod.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    protected void setConfigValue(String path, Object value) throws Exception {
        Field configField = WindfallConfig.class.getDeclaredField("config");
        configField.setAccessible(true);
        JsonObject json = (JsonObject) configField.get(testConfig);
        String[] parts = path.split("\\.");
        JsonObject current = json;
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.has(parts[i]) || !current.get(parts[i]).isJsonObject()) {
                JsonObject child = new JsonObject();
                current.add(parts[i], child);
                current = child;
            } else {
                current = current.getAsJsonObject(parts[i]);
            }
        }
        if (value instanceof Boolean) current.addProperty(parts[parts.length - 1], (Boolean) value);
        else if (value instanceof Integer) current.addProperty(parts[parts.length - 1], (Integer) value);
        else if (value instanceof Double) current.addProperty(parts[parts.length - 1], (Double) value);
        else if (value instanceof String) current.addProperty(parts[parts.length - 1], (String) value);
    }

    protected WindfallPlayer createMockPlayer(String name) {
        return createMockPlayer(name, UUID.randomUUID());
    }

    protected WindfallPlayer createMockPlayer(String name, UUID uuid) {
        try {
            sun.misc.Unsafe unsafe = getUnsafe();
            WindfallPlayer player = (WindfallPlayer) unsafe.allocateInstance(WindfallPlayer.class);

            setField(WindfallPlayer.class, "uuid", player, uuid);
            setField(WindfallPlayer.class, "name", player, name);
            setField(WindfallPlayer.class, "protocolVersion", player, 770);
            setField(WindfallPlayer.class, "violationLevels", player, new ConcurrentHashMap<String, Integer>());
            setField(WindfallPlayer.class, "buffers", player, new ConcurrentHashMap<String, Double>());
            setField(WindfallPlayer.class, "alertsEnabled", player, true);
            setField(WindfallPlayer.class, "valid", player, true);
            setField(WindfallPlayer.class, "pose", player, WindfallPlayer.Pose.STANDING);
            setField(WindfallPlayer.class, "actionData", player, null);
            setField(WindfallPlayer.class, "serverPlayer", player, null);

            Class<?> posStateClass = Class.forName("io.windfall.anticheat.core.player.WindfallPlayer$PositionState");
            Constructor<?> posCtor = posStateClass.getDeclaredConstructor(
                    double.class, double.class, double.class,
                    double.class, double.class, double.class,
                    double.class, double.class, double.class,
                    double.class, double.class, double.class,
                    int.class);
            posCtor.setAccessible(true);
            Object posState = posCtor.newInstance(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            setField(WindfallPlayer.class, "pos", player, posState);

            Class<?> groundStateClass = Class.forName("io.windfall.anticheat.core.player.WindfallPlayer$GroundState");
            Constructor<?> groundCtor = groundStateClass.getDeclaredConstructor(
                    boolean.class, boolean.class, double.class, double.class, double.class);
            groundCtor.setAccessible(true);
            Object groundState = groundCtor.newInstance(false, false, 0.0, 0.0, 0.0);
            setField(WindfallPlayer.class, "ground", player, groundState);

            Class<?> rotStateClass = Class.forName("io.windfall.anticheat.core.player.WindfallPlayer$RotationState");
            Constructor<?> rotCtor = rotStateClass.getDeclaredConstructor(
                    float.class, float.class, float.class, float.class);
            rotCtor.setAccessible(true);
            Object rotState = rotCtor.newInstance(0.0f, 0.0f, 0.0f, 0.0f);
            setField(WindfallPlayer.class, "rotation", player, rotState);

            return player;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test WindfallPlayer", e);
        }
    }

    protected static sun.misc.Unsafe getUnsafe() throws Exception {
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (sun.misc.Unsafe) unsafeField.get(null);
    }

    protected static void setField(Class<?> clazz, String fieldName, Object target, Object value) throws Exception {
        Field f = clazz.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    protected PlayerMoveC2SPacket createMovePacket(double x, double y, double z, boolean onGround) {
        return new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, false);
    }

    protected PlayerMoveC2SPacket createFullMovePacket(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        return new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, onGround, false);
    }

    protected HandSwingC2SPacket createSwingPacket() {
        return new HandSwingC2SPacket(Hand.MAIN_HAND);
    }

    protected ChatMessageC2SPacket createChatPacket(String message) {
        return new ChatMessageC2SPacket(message, Instant.now(), 0L, null, null);
    }

    protected CreativeInventoryActionC2SPacket createCreativePacket(int slot) throws Exception {
        sun.misc.Unsafe unsafe = getUnsafe();
        Class<?> creativeClass = Class.forName(
                "net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket",
                false, getClass().getClassLoader());
        Object pkt = unsafe.allocateInstance(creativeClass);
        Field slotField = creativeClass.getDeclaredField("slot");
        slotField.setAccessible(true);
        slotField.set(pkt, (short) slot);
        return (CreativeInventoryActionC2SPacket) pkt;
    }

    protected PlayerInteractEntityC2SPacket createAttackPacket() throws Exception {
        sun.misc.Unsafe unsafe = getUnsafe();
        PlayerInteractEntityC2SPacket packet = (PlayerInteractEntityC2SPacket) unsafe.allocateInstance(PlayerInteractEntityC2SPacket.class);

        Field attackField = PlayerInteractEntityC2SPacket.class.getDeclaredField("ATTACK");
        attackField.setAccessible(true);
        Object attackHandler = attackField.get(null);

        if (attackHandler == null) {
            Class<?> ithClass = Class.forName("net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket$InteractTypeHandler");
            Class<?> handlerClass = Class.forName("net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket$Handler");
            attackHandler = java.lang.reflect.Proxy.newProxyInstance(
                    ithClass.getClassLoader(),
                    new Class<?>[]{ithClass},
                    (proxy, method, args) -> {
                        if ("handle".equals(method.getName()) && args.length == 1) {
                            Object handler = args[0];
                            handlerClass.getMethod("attack").invoke(handler);
                            return null;
                        }
                        if ("getType".equals(method.getName())) {
                            Field typeEnum = Class.forName("net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket$InteractType")
                                    .getDeclaredField("ATTACK");
                            typeEnum.setAccessible(true);
                            return typeEnum.get(null);
                        }
                        return null;
                    }
            );
        }

        setField(PlayerInteractEntityC2SPacket.class, "type", packet, attackHandler);
        setField(PlayerInteractEntityC2SPacket.class, "entityId", packet, 1);
        setField(PlayerInteractEntityC2SPacket.class, "playerSneaking", packet, false);

        return packet;
    }
}
