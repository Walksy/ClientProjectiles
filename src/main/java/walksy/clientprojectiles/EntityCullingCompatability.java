package walksy.clientprojectiles;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class EntityCullingCompatability {
    private static final String ENTITY_CULLING_MOD_ID = "entityculling";
    private static final String[] CULLABLE_CLASSES = {"dev.tr7zw.entityculling.access.Cullable", "dev.tr7zw.entityculling.versionless.access.Cullable"};
    private static boolean resolved;
    private static Class<?> cullable;
    private static MethodHandle setCulled;
    private static MethodHandle setOutOfCamera;

    private EntityCullingCompatability() {
    }

    public static void tick(final Minecraft minecraft) {
        if (minecraft.level == null || !resolve()) {
            return;
        }
        for (final int id : ClientProjectiles.BUFFER.getActiveEntityIds().keySet()) {
            final Entity entity = minecraft.level.getEntity(id);
            if (cullable.isInstance(entity)) {
                try {
                    setCulled.invoke(entity, false);
                    setOutOfCamera.invoke(entity, false);
                } catch (final Throwable e) {
                    cullable = null;
                    return;
                }
            }
        }
    }

    private static boolean resolve() {
        if (!resolved) {
            resolved = true;
            if (FabricLoader.getInstance().isModLoaded(ENTITY_CULLING_MOD_ID)) {
                final MethodType setter = MethodType.methodType(void.class, boolean.class);
                for (final String name : CULLABLE_CLASSES) {
                    try {
                        final Class<?> type = Class.forName(name, false, EntityCullingCompatability.class.getClassLoader());
                        setCulled = MethodHandles.publicLookup().findVirtual(type, "setCulled", setter);
                        setOutOfCamera = MethodHandles.publicLookup().findVirtual(type, "setOutOfCamera", setter);
                        cullable = type;
                        break;
                    } catch (final ReflectiveOperationException ignored) {
                    }
                }
            }
        }
        return cullable != null;
    }
}
