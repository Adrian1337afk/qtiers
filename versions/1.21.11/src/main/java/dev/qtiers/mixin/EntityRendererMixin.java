package dev.qtiers.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.qtiers.QTiersConfig;
import dev.qtiers.TierDisplay;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Adds tiers to player nametags only (chat and other UI are left alone). */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    private Text qtiers$addTiers(Text original, Entity entity) {
        if (entity instanceof PlayerEntity player && QTiersConfig.get().showInNametags) {
            return TierDisplay.decorate(player.getUuid(), original);
        }
        return original;
    }
}
