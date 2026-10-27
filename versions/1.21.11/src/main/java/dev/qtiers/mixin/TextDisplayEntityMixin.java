package dev.qtiers.mixin;

import dev.qtiers.ServerNames;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.Text;
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
@Mixin(DisplayEntity.TextDisplayEntity.class)
public abstract class TextDisplayEntityMixin {
    @Shadow private DisplayEntity.TextDisplayEntity.@Nullable TextLines textLines;

    @Shadow
    public abstract DisplayEntity.TextDisplayEntity.@Nullable Data getData();

    /** The decorated text the cached lines were built from. */
    @Unique
    private String qtiers$shown;

    @Inject(method = "splitLines", at = @At("HEAD"))
    private void qtiers$refreshWhenTiersChange(DisplayEntity.TextDisplayEntity.LineSplitter splitter,
                                               CallbackInfoReturnable<DisplayEntity.TextDisplayEntity.TextLines> cir) {
        DisplayEntity.TextDisplayEntity.Data data = getData();
        if (data == null) return;
        String shown = ServerNames.decorateTextDisplay((DisplayEntity.TextDisplayEntity) (Object) this, data.text()).getString();
        if (!shown.equals(qtiers$shown)) {
            qtiers$shown = shown;
            textLines = null;
        }
    }

    @ModifyArg(method = "splitLines", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/decoration/DisplayEntity$TextDisplayEntity$LineSplitter;split(Lnet/minecraft/text/Text;I)Lnet/minecraft/entity/decoration/DisplayEntity$TextDisplayEntity$TextLines;"),
            index = 0)
    private Text qtiers$addTiers(Text text) {
        return ServerNames.decorateTextDisplay((DisplayEntity.TextDisplayEntity) (Object) this, text);
    }
}
