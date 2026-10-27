package dev.qtiers.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.qtiers.QTiersConfig;
import dev.qtiers.ServerNames;
import dev.qtiers.TierDisplay;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @ModifyReturnValue(method = "getPlayerName", at = @At("RETURN"))
    private Text qtiers$addTiers(Text original, PlayerListEntry entry) {
        if (QTiersConfig.get().showInTabList) {
            // ServerNames maps a server's fake tab rows (e.g. mcpvp.club) to the real player
            return TierDisplay.decorate(ServerNames.tabPlayer(entry), original);
        }
        return original;
    }
}
