package walksy.clientprojectiles.interactions;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public interface ProjectileCollideClient {
    default void collide(HitResult hitResult, final CallbackInfo ci) {

    }

    default void collideWithEntity(EntityHitResult entityHitResult, CallbackInfo ci) {

    }

    default void collideWithBlock(BlockHitResult blockHitResult, CallbackInfo ci) {

    }
}
