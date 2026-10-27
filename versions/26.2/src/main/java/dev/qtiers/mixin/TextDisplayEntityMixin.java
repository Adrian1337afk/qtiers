package dev.qtiers.mixin;

import dev.qtiers.ServerNames;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Servers like mcpvp.club hide the vanilla nametag and show the name with a text display riding
 * the player. This adds the player's tiers to that text, re-splitting the lines when they change.
 */
@Mixin(Display.TextDisplay.class)
public abstract class TextDisplayEntityMixin {
    @Shadow private Display.TextDisplay.@Nullable CachedInfo clientDisplayCache;

    @Shadow
    public abstract Display.TextDisplay.@Nullable TextRenderState textRenderState();

    /** The decorated text the cached lines were built from. */
    @Unique
    private String qtiers$shown;

    @Inject(method = "cacheDisplay", at = @At("HEAD"))
    private void qtiers$refreshWhenTiersChange(Display.TextDisplay.LineSplitter splitter,
                                               CallbackInfoReturnable<Display.TextDisplay.CachedInfo> cir) {
        Display.TextDisplay.TextRenderState state = textRenderState();
        if (state == null) return;
        String shown = ServerNames.decorateTextDisplay((Display.TextDisplay) (Object) this, state.text()).getString();
        if (!shown.equals(qtiers$shown)) {
            qtiers$shown = shown;
            clientDisplayCache = null;
        }
    }

    @ModifyArg(method = "cacheDisplay", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Display$TextDisplay$LineSplitter;split(Lnet/minecraft/network/chat/Component;I)Lnet/minecraft/world/entity/Display$TextDisplay$CachedInfo;"),
            index = 0)
    private Component qtiers$addTiers(Component text) {
        return ServerNames.decorateTextDisplay((Display.TextDisplay) (Object) this, text);
    }
}
