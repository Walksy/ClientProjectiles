package walksy.clientprojectiles.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import walksy.clientprojectiles.ClientProjectiles;
import walksy.clientprojectiles.config.ClientProjectilesConfig;
import walksy.clientprojectiles.factory.ProjectileFactory;
import walksy.clientprojectiles.interactions.ProjectileUseItemClient;

@Mixin(ExperienceBottleItem.class)
public class ExperienceBottleItemMixin implements ProjectileUseItemClient {

    @Unique
    private static final float POWER = 0.7F;

    @Override
    @Inject(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"))
    public void use(final Level world, final Player user, final InteractionHand hand, final CallbackInfoReturnable<InteractionResult> cir) {
        if (world instanceof ClientLevel clientWorld && ClientProjectilesConfig.modEnabled && ClientProjectilesConfig.expBottles) {
            ClientProjectiles.playSoundFromClientInteraction(clientWorld, user, SoundEvents.EXPERIENCE_BOTTLE_THROW, SoundSource.NEUTRAL);
            ProjectileFactory.createProjectile(ThrownExperienceBottle::new, clientWorld, user.getItemInHand(hand), user)
                    .withVelocity(user, -20.0F, POWER, ProjectileFactory.DIVERGENCE)
                    .spawn(ClientProjectiles.BUFFER);
        }
    }
}
