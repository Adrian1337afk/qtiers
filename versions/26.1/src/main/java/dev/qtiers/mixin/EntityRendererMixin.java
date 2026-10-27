package dev.qtiers.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.qtiers.QTiersConfig;
import dev.qtiers.TierDisplay;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Adds tiers to player nametags only (chat and other UI are left alone). */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @ModifyReturnValue(method = "getNameTag", at = @At("RETURN"))
    private Component qtiers$addTiers(Component original, Entity entity) {
        if (entity instanceof Player player && QTiersConfig.get().showInNametags) {
            return TierDisplay.decorate(player.getUUID(), original);
        }
        return original;
    }
}
