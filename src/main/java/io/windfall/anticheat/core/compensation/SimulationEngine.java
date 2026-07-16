package io.windfall.anticheat.core.compensation;

import io.windfall.anticheat.core.physics.PhysicsConstants;
import io.windfall.anticheat.core.player.WindfallPlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SimulationEngine {

    private static final int MAX_SCENARIOS = 16;
    private static final double MATCH_THRESHOLD = 0.1;

    private final PingPongManager pingPongManager;
    private final LatencyCompensator latencyCompensator;

    public SimulationEngine(PingPongManager pingPongManager, LatencyCompensator latencyCompensator) {
        this.pingPongManager = pingPongManager;
        this.latencyCompensator = latencyCompensator;
    }

    public SimulationResult simulate(WindfallPlayer player, double actualX, double actualY, double actualZ) {
        int confirmedTick = pingPongManager.getConfirmedTick(player);
        int currentTick = pingPongManager.getCurrentTick(player);

        if (confirmedTick >= currentTick) {
            double deviation = calculateDeviation(player, actualX, actualY, actualZ);
            boolean matches = deviation <= MATCH_THRESHOLD;
            return new SimulationResult(0, deviation, 1, matches);
        }

        List<WorldChange> unconfirmedChanges = latencyCompensator.getUnconfirmedChanges(
            player.getUuid(), confirmedTick, currentTick);

        if (unconfirmedChanges.isEmpty()) {
            double deviation = calculateDeviation(player, actualX, actualY, actualZ);
            boolean matches = deviation <= MATCH_THRESHOLD;
            return new SimulationResult(0, deviation, 1, matches);
        }

        int changeCount = Math.min(unconfirmedChanges.size(), log2Floor(MAX_SCENARIOS));
        int scenarioCount = Math.min(1 << changeCount, MAX_SCENARIOS);

        double bestDeviation = Double.MAX_VALUE;
        int bestScenario = -1;

        for (int i = 0; i < scenarioCount; i++) {
            double deviation = simulateScenario(player, actualX, actualY, actualZ,
                unconfirmedChanges, i, changeCount);
            if (deviation < bestDeviation) {
                bestDeviation = deviation;
                bestScenario = i;
            }
        }

        boolean matches = bestDeviation <= MATCH_THRESHOLD;
        return new SimulationResult(bestScenario, bestDeviation, scenarioCount, matches);
    }

    private double simulateScenario(WindfallPlayer player, double actualX, double actualY, double actualZ,
                                     List<WorldChange> changes, int scenarioMask, int changeCount) {
        double predictedX = player.getLastX();
        double predictedY = player.getLastY();
        double predictedZ = player.getLastZ();
        double deltaY = player.getDeltaY();
        boolean onGround = player.isOnGround();

        double gravityMod = 1.0;
        double airDragMod = 1.0;
        double velX = 0, velY = 0, velZ = 0;

        for (int bit = 0; bit < changeCount; bit++) {
            boolean changeApplied = (scenarioMask & (1 << bit)) != 0;
            if (!changeApplied) continue;

            WorldChange change = changes.get(bit);
            switch (change.getType()) {
                case BLOCK_BREAK: {
                    if (onGround && isBlockBelowPlayer(predictedX, predictedY, predictedZ,
                            change.getBlockX(), change.getBlockY(), change.getBlockZ())) {
                        onGround = false;
                    }
                    break;
                }
                case BLOCK_PLACE: {
                    if (!onGround && isBlockAtFeet(predictedX, predictedY, predictedZ,
                            change.getBlockX(), change.getBlockY(), change.getBlockZ())) {
                        onGround = true;
                        predictedY = change.getBlockY() + 1.0;
                        deltaY = 0;
                    }
                    break;
                }
                case BLOCK_SHIFT: {
                    if (onGround && isBlockBelowPlayer(predictedX, predictedY, predictedZ,
                            change.getOldX(), change.getOldY(), change.getOldZ())) {
                        onGround = false;
                    }
                    if (!onGround && isBlockAtFeet(predictedX, predictedY, predictedZ,
                            change.getBlockX(), change.getBlockY(), change.getBlockZ())) {
                        onGround = true;
                        predictedY = change.getBlockY() + 1.0;
                        deltaY = 0;
                    }
                    break;
                }
                case VELOCITY: {
                    velX += change.getVelocityX();
                    velY += change.getVelocityY();
                    velZ += change.getVelocityZ();
                    break;
                }
                case POTION_EFFECT: {
                    gravityMod *= change.getGravityMod();
                    airDragMod *= change.getAirDragMod();
                    break;
                }
            }
        }

        double gravity = PhysicsConstants.GRAVITY * gravityMod;
        double airDrag = PhysicsConstants.AIR_DRAG * airDragMod;

        if (!onGround) {
            predictedY += (deltaY - gravity) * airDrag;
        }

        predictedX += velX;
        predictedY += velY;
        predictedZ += velZ;

        return Math.sqrt(
            (actualX - predictedX) * (actualX - predictedX) +
            (actualY - predictedY) * (actualY - predictedY) +
            (actualZ - predictedZ) * (actualZ - predictedZ)
        );
    }

    private double calculateDeviation(WindfallPlayer player, double actualX, double actualY, double actualZ) {
        double predictedX = player.getLastX() + player.getDeltaX();
        double predictedY = player.getLastY() + player.getDeltaY();
        double predictedZ = player.getLastZ() + player.getDeltaZ();

        return Math.sqrt(
            (actualX - predictedX) * (actualX - predictedX) +
            (actualY - predictedY) * (actualY - predictedY) +
            (actualZ - predictedZ) * (actualZ - predictedZ)
        );
    }

    public boolean needsSimulation(WindfallPlayer player) {
        int confirmedTick = pingPongManager.getConfirmedTick(player);
        int currentTick = pingPongManager.getCurrentTick(player);
        return confirmedTick < currentTick;
    }

    private static boolean isBlockBelowPlayer(double px, double py, double pz,
                                               int bx, int by, int bz) {
        int playerBlockY = (int) Math.floor(py - 0.01);
        return by == playerBlockY &&
            Math.abs(px - (bx + 0.5)) < 1.0 &&
            Math.abs(pz - (bz + 0.5)) < 1.0;
    }

    private static boolean isBlockAtFeet(double px, double py, double pz,
                                          int bx, int by, int bz) {
        int playerBlockY = (int) Math.floor(py);
        return by == playerBlockY &&
            Math.abs(px - (bx + 0.5)) < 1.0 &&
            Math.abs(pz - (bz + 0.5)) < 1.0;
    }

    private static int log2Floor(int n) {
        int bits = 0;
        while ((1 << bits) < n && bits < 30) bits++;
        return bits;
    }

    public static final class SimulationResult {
        public final int bestScenario;
        public final double bestDeviation;
        public final int scenarioCount;
        public final boolean matches;

        SimulationResult(int bestScenario, double bestDeviation, int scenarioCount, boolean matches) {
            this.bestScenario = bestScenario;
            this.bestDeviation = bestDeviation;
            this.scenarioCount = scenarioCount;
            this.matches = matches;
        }

        public boolean isMultiScenario() {
            return scenarioCount > 1;
        }
    }
}
