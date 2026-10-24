package dev.qtiers.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.qtiers.QTiersConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Shows your own nametag (with tiers) in third person (F5), like WhoAmI. Vanilla hides it for the camera entity. */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
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
}
