package walksy.clientprojectiles.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.item.WindChargeItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import walksy.clientprojectiles.ClientProjectiles;
import walksy.clientprojectiles.config.ClientProjectilesConfig;
import walksy.clientprojectiles.factory.ProjectileFactory;
import walksy.clientprojectiles.interactions.ProjectileUseItemClient;

@Mixin(WindChargeItem.class)
public class WindChargeItemMixin implements ProjectileUseItemClient {

    @Override
    @Inject(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"))
    public void use(final Level world, final Player user, final InteractionHand hand, final CallbackInfoReturnable<InteractionResult> cir) {
        if (world instanceof ClientLevel clientWorld && ClientProjectilesConfig.modEnabled && ClientProjectilesConfig.windCharge) {
            ProjectileFactory.createProjectile((w, shooter, stack) -> new WindCharge(user, world, user.position().x(), user.getEyePosition().y(), user.position().z()), clientWorld, user.getItemInHand(hand), user)
                    .setOwner(user)
                    .withVelocity(user, 0.0F, WindChargeItem.PROJECTILE_SHOOT_POWER, ProjectileFactory.DIVERGENCE)
                    .spawn(ClientProjectiles.BUFFER);
            ClientProjectiles.playSoundFromClientInteraction(clientWorld, user, SoundEvents.WIND_CHARGE_THROW, SoundSource.NEUTRAL);
        }
    }
}
