package walksy.clientprojectiles.factory;

import org.jspecify.annotations.Nullable;
import walksy.clientprojectiles.ClientProjectiles;
import walksy.clientprojectiles.EventBuffer;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

public final class ProjectileFactory {
    public static final float DIVERGENCE = 0.0F;

    private ProjectileFactory() {
    }

    public static <T extends Projectile> ProjectileBuilder<T> createProjectile(final ClientProjectileCreator<T> creator, final ClientLevel world, final ItemStack stack, final LivingEntity shooter) {
        return new ProjectileBuilder<>(creator.create(world, shooter, stack), world, stack);
    }

    public static <T extends Projectile> ProjectileBuilder<T> createProjectile(final ClientPositionProjectileCreator<T> creator, final ClientLevel world, final ItemStack stack, final LivingEntity shooter) {
        final Vec3 spawnPos = shooter.getEyePosition().subtract(0.0D, 0.1D, 0.0D);
        return new ProjectileBuilder<>(creator.create(world, spawnPos, stack, shooter.getDirection()), world, stack);
    }

    public static <T extends Projectile> ProjectileBuilder<T> createProjectile(final T entity, final ClientLevel world, final ItemStack stack) {
        return new ProjectileBuilder<>(entity, world, stack);
    }

    public static void shootAll(final ClientLevel world, final LivingEntity shooter, final InteractionHand hand, final ItemStack stack, final List<ItemStack> projectiles, final float speed, final float divergence, final boolean critical, @Nullable final LivingEntity target, final ProjectileShootCallback shootCallback) {
        final float f = EnchantmentHelper.has(stack, EnchantmentEffectComponents.PROJECTILE_SPREAD) ? 10 : 0;
        final float g = projectiles.size() == 1 ? 0.0F : 2.0F * f / (float)(projectiles.size() - 1);
        final float h = (float)((projectiles.size() - 1) % 2) * g / 2.0F;
        float i = 1.0F;

        for (int j = 0; j < projectiles.size(); ++j) {
            final ItemStack itemStack = projectiles.get(j);
            if (!itemStack.isEmpty()) {
                final float k = h + i * (float)((j + 1) / 2) * g;
                i = -i;
                final int j2 = j;
                createProjectile(createArrowEntity(world, shooter, stack, itemStack, critical), world, itemStack)
                        .configure(p -> shootCallback.shoot(shooter, p, j2, speed, divergence, k, target))
                        .spawn(ClientProjectiles.BUFFER);
                final int dmg = itemStack.is(Items.FIREWORK_ROCKET) ? 3 : 1;
                stack.hurtAndBreak(dmg, shooter, hand.asEquipmentSlot());
                if (stack.isEmpty()) {
                    break;
                }
            }
        }
    }

    private static Projectile createArrowEntity(final ClientLevel world, final LivingEntity shooter, final ItemStack weaponStack, final ItemStack projectileStack, final boolean critical) {
        final Item item = projectileStack.getItem();
        final ArrowItem arrowItem = item instanceof ArrowItem a ? a : (ArrowItem) Items.ARROW;
        final AbstractArrow arrow = arrowItem.createArrow(world, projectileStack, shooter, weaponStack);
        if (critical) {
            arrow.setCritArrow(true);
        }
        return arrow;
    }

    @FunctionalInterface
    public interface ClientProjectileCreator<T extends Projectile> {
        T create(ClientLevel world, LivingEntity shooter, ItemStack stack);
    }

    @FunctionalInterface
    public interface ClientPositionProjectileCreator<T extends Projectile> {
        T create(ClientLevel world, Position pos, ItemStack stack, Direction direction);
    }

    @FunctionalInterface
    public interface ProjectileShootCallback {
        void shoot(LivingEntity shooter, Projectile projectile, int index, float speed, float divergence, float roll, @Nullable LivingEntity target);
    }

    public static final class ProjectileBuilder<T extends Projectile> {
        private final T entity;
        private final ClientLevel world;

        private ProjectileBuilder(final T entity, final ClientLevel world, final ItemStack stack) {
            this.entity = entity;
            this.world = world;
        }

        public ProjectileBuilder<T> withVelocity(final LivingEntity shooter, final float roll, final float power, final float divergence) {
            this.entity.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), roll, power, divergence);
            return this;
        }

        @Deprecated
        public ProjectileBuilder<T> withVelocity(final double x, final double y, final double z, final float power, final float divergence) {
            this.entity.shoot(x, y, z, power, divergence);
            return this;
        }

        public ProjectileBuilder<T> setOwner(final Entity owner) {
            this.entity.setOwner(owner);
            return this;
        }

        public ProjectileBuilder<T> configure(final Consumer<T> action) {
            action.accept(this.entity);
            return this;
        }

        public T spawn(EventBuffer buffer) {
            buffer.addEntity(this.entity);
            this.world.addEntity(this.entity);
            return this.entity;
        }
    }
}
