package dev.qtiers.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.qtiers.QTiersConfig;
import dev.qtiers.ServerNames;
import dev.qtiers.TierDisplay;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerListHudMixin {
    @ModifyReturnValue(method = "getNameForDisplay", at = @At("RETURN"))
    private Component qtiers$addTiers(Component original, PlayerInfo entry) {
        if (QTiersConfig.get().showInTabList) {
            // ServerNames maps a server's fake tab rows (e.g. mcpvp.club) to the real player
            return TierDisplay.decorate(ServerNames.tabPlayer(entry), original);
        }
        return original;
    }
}
