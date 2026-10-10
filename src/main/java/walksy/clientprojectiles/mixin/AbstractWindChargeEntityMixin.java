package walksy.clientprojectiles.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import walksy.clientprojectiles.ClientProjectiles;
import walksy.clientprojectiles.interactions.ProjectileCollideClient;

@Mixin(AbstractWindCharge.class)
public abstract class AbstractWindChargeEntityMixin extends AbstractHurtingProjectile implements ProjectileCollideClient {

    @Unique
    private static final float POWER = 1.2F;

    protected AbstractWindChargeEntityMixin(EntityType<? extends AbstractHurtingProjectile> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    @Inject(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/hurtingprojectile/windcharge/AbstractWindCharge;level()Lnet/minecraft/world/level/Level;"))
    public void collide(HitResult hitResult, CallbackInfo ci) {
        if (this.level() instanceof ClientLevel clientWorld) {
            Vec3 pos = hitResult.getLocation();
            this.explode(clientWorld, pos.x(), pos.y(), pos.z(), WeightedList.of());
        }
    }

    @Override
    @Inject(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/hurtingprojectile/windcharge/AbstractWindCharge;level()Lnet/minecraft/world/level/Level;"))
    public void collideWithEntity(EntityHitResult entityHitResult, CallbackInfo ci) {
        if (this.level() instanceof ClientLevel clientWorld) {
            Vec3 pos = entityHitResult.getLocation();
            this.explode(clientWorld, pos.x(), pos.y(), pos.z(), WeightedList.of());
        }
    }

    @Override
    @Inject(method = "onHitBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/hurtingprojectile/windcharge/AbstractWindCharge;level()Lnet/minecraft/world/level/Level;"))
    public void collideWithBlock(BlockHitResult blockHitResult, CallbackInfo ci) {
        if (this.level() instanceof ClientLevel) {
            ClientProjectiles.removeFromWorldIfWithinBuffer(this.getId());
        }
    }

    @Unique
    private void explode(ClientLevel world, double x, double y, double z, WeightedList<ExplosionParticleInfo> blockParticles) {
        Vec3 vec3d = new Vec3(x, y, z);
        ClientProjectiles.registerClientExplosion(x, y, z);
        world.playLocalSound(vec3d.x(), vec3d.y(), vec3d.z(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.BLOCKS, 4.0F, (1.0F + (world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.2F) * 0.7F, false);
        world.addParticle(ParticleTypes.GUST_EMITTER_SMALL, vec3d.x(), vec3d.y(), vec3d.z(), (double) 1.0F, (double) 0.0F, (double) 0.0F);
        world.trackExplosionEffects(vec3d, POWER, 1, blockParticles);
        ClientProjectiles.removeFromWorldIfWithinBuffer(this.getId());
    }
}
