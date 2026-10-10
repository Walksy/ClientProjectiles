package walksy.clientprojectiles.interactions;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public interface ProjectileUseItemClient {
    default void use(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {

    }

    default void shootCrossbow(Level world, LivingEntity shooter, InteractionHand hand, ItemStack stack, float speed, float divergence, LivingEntity target, CallbackInfo ci) {

    }
}
