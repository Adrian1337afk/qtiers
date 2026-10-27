package dev.qtiers.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.qtiers.QTiersConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class PlayerEntityRendererMixin {
    /** How far player nametags are raised above vanilla's spot, in blocks. */
    private static final double NAMETAG_RAISE = 0.2;

    /** Shows your own nametag (with tiers) in third person (F5), like WhoAmI. Vanilla hides it for the camera entity. */
    @ModifyReturnValue(method = "shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z", at = @At("RETURN"))
    private boolean qtiers$showOwnName(boolean original, Avatar entity, double distanceToCameraSq) {
        if (original || !QTiersConfig.get().showOwnNametag) return original;
        Minecraft client = Minecraft.getInstance();
        return entity == client.player
                && entity == client.getCameraEntity()
                && !client.options.getCameraType().isFirstPerson()
                && Minecraft.renderNames()
                && !entity.isInvisible();
    }

    /** Lifts player nametags a little so the tiers sit clear of the head. */
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void qtiers$raiseNametag(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        if (state.nameTagAttachment != null && QTiersConfig.get().enabled) {
            state.nameTagAttachment = state.nameTagAttachment.add(0, NAMETAG_RAISE, 0);
        }
    }
}
