package io.windfall.anticheat.core.compensation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CompensatedEntities {
    private final Map<Integer, EntitySnapshot> entities = new ConcurrentHashMap<>();

    public void updateEntity(int entityId, double x, double y, double z) {
        entities.put(entityId, new EntitySnapshot(entityId, x, y, z, System.currentTimeMillis()));
    }

    public EntitySnapshot getEntity(int entityId) {
        return entities.get(entityId);
    }

    public void removeEntity(int entityId) {
        entities.remove(entityId);
    }

    public static class EntitySnapshot {
        private final int entityId;
        private final double x, y, z;
        private final long timestamp;
        public EntitySnapshot(int id, double x, double y, double z, long ts) {
            this.entityId = id; this.x = x; this.y = y; this.z = z; this.timestamp = ts;
        }
        public int getEntityId() { return entityId; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public long getTimestamp() { return timestamp; }
    }
}
