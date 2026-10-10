package walksy.clientprojectiles.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import walksy.clientprojectiles.config.ClientProjectilesConfig;
import walksy.clientprojectiles.factory.ProjectileFactory;
import walksy.clientprojectiles.interactions.ProjectileUseItemClient;

import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;

@Mixin(BowItem.class)
public abstract class BowItemMixin extends ProjectileWeaponItem implements ProjectileUseItemClient {

    public BowItemMixin(Properties settings) {
        super(settings);
    }

    @Shadow
    protected abstract void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float speed, float divergence, float yaw, @Nullable LivingEntity target);

    @Inject(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BowItem;draw(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)Ljava/util/List;"), cancellable = true)
    public void shootBow(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        if (world instanceof ClientLevel clientWorld && ClientProjectilesConfig.modEnabled && ClientProjectilesConfig.bow) {
            List<ItemStack> list = draw(stack, user.getProjectile(stack), user);
            if (!list.isEmpty()) {
                int i = this.getUseDuration(stack, user) - remainingUseTicks;
                float f = BowItem.getPowerForTime(i);
                ProjectileFactory.shootAll(clientWorld, user, user.getUsedItemHand(), stack, list, f * 3.0F, 1.0F, f == 1.0F, null, this::shootProjectile);
            }
        }
    }
}
