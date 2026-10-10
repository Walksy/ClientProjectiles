package walksy.clientprojectiles.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import walksy.clientprojectiles.ClientProjectiles;

@Mixin(ClientPacketListener.class)
public class ServerProjectileSyncMixin {

    @Unique
    private Entity motionShadow;

    @WrapOperation(method = {"handleTeleportEntity", "handleEntityPositionSync"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getEntity(I)Lnet/minecraft/world/entity/Entity;"))
    private Entity resolvePositionShadow(final ClientLevel level, final int id, final Operation<Entity> original) {
        final Entity entity = original.call(level, id);
        return entity != null ? entity : ClientProjectiles.SYNC.shadow(id);
    }

    @WrapOperation(method = "handleMoveEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundMoveEntityPacket;getEntity(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;"))
    private Entity resolveMoveShadow(final ClientboundMoveEntityPacket packet, final Level level, final Operation<Entity> original) {
        final Entity entity = original.call(packet, level);
        return entity != null ? entity : ClientProjectiles.SYNC.shadow(((MoveEntityPacketAccessor) packet).getEntityId());
    }

    @WrapOperation(method = "handleSetEntityMotion", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getEntity(I)Lnet/minecraft/world/entity/Entity;"))
    private Entity resolveMotionShadow(final ClientLevel level, final int id, final Operation<Entity> original) {
        final Entity entity = original.call(level, id);
        if (entity != null) {
            return entity;
        }
        this.motionShadow = ClientProjectiles.SYNC.shadow(id);
        return this.motionShadow;
    }

    @Inject(method = "handleSetEntityMotion", at = @At("RETURN"))
    private void onSetEntityMotion(final ClientboundSetEntityMotionPacket packet, final CallbackInfo ci) {
        final Entity shadow = this.motionShadow;
        this.motionShadow = null;
        if (shadow != null) {
            ClientProjectiles.SYNC.onServerMotion(shadow);
        }
    }

    @Inject(method = "handleRemoveEntities", at = @At("HEAD"))
    private void onRemoveEntities(final ClientboundRemoveEntitiesPacket packet, final CallbackInfo ci) {
        if (Minecraft.getInstance().isSameThread()) {
            for (final int id : packet.getEntityIds()) {
                ClientProjectiles.SYNC.onServerRemoved(id);
            }
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void onMovePlayer(final ClientboundPlayerPositionPacket packet, final CallbackInfo ci) {
        if (Minecraft.getInstance().isSameThread()) {
            ClientProjectiles.SYNC.onTeleport();
        }
    }
}
