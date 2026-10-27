package dev.qtiers.mixin;

import dev.qtiers.ClickableNames;
import dev.qtiers.QTiersProfileScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Opens the profile straight away when a clickable name is clicked, instead of running the command. */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Inject(method = "handleComponentClicked(Lnet/minecraft/network/chat/Style;Z)Z", at = @At("HEAD"), cancellable = true)
    private void qtiers$openProfile(Style style, boolean allowInsertions, CallbackInfoReturnable<Boolean> cir) {
        if (!allowInsertions && style.getClickEvent() instanceof ClickEvent.RunCommand(String command)
                && command.startsWith(ClickableNames.COMMAND_PREFIX)) {
            String name = command.substring(ClickableNames.COMMAND_PREFIX.length()).trim();
            if (name.matches("[A-Za-z0-9_]{3,16}")) {
                Minecraft.getInstance().gui.setScreen(new QTiersProfileScreen(name));
                cir.setReturnValue(true);
            }
        }
    }
}
