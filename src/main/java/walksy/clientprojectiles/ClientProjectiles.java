package walksy.clientprojectiles;

import net.fabricmc.api.ModInitializer;
import main.walksy.lib.api.event.WalksyLibEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import walksy.clientprojectiles.config.ClientProjectilesConfig;
import walksy.clientprojectiles.mixin.PersistentProjectileEntityAccessor;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;

public class ClientProjectiles implements ModInitializer {
    public static final ServerProjectileSync SYNC = new ServerProjectileSync();
    public static final EventBuffer BUFFER = new EventBuffer(SYNC);

    @Override
    public void onInitialize() {
        WalksyLibEvents.START_CLIENT_TICK.register(EntityCullingCompatability::tick);
        WalksyLibEvents.START_RECEIVE_PACKET_MAIN.register((packet, listener, cancellable) -> {
            if (packet instanceof final ClientboundSoundPacket sound) {
                if (playSoundFromServer(sound.getSound())) {
                    cancellable.cancel();
                }
            } else if (packet instanceof final ClientboundSoundEntityPacket sound) {
                if (playSoundFromServer(sound.getSound())) {
                    cancellable.cancel();
                }
            } else if (packet instanceof final ClientboundAddEntityPacket spawn) {
                if (isDuplicateSpawn(spawn) || tryMatchAndStoreArrow(spawn) || shouldSuppressEntitySpawn(spawn)) {
                    cancellable.cancel();
                }
            } else if (packet instanceof final ClientboundLevelEventPacket event) {
                if (worldEventFromServer(event.getType())) {
                    cancellable.cancel();
                }
            }
        });
        WalksyLibEvents.END_CLIENT_TICK.register(client -> {
            BUFFER.tick(id -> {
                if (client.level != null) {
                    client.level.removeEntity(id, Entity.RemovalReason.DISCARDED);
                }
            });
            SYNC.tick(client.level);
            if (client.level == null) {
                return;
            }
            for (final EventBuffer.ArrowToSpawn ats : BUFFER.drainReadyLandedArrows()) {
                addServerArrowToWorld(client.level, ats.packet(), ats.pos(), ats.vel());
                client.level.removeEntity(ats.clientId(), Entity.RemovalReason.DISCARDED);
            }
            for (final EventBuffer.ArrowToSpawn ats : BUFFER.tickSimulations(client.level)) {
                addServerArrowToWorld(client.level, ats.packet(), ats.pos(), ats.vel());
            }
            if (ClientProjectilesConfig.modEnabled && ClientProjectilesConfig.pearlWindChargeCollision) {
                this.checkClientProjectileCollisions(client.level);
            }
        });
    }

    private void checkClientProjectileCollisions(final ClientLevel world) {
        final List<WindCharge> windCharges = new ArrayList<>();
        final List<ThrownEnderpearl> enderPearls = new ArrayList<>();
        for (final Entity entity : world.entitiesForRendering()) {
            if (!BUFFER.containsEntity(entity.getId())) {
                continue;
            }
            if (entity instanceof WindCharge windCharge) {
                windCharges.add(windCharge);
            } else if (entity instanceof ThrownEnderpearl enderPearl) {
                enderPearls.add(enderPearl);
            }
        }
        if (windCharges.isEmpty() || enderPearls.isEmpty()) {
            return;
        }
        for (final WindCharge windCharge : windCharges) {
            if (!windCharge.isAlive()) {
                continue;
            }
            final AABB wcAABB = windCharge.getBoundingBox().expandTowards(windCharge.getDeltaMovement());
            for (final ThrownEnderpearl enderPearl : enderPearls) {
                if (!enderPearl.isAlive()) {
                    continue;
                }
                if (wcAABB.intersects(enderPearl.getBoundingBox().expandTowards(enderPearl.getDeltaMovement()))) {
                    removeFromWorld(windCharge.getId());
                    removeFromWorld(enderPearl.getId());
                    break;
                }
            }
        }
    }

    public static void playSoundFromClientInteraction(final ClientLevel world, final LivingEntity user, final SoundEvent event, final SoundSource category) {
        BUFFER.addSound(event);
        world.playLocalSound(user.getX(), user.getY(), user.getZ(), event, category, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F), false);
    }

    public static boolean playSoundFromServer(final Holder<SoundEvent> sound) {
        return BUFFER.tryConsumeSound(sound);
    }

    public static void playWorldEventFromClientInteraction(final ClientLevel world, final int eventId, final BlockPos pos, final int data) {
        BUFFER.addWorldEvent(eventId);
        world.levelEvent(eventId, pos, data);
    }

    public static boolean worldEventFromServer(final int eventId) {
        return BUFFER.tryConsumeWorldEvent(eventId);
    }

    public static void registerClientExplosion(final double x, final double y, final double z) {
        BUFFER.addExplosion(x, y, z);
    }

    public static boolean explosionFromServer(final double x, final double y, final double z) {
        return BUFFER.tryConsumeExplosion(x, y, z);
    }

    public static boolean shouldSuppressEntitySpawn(final ClientboundAddEntityPacket packet) {
        return BUFFER.tryConsumeEntitySpawn(packet);
    }

    public static boolean isDuplicateSpawn(final ClientboundAddEntityPacket packet) {
        return BUFFER.isDuplicateSpawn(packet);
    }

    public static boolean tryMatchAndStoreArrow(final ClientboundAddEntityPacket packet) {
        return BUFFER.tryMatchAndStoreArrow(packet);
    }

    public static boolean containsWithinBuffer(final int id) {
        return BUFFER.containsEntity(id);
    }

    public static void removeFromWorldIfWithinBuffer(final int id) {
        if (BUFFER.containsEntity(id)) {
            removeFromWorld(id);
        }
    }

    public static ClientboundAddEntityPacket consumePendingArrow(final int clientId) {
        return BUFFER.consumePendingArrowPacket(clientId);
    }

    public static void markArrowLanded(final int clientId, final Vec3 pos) {
        BUFFER.markArrowLanded(clientId, pos);
    }

    public static void startSimulatingArrow(final int clientId) {
        BUFFER.startSimulation(clientId);
    }

    public static void addServerArrowToWorld(final ClientLevel world, final ClientboundAddEntityPacket packet, final Vec3 pos, final Vec3 vel) {
        final Entity entity = packet.getType().create(world, EntitySpawnReason.LOAD);
        if (entity == null) {
            return;
        }
        entity.recreateFromPacket(packet);
        entity.setPos(pos);
        entity.setDeltaMovement(vel);
        if (vel.equals(Vec3.ZERO) && entity instanceof PersistentProjectileEntityAccessor ppe) {
            ppe.invokeSetInGround(true);
        }
        world.addEntity(entity);
        entity.xo = pos.x;
        entity.yo = pos.y;
        entity.zo = pos.z;
    }

    public static void addServerArrowToWorld(final ClientLevel world, final ClientboundAddEntityPacket packet, final Vec3 pos) {
        addServerArrowToWorld(world, packet, pos, Vec3.ZERO);
    }

    public static void removeFromWorld(final int id) {
        BUFFER.removeEntity(id);
        final ClientLevel world = Minecraft.getInstance().level;
        if (world != null) {
            world.removeEntity(id, Entity.RemovalReason.DISCARDED);
        }
    }
}
