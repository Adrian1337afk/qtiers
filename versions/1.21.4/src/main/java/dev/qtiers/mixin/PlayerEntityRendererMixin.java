package dev.qtiers.mixin;

import dev.qtiers.QTiersConfig;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    /** How far player nametags are raised above vanilla's spot, in blocks. */
    private static final double NAMETAG_RAISE = 0.2;

    /** Lifts player nametags a little so the tiers sit clear of the head. */
    @Inject(method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("TAIL"))
    private void qtiers$raiseNametag(AbstractClientPlayerEntity entity, PlayerEntityRenderState state, float tickProgress, CallbackInfo ci) {
        if (state.nameLabelPos != null && QTiersConfig.get().enabled) {
            state.nameLabelPos = state.nameLabelPos.add(0, NAMETAG_RAISE, 0);
        }
    }
}
