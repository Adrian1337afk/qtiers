package dev.qtiers.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.qtiers.QTiersConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    /** How far player nametags are raised above vanilla's spot, in blocks. */
    private static final double NAMETAG_RAISE = 0.2;

    /** Shows your own nametag (with tiers) in third person (F5), like WhoAmI. Vanilla hides it for the camera entity. */
    @ModifyReturnValue(method = "hasLabel(Lnet/minecraft/entity/PlayerLikeEntity;D)Z", at = @At("RETURN"))
    private boolean qtiers$showOwnName(boolean original, PlayerLikeEntity entity, double squaredDistance) {
        if (original || !QTiersConfig.get().showOwnNametag) return original;
        MinecraftClient client = MinecraftClient.getInstance();
        return entity == client.player
                && entity == client.getCameraEntity()
                && !client.options.getPerspective().isFirstPerson()
                && MinecraftClient.isHudEnabled()
                && !entity.isInvisible();
    }

    /** Lifts player nametags a little so the tiers sit clear of the head. */
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("TAIL"))
    private void qtiers$raiseNametag(PlayerLikeEntity entity, PlayerEntityRenderState state, float tickProgress, CallbackInfo ci) {
        if (state.nameLabelPos != null && QTiersConfig.get().enabled) {
            state.nameLabelPos = state.nameLabelPos.add(0, NAMETAG_RAISE, 0);
        }
    }
}
