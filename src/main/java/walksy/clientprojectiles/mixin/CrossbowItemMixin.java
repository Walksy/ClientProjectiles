package walksy.clientprojectiles.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import walksy.clientprojectiles.config.ClientProjectilesConfig;
import walksy.clientprojectiles.factory.ProjectileFactory;
import walksy.clientprojectiles.interactions.ProjectileUseItemClient;

@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin implements ProjectileUseItemClient {

    @Shadow
    protected abstract void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float speed, float divergence, float yaw, @Nullable LivingEntity target);

    @Override
    @Inject(method = "performShooting", at = @At(value = "HEAD"), cancellable = true)
    public void shootCrossbow(final Level world, final LivingEntity shooter, final InteractionHand hand, final ItemStack stack, final float speed, final float divergence, final LivingEntity target, final CallbackInfo ci) {
        if (world instanceof ClientLevel clientWorld && ClientProjectilesConfig.modEnabled && ClientProjectilesConfig.crossbow) {
            final ChargedProjectiles chargedProjectilesComponent = (ChargedProjectiles)stack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
            if (chargedProjectilesComponent != null && !chargedProjectilesComponent.isEmpty()) {
                ProjectileFactory.shootAll(clientWorld, shooter, hand, stack, chargedProjectilesComponent.itemCopies(), speed, divergence, shooter instanceof Player, target, this::shootProjectile);
            }
            ci.cancel();
        }
    }
}
