package walksy.clientprojectiles.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import walksy.clientprojectiles.ClientProjectiles;
import walksy.clientprojectiles.interactions.ProjectileCollideClient;

@Mixin(ThrownExperienceBottle.class)
public abstract class ExperienceBottleEntityMixin extends ThrowableItemProjectile implements ProjectileCollideClient {

    public ExperienceBottleEntityMixin(EntityType<? extends ThrowableItemProjectile> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    @Inject(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/throwableitemprojectile/ThrownExperienceBottle;level()Lnet/minecraft/world/level/Level;"))
    public void collide(HitResult hitResult, CallbackInfo ci) {
        if (this.level() instanceof ClientLevel clientWorld && ClientProjectiles.containsWithinBuffer(this.getId())) {
            ClientProjectiles.playWorldEventFromClientInteraction(clientWorld, 2002, this.blockPosition(), -13083194);
            ClientProjectiles.removeFromWorld(this.getId());
        }
    }
}
