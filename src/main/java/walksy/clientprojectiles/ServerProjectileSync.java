package walksy.clientprojectiles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ServerProjectileSync {
    private static final int HISTORY_TICKS = 200;
    private static final int IDLE_TICKS = 60;
    private static final int SPAWN_MATCH_TICKS = 2;
    private static final double IMPULSE_THRESHOLD_SQ = 0.08 * 0.08;
    private static final int LANDING_TELEPORT_TICKS = 2;
    private final Map<Integer, Deque<Vec3>> clientHistory = new HashMap<>();
    private final Map<Integer, Shadow> shadows = new HashMap<>();
    private final Map<UUID, Untracked> untracked = new HashMap<>();
    private @Nullable ClientLevel level;
    private int currentTick;
    private int lastTeleportTick = Integer.MIN_VALUE / 2;

    private record Untracked(int clientId, Vec3 offset) {}

    private static final class Shadow {
        private final Entity entity;
        private final int clientId;
        private final Deque<Vec3> history;
        private final Vec3 offset;
        private int lastUpdateTick;

        private Shadow(final Entity entity, final int clientId, final Deque<Vec3> history, final Vec3 offset, final int lastUpdateTick) {
            this.entity = entity;
            this.clientId = clientId;
            this.history = history;
            this.offset = offset;
            this.lastUpdateTick = lastUpdateTick;
        }
    }

    public void onClientSpawn(final Projectile projectile) {
        final Deque<Vec3> history = new ArrayDeque<>();
        history.addLast(projectile.getDeltaMovement());
        this.clientHistory.put(projectile.getId(), history);
    }

    public void track(final int clientId, final ClientboundAddEntityPacket packet) {
        final Deque<Vec3> history = this.clientHistory.get(clientId);
        if (history != null) {
            this.track(clientId, packet, history, spawnOffset(history, packet.getMovement()));
        }
    }

    private void track(final int clientId, final ClientboundAddEntityPacket packet, final Deque<Vec3> history, final Vec3 offset) {
        final ClientLevel world = Minecraft.getInstance().level;
        if (world == null) {
            return;
        }
        final Entity shadow = packet.getType().create(world, EntitySpawnReason.LOAD);
        if (shadow == null) {
            return;
        }
        shadow.recreateFromPacket(packet);
        shadow.setDeltaMovement(packet.getMovement());
        this.shadows.put(shadow.getId(), new Shadow(shadow, clientId, history, offset, this.currentTick));
    }

    public boolean retrack(final ClientboundAddEntityPacket packet) {
        final Untracked entry = this.untracked.remove(packet.getUUID());
        if (entry == null || !ClientProjectiles.containsWithinBuffer(entry.clientId())) {
            return false;
        }
        final Deque<Vec3> history = this.clientHistory.get(entry.clientId());
        if (history == null) {
            return false;
        }
        this.track(entry.clientId(), packet, history, entry.offset());
        return true;
    }

    public void onTeleport() {
        this.lastTeleportTick = this.currentTick;
    }

    public @Nullable Entity shadow(final int id) {
        final Shadow shadow = this.shadows.get(id);
        if (shadow == null) {
            return null;
        }
        shadow.lastUpdateTick = this.currentTick;
        return shadow.entity;
    }

    public void onServerMotion(final Entity entity) {
        final Shadow shadow = this.shadows.get(entity.getId());
        if (shadow == null || shadow.entity != entity || this.level == null) {
            return;
        }
        final Vec3 velocity = entity.getDeltaMovement();
        for (final Vec3 predicted : shadow.history) {
            if (velocity.distanceToSqr(predicted.add(shadow.offset)) <= IMPULSE_THRESHOLD_SQ) {
                return;
            }
        }
        this.handOver(shadow, this.level);
    }

    public void onServerRemoved(final int id) {
        final Shadow shadow = this.shadows.remove(id);
        if (shadow == null || !(shadow.entity instanceof ThrownEnderpearl)) {
            return;
        }
        if (this.currentTick - this.lastTeleportTick <= LANDING_TELEPORT_TICKS) {
            ClientProjectiles.removeFromWorldIfWithinBuffer(shadow.clientId);
        } else {
            this.untracked.put(shadow.entity.getUUID(), new Untracked(shadow.clientId, shadow.offset));
        }
    }

    public void tick(@Nullable final ClientLevel world) {
        if (world != this.level) {
            this.clientHistory.clear();
            this.shadows.clear();
            this.untracked.clear();
            this.level = world;
        }
        if (world == null) {
            return;
        }
        this.currentTick++;
        final Map<Integer, Integer> active = ClientProjectiles.BUFFER.getActiveEntityIds();
        this.clientHistory.keySet().retainAll(active.keySet());
        this.untracked.values().removeIf(entry -> !active.containsKey(entry.clientId()));
        this.clientHistory.forEach((id, history) -> {
            final Entity entity = world.getEntity(id);
            if (entity != null) {
                history.addLast(entity.getDeltaMovement());
                while (history.size() > HISTORY_TICKS) {
                    history.pollFirst();
                }
            }
        });
        this.shadows.entrySet().removeIf(entry -> {
            final Shadow shadow = entry.getValue();
            return this.currentTick - shadow.lastUpdateTick > IDLE_TICKS
                || world.getEntity(entry.getKey()) != null
                || (shadow.entity instanceof AbstractArrow && !active.containsKey(shadow.clientId));
        });
    }

    private void handOver(final Shadow shadow, final ClientLevel world) {
        final Entity entity = shadow.entity;
        this.shadows.remove(entity.getId());
        ClientProjectiles.removeFromWorld(shadow.clientId);
        final Vec3 position = entity.getPositionCodec().getBase();
        entity.setPos(position);
        world.addEntity(entity);
        entity.xo = position.x;
        entity.yo = position.y;
        entity.zo = position.z;
    }

    private static Vec3 spawnOffset(final Deque<Vec3> history, final Vec3 spawnVelocity) {
        Vec3 offset = Vec3.ZERO;
        double closest = Double.MAX_VALUE;
        int checked = 0;
        for (final Vec3 predicted : history) {
            if (checked++ >= SPAWN_MATCH_TICKS) {
                break;
            }
            final double distance = spawnVelocity.distanceToSqr(predicted);
            if (distance < closest) {
                closest = distance;
                offset = spawnVelocity.subtract(predicted);
            }
        }
        return offset;
    }
}
