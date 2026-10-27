package dev.qtiers.mixin;

import dev.qtiers.ClickableNames;
import dev.qtiers.QTiersProfileScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Opens the profile straight away when a clickable name in chat is clicked, instead of running the command. */
@Mixin(Screen.class)
public abstract class ChatScreenMixin {
    @Inject(method = "handleTextClick(Lnet/minecraft/text/Style;)Z", at = @At("HEAD"), cancellable = true)
    private void qtiers$openProfile(Style style, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ChatScreen) || style == null) return;
        if (style.getClickEvent() instanceof ClickEvent.RunCommand(String command)
                && command.startsWith(ClickableNames.COMMAND_PREFIX)) {
            String name = command.substring(ClickableNames.COMMAND_PREFIX.length()).trim();
            if (name.matches("[A-Za-z0-9_]{3,16}")) {
                MinecraftClient.getInstance().setScreen(new QTiersProfileScreen(name));
                cir.setReturnValue(true);
            }
        }
    }
}
