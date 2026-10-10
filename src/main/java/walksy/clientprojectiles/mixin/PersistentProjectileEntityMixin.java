package walksy.clientprojectiles.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import walksy.clientprojectiles.ClientProjectiles;

@Mixin(AbstractArrow.class)
public abstract class PersistentProjectileEntityMixin extends Projectile {

    public PersistentProjectileEntityMixin(final EntityType<? extends Projectile> entityType, final Level world) {
        super(entityType, world);
    }

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void onArrowHitEntity(final EntityHitResult entityHitResult, final CallbackInfo ci) {
        if (this.level() instanceof ClientLevel && ClientProjectiles.containsWithinBuffer(this.getId())) {
            ClientProjectiles.startSimulatingArrow(this.getId());
            ClientProjectiles.removeFromWorld(this.getId());
            ci.cancel();
        }
    }

    @Inject(method = "onHitBlock", at = @At("RETURN"))
    private void onArrowHitBlock(final BlockHitResult blockHitResult, final CallbackInfo ci) {
        if (!(this.level() instanceof ClientLevel world)) {
            return;
        }
        if (!ClientProjectiles.containsWithinBuffer(this.getId())) {
            return;
        }
        final Vec3 stuckPos = new Vec3(this.getX(), this.getY(), this.getZ());
        final ClientboundAddEntityPacket serverPacket = ClientProjectiles.consumePendingArrow(this.getId());
        if (serverPacket != null) {
            ClientProjectiles.addServerArrowToWorld(world, serverPacket, stuckPos);
            ClientProjectiles.removeFromWorld(this.getId());
        } else {
            ClientProjectiles.markArrowLanded(this.getId(), stuckPos);
        }
    }
}
