package io.windfall.anticheat.core.physics;

public class PredictionContext {
    private double motionX, motionY, motionZ;
    private double posX, posY, posZ;
    private boolean onGround;
    private boolean sprinting;
    private boolean sneaking;
    private boolean swimming;
    private boolean gliding;
    private boolean jumping;
    private double flySpeed;
    private double lastReportedX, lastReportedY, lastReportedZ;

    public PredictionContext(double x, double y, double z, double mx, double my, double mz,
                             boolean onGround, boolean sprinting, boolean sneaking, boolean swimming,
                             boolean gliding, boolean jumping) {
        this.posX = x; this.posY = y; this.posZ = z;
        this.motionX = mx; this.motionY = my; this.motionZ = mz;
        this.onGround = onGround;
        this.sprinting = sprinting; this.sneaking = sneaking;
        this.swimming = swimming; this.gliding = gliding;
        this.jumping = jumping;
        this.flySpeed = 0.05;
    }

    public double getMotionX() { return motionX; }
    public double getMotionY() { return motionY; }
    public double getMotionZ() { return motionZ; }
    public void setMotionX(double v) { this.motionX = v; }
    public void setMotionY(double v) { this.motionY = v; }
    public void setMotionZ(double v) { this.motionZ = v; }
    public double getPosX() { return posX; }
    public double getPosY() { return posY; }
    public double getPosZ() { return posZ; }
    public void setPosX(double v) { this.posX = v; }
    public void setPosY(double v) { this.posY = v; }
    public void setPosZ(double v) { this.posZ = v; }
    public boolean isOnGround() { return onGround; }
    public void setOnGround(boolean v) { this.onGround = v; }
    public boolean isSprinting() { return sprinting; }
    public boolean isSneaking() { return sneaking; }
    public boolean isSwimming() { return swimming; }
    public boolean isGliding() { return gliding; }
    public boolean isJumping() { return jumping; }
    public double getFlySpeed() { return flySpeed; }
    public void setFlySpeed(double v) { this.flySpeed = v; }
    public void setLastReportedPosition(double x, double y, double z) { this.lastReportedX = x; this.lastReportedY = y; this.lastReportedZ = z; }
    public double getLastReportedX() { return lastReportedX; }
    public double getLastReportedY() { return lastReportedY; }
    public double getLastReportedZ() { return lastReportedZ; }
}
