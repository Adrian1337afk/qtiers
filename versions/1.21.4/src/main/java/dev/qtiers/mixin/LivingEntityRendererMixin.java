package dev.qtiers.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.qtiers.QTiersConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Shows your own nametag (with tiers) in third person (F5), like WhoAmI. Vanilla hides it for the camera entity. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @ModifyReturnValue(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z", at = @At("RETURN"))
    private boolean qtiers$showOwnName(boolean original, LivingEntity entity, double squaredDistance) {
        if (original || !QTiersConfig.get().showOwnNametag) return original;
        MinecraftClient client = MinecraftClient.getInstance();
        return entity == client.player
                && entity == client.getCameraEntity()
                && !client.options.getPerspective().isFirstPerson()
                && MinecraftClient.isHudEnabled()
                && !entity.isInvisible();
    }
}
