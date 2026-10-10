package walksy.clientprojectiles.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import walksy.clientprojectiles.ClientProjectiles;
import walksy.clientprojectiles.config.ClientProjectilesConfig;
import walksy.clientprojectiles.interactions.ProjectileCollideClient;

@Mixin(ThrownEnderpearl.class)
public abstract class EnderPearlEntityMixin extends ThrowableItemProjectile implements ProjectileCollideClient {

    public EnderPearlEntityMixin(final EntityType<? extends ThrowableItemProjectile> entityType, final Level world) {
        super(entityType, world);
    }

    @Override
    @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
    public void collide(final HitResult hitResult, final CallbackInfo ci) {
        if (this.level() instanceof ClientLevel && ClientProjectilesConfig.modEnabled && ClientProjectilesConfig.enderPearls) {
            ClientProjectiles.removeFromWorldIfWithinBuffer(this.getId());
            ci.cancel();
        }
    }
}
