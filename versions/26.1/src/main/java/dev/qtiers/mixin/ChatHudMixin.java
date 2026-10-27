package dev.qtiers.mixin;

import dev.qtiers.ClickableNames;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Makes online players' names in incoming chat clickable (see {@link ClickableNames}). */
@Mixin(ChatComponent.class)
public abstract class ChatHudMixin {
    @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"), argsOnly = true)
    private Component qtiers$clickableNames(Component message) {
        return ClickableNames.process(message);
    }
}
