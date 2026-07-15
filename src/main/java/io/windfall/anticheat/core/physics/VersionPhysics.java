package io.windfall.anticheat.core.physics;

import io.windfall.anticheat.core.player.WindfallPlayer;

public class VersionPhysics {
    private final int serverProtocol;
    public VersionPhysics(int serverProtocol) { this.serverProtocol = serverProtocol; }

    public double getJumpVelocity(WindfallPlayer player) {
        return PhysicsConstants.JUMP_VELOCITY;
    }

    public double getMovementSpeed(WindfallPlayer player, boolean sprinting, boolean sneaking) {
        if (sneaking) return PhysicsConstants.SNEAK_SPEED;
        return sprinting ? PhysicsConstants.SPRINT_SPEED : PhysicsConstants.WALK_SPEED;
    }

    public double getBlockFriction(int blockId) {
        return 0.6;
    }

    public boolean isColliding(BoundingBox playerBB, double dx, double dy, double dz, Object world) {
        return false;
    }
}
