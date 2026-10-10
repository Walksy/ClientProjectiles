package walksy.clientprojectiles;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import main.walksy.lib.core.platform.WLRegistries;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public class EventBuffer {
    private static final int EXPIRY_TICKS = 20;
    private static final double RANGE = 25.0;
    private final Map<String, Integer> soundEntries = new HashMap<>();
    private final Map<Integer, ArrayDeque<Integer>> worldEventEntries = new HashMap<>();
    private final List<EntityEntry> entityEntries = new ArrayList<>();
    private final Map<Integer, Integer> activeEntityIds = new HashMap<>();
    private final List<ExplosionEntry> explosionEntries = new ArrayList<>();
    private final Map<Integer, ClientboundAddEntityPacket> pendingArrowPackets = new HashMap<>();
    private final Map<Integer, Vec3> waitingArrowLandedPositions = new HashMap<>();
    private final List<SimulatedArrow> simulatedArrows = new ArrayList<>();
    private final Map<Integer, Integer> hitEntityBeforePacket = new HashMap<>();
    private final Map<UUID, Integer> suppressedSpawnUuids = new HashMap<>();
    private final ServerProjectileSync sync;
    private int currentTick = 0;

    public EventBuffer(final ServerProjectileSync sync) {
        this.sync = sync;
    }

    public void tick(final Consumer<Integer> onEntityExpired) {
        this.currentTick++;
        this.soundEntries.values().removeIf(t -> this.currentTick - t >= EXPIRY_TICKS);
        this.worldEventEntries.values().forEach(q -> q.removeIf(t -> this.currentTick - t >= EXPIRY_TICKS));
        this.worldEventEntries.entrySet().removeIf(e -> e.getValue().isEmpty());
        this.entityEntries.removeIf(e -> this.currentTick - e.spawnTick() >= EXPIRY_TICKS);
        this.activeEntityIds.entrySet().removeIf(e -> {
            if (this.currentTick - e.getValue() >= 150) {
                onEntityExpired.accept(e.getKey());
                this.pendingArrowPackets.remove(e.getKey());
                this.waitingArrowLandedPositions.remove(e.getKey());
                return true;
            }
            return false;
        });
        this.explosionEntries.removeIf(e -> this.currentTick - e.spawnTick() >= EXPIRY_TICKS);
        this.hitEntityBeforePacket.entrySet().removeIf(e -> this.currentTick - e.getValue() >= EXPIRY_TICKS);
        this.suppressedSpawnUuids.entrySet().removeIf(e -> this.currentTick - e.getValue() >= EXPIRY_TICKS);
    }

    public boolean isDuplicateSpawn(final ClientboundAddEntityPacket packet) {
        return this.suppressedSpawnUuids.containsKey(packet.getUUID());
    }

    public void addSound(final SoundEvent sound) {
        this.soundEntries.put(WLRegistries.soundId(sound).getPath(), this.currentTick);
    }

    public boolean tryConsumeSound(final Holder<SoundEvent> sound) {
        // Some servers send sounds inline with a mangled id (e.g. "minecraftentity.ender_pearl.throw") and can send
        // more than one copy, so match on the path suffix and swallow every copy while the client-played sound is recent.
        final String id = WLRegistries.soundId(sound.value()).toString().replace(":", "");
        for (final String path : this.soundEntries.keySet()) {
            if (id.endsWith(path)) {
                return true;
            }
        }
        return false;
    }

    public void addWorldEvent(final int eventId) {
        this.worldEventEntries.computeIfAbsent(eventId, k -> new ArrayDeque<>()).addLast(this.currentTick);
    }

    public boolean tryConsumeWorldEvent(final int eventId) {
        final ArrayDeque<Integer> queue = this.worldEventEntries.get(eventId);
        if (queue == null || queue.isEmpty()) {
            return false;
        }
        queue.pollFirst();
        if (queue.isEmpty()) {
            this.worldEventEntries.remove(eventId);
        }
        return true;
    }

    public void addEntity(final Projectile entity) {
        this.entityEntries.add(new EntityEntry(entity.getId(), entity.getType(), entity.getX(), entity.getY(), entity.getZ(), this.currentTick));
        this.activeEntityIds.put(entity.getId(), this.currentTick);
        this.sync.onClientSpawn(entity);
    }

    public boolean containsEntity(final int id) {
        return this.activeEntityIds.containsKey(id);
    }

    public void removeEntity(final int id) {
        this.activeEntityIds.remove(id);
        this.pendingArrowPackets.remove(id);
        this.waitingArrowLandedPositions.remove(id);
    }

    public boolean tryConsumeEntitySpawn(final ClientboundAddEntityPacket packet) {
        final EntityType<?> type = packet.getType();
        if (type == EntityTypes.ARROW || type == EntityTypes.SPECTRAL_ARROW) {
            return false;
        }
        if (this.sync.retrack(packet)) {
            this.suppressedSpawnUuids.put(packet.getUUID(), this.currentTick);
            return true;
        }
        final double x = packet.getX();
        final double y = packet.getY();
        final double z = packet.getZ();
        final Iterator<EntityEntry> iter = this.entityEntries.iterator();
        while (iter.hasNext()) {
            final EntityEntry entry = iter.next();
            if (this.matchesEntity(entry, type, x, y, z)) {
                iter.remove();
                this.suppressedSpawnUuids.put(packet.getUUID(), this.currentTick);
                this.sync.track(entry.id(), packet);
                return true;
            }
        }
        return false;
    }

    public boolean tryMatchAndStoreArrow(final ClientboundAddEntityPacket packet) {
        final EntityType<?> type = packet.getType();
        if (type != EntityTypes.ARROW && type != EntityTypes.SPECTRAL_ARROW) {
            return false;
        }
        final double x = packet.getX();
        final double y = packet.getY();
        final double z = packet.getZ();
        final Iterator<EntityEntry> iter = this.entityEntries.iterator();
        while (iter.hasNext()) {
            final EntityEntry entry = iter.next();
            if (this.matchesEntity(entry, type, x, y, z)) {
                iter.remove();
                if (this.activeEntityIds.containsKey(entry.id())) {
                    this.pendingArrowPackets.put(entry.id(), packet);
                    this.suppressedSpawnUuids.put(packet.getUUID(), this.currentTick);
                    this.sync.track(entry.id(), packet);
                    return true;
                }
                if (this.hitEntityBeforePacket.remove(entry.id()) != null) {
                    this.simulatedArrows.add(new SimulatedArrow(packet));
                    this.suppressedSpawnUuids.put(packet.getUUID(), this.currentTick);
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    public ClientboundAddEntityPacket consumePendingArrowPacket(final int clientId) {
        return this.pendingArrowPackets.remove(clientId);
    }

    public void markArrowLanded(final int clientId, final Vec3 pos) {
        this.waitingArrowLandedPositions.put(clientId, pos);
    }

    public void startSimulation(final int clientId) {
        final ClientboundAddEntityPacket packet = this.pendingArrowPackets.remove(clientId);
        this.waitingArrowLandedPositions.remove(clientId);
        if (packet == null) {
            this.hitEntityBeforePacket.put(clientId, this.currentTick);
            return;
        }
        this.simulatedArrows.add(new SimulatedArrow(packet));
    }

    public List<ArrowToSpawn> drainReadyLandedArrows() {
        if (this.pendingArrowPackets.isEmpty() || this.waitingArrowLandedPositions.isEmpty()) {
            return List.of();
        }
        final List<ArrowToSpawn> result = new ArrayList<>();
        final Iterator<Map.Entry<Integer, Vec3>> iter = this.waitingArrowLandedPositions.entrySet().iterator();
        while (iter.hasNext()) {
            final Map.Entry<Integer, Vec3> e = iter.next();
            final ClientboundAddEntityPacket packet = this.pendingArrowPackets.remove(e.getKey());
            if (packet != null) {
                iter.remove();
                this.activeEntityIds.remove(e.getKey());
                result.add(new ArrowToSpawn(e.getKey(), packet, e.getValue(), Vec3.ZERO));
            }
        }
        return result;
    }

    public List<ArrowToSpawn> tickSimulations(final ClientLevel world) {
        if (this.simulatedArrows.isEmpty()) {
            return List.of();
        }
        final List<ArrowToSpawn> result = new ArrayList<>();
        final Iterator<SimulatedArrow> iter = this.simulatedArrows.iterator();
        while (iter.hasNext()) {
            final SimulatedArrow sim = iter.next();
            sim.ticksAlive++;
            if (sim.ticksAlive > SimulatedArrow.MAX_TICKS) {
                iter.remove();
                continue;
            }
            final Vec3 newVel = new Vec3(sim.vel.x * 0.99, sim.vel.y * 0.99 - 0.05, sim.vel.z * 0.99);
            final Vec3 newPos = sim.pos.add(newVel);
            final BlockHitResult blockHit = world.clip(new ClipContext(sim.pos, newPos, Block.COLLIDER, Fluid.NONE, CollisionContext.empty()));
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                iter.remove();
                result.add(new ArrowToSpawn(-1, sim.packet, blockHit.getLocation(), Vec3.ZERO));
                continue;
            }
            final AABB searchBox = new AABB(sim.pos, newPos).inflate(0.5);
            boolean hitEntity = false;
            for (final LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class, searchBox, e -> true)) {
                if (entity.getBoundingBox().inflate(0.3).clip(sim.pos, newPos).isPresent()) {
                    hitEntity = true;
                    break;
                }
            }
            if (hitEntity) {
                iter.remove();
                continue;
            }
            sim.pos = newPos;
            sim.vel = newVel;
        }
        return result;
    }

    private boolean matchesEntity(final EntityEntry entry, final EntityType<?> type, final double x, final double y, final double z) {
        if (entry.type() != type) {
            return false;
        }
        final double dx = entry.x() - x;
        final double dy = entry.y() - y;
        final double dz = entry.z() - z;
        final double distSq = dx * dx + dy * dy + dz * dz;
        return distSq <= RANGE;
    }

    public void addExplosion(final double x, final double y, final double z) {
        this.explosionEntries.add(new ExplosionEntry(x, y, z, this.currentTick));
    }

    public boolean tryConsumeExplosion(final double x, final double y, final double z) {
        final Iterator<ExplosionEntry> iter = this.explosionEntries.iterator();
        while (iter.hasNext()) {
            final ExplosionEntry entry = iter.next();
            final double dx = entry.x() - x;
            final double dy = entry.y() - y;
            final double dz = entry.z() - z;
            if (dx * dx + dy * dy + dz * dz <= RANGE) {
                iter.remove();
                return true;
            }
        }
        return false;
    }

    public Map<Integer, Integer> getActiveEntityIds() {
        return this.activeEntityIds;
    }

    public record ArrowToSpawn(int clientId, ClientboundAddEntityPacket packet, Vec3 pos, Vec3 vel) {}

    static final class SimulatedArrow {
        public static final int MAX_TICKS = 80;
        public final ClientboundAddEntityPacket packet;
        public Vec3 pos;
        public Vec3 vel;
        public int ticksAlive = 0;

        public SimulatedArrow(final ClientboundAddEntityPacket packet) {
            this.packet = packet;
            this.pos = new Vec3(packet.getX(), packet.getY(), packet.getZ());
            this.vel = packet.getMovement();
        }
    }

    private record EntityEntry(int id, EntityType<?> type, double x, double y, double z, int spawnTick) {}
    private record ExplosionEntry(double x, double y, double z, int spawnTick) {}
}
