package walksy.clientprojectiles.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import walksy.clientprojectiles.ClientProjectiles;
import walksy.clientprojectiles.config.ClientProjectilesConfig;
import walksy.clientprojectiles.factory.ProjectileFactory;
import walksy.clientprojectiles.interactions.ProjectileUseItemClient;

@Mixin(ThrowablePotionItem.class)
public abstract class ThrowablePotionItemMixin implements ProjectileUseItemClient {

    @Shadow
    public abstract Projectile asProjectile(Level world, Position pos, ItemStack stack, Direction direction);

    @Override
    @Inject(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"))
    public void use(final Level world, final Player user, final InteractionHand hand, final CallbackInfoReturnable<InteractionResult> cir) {
        if (world instanceof ClientLevel clientWorld && ClientProjectilesConfig.modEnabled) {
            final ThrowablePotionItem this$ = ThrowablePotionItem.class.cast(this);
            if (this$ instanceof SplashPotionItem && ClientProjectilesConfig.splashPotions) {
                ClientProjectiles.playSoundFromClientInteraction(clientWorld, user, SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS);
            } else if (this$ instanceof LingeringPotionItem && ClientProjectilesConfig.lingeringPotions) {
                ClientProjectiles.playSoundFromClientInteraction(clientWorld, user, SoundEvents.LINGERING_POTION_THROW, SoundSource.NEUTRAL);
            } else {
                return;
            }
            ProjectileFactory.createProjectile(this::asProjectile, clientWorld, user.getItemInHand(hand), user)
                    .setOwner(user)
                    .withVelocity(user, -20.0F, ThrowablePotionItem.PROJECTILE_SHOOT_POWER, ProjectileFactory.DIVERGENCE)
                    .spawn(ClientProjectiles.BUFFER);
        }
    }
}
