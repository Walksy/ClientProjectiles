package walksy.clientprojectiles.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import walksy.clientprojectiles.ClientProjectiles;

@Mixin(ClientPacketListener.class)
public class ClientConnectionMixin {

    @Unique
    private boolean suppressExplosionEffects = false;

    @Redirect(method = "handleExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"))
    private void redirectExplosionSound(final ClientLevel world, final double x, final double y, final double z, final SoundEvent sound, final SoundSource category, final float volume, final float pitch, final boolean useDistance) {
        this.suppressExplosionEffects = ClientProjectiles.explosionFromServer(x, y, z);
        if (!this.suppressExplosionEffects) {
            world.playLocalSound(x, y, z, sound, category, volume, pitch, useDistance);
        }
    }

    @Redirect(method = "handleExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void redirectExplosionParticle(final ClientLevel world, final ParticleOptions particle, final double x, final double y, final double z, final double vx, final double vy, final double vz) {
        if (!this.suppressExplosionEffects) {
            world.addParticle(particle, x, y, z, vx, vy, vz);
        }
    }

    @Redirect(method = "handleExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;trackExplosionEffects(Lnet/minecraft/world/phys/Vec3;FILnet/minecraft/util/random/WeightedList;)V"))
    private void redirectExplosionBlockParticles(final ClientLevel world, final Vec3 center, final float radius, final int blockCount, final WeightedList<ExplosionParticleInfo> pool) {
        if (!this.suppressExplosionEffects) {
            world.trackExplosionEffects(center, radius, blockCount, pool);
        }
    }
}
