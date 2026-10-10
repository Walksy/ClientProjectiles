package walksy.clientprojectiles.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import walksy.clientprojectiles.ClientProjectiles;
import walksy.clientprojectiles.interactions.ProjectileCollideClient;

@Mixin(AbstractThrownPotion.class)
public abstract class PotionEntityMixin extends ThrowableItemProjectile implements ProjectileCollideClient {

    public PotionEntityMixin(EntityType<? extends ThrowableItemProjectile> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    @Inject(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/throwableitemprojectile/AbstractThrownPotion;level()Lnet/minecraft/world/level/Level;"))
    public void collide(HitResult hitResult, CallbackInfo ci) {
        if (this.level() instanceof ClientLevel clientWorld) {
            PotionContents potionContentsComponent = (PotionContents)this.getItem().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            int i = potionContentsComponent.potion()
                .isPresent() && ((Potion)((Holder)potionContentsComponent.potion().get()).value())
                .hasInstantEffects() ? 2007 : 2002;
            ClientProjectiles.playWorldEventFromClientInteraction(clientWorld, i, this.blockPosition(), potionContentsComponent.getColor());
            ClientProjectiles.removeFromWorldIfWithinBuffer(this.getId());
        }
    }
}
