package com.example.chattabs.mixin;

import com.example.chattabs.chat.TabManager;
import com.example.chattabs.config.ChatTabsConfig;
import com.example.chattabs.gui.ChatTabBar;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    // ---------------------------------------------------------------- routing

    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void chattabs$routeMessage(Text message,
                                       @Nullable MessageSignatureData signature,
                                       @Nullable MessageIndicator indicator,
                                       CallbackInfo ci) {
        if (TabManager.get().isSuppressed()) return;
        ci.cancel();
        TabManager.get().handleIncoming((ChatHud) (Object) this, message, signature, indicator);
    }

    // ---------------------------------------------------------------- size

    @Inject(method = "getWidth()I", at = @At("HEAD"), cancellable = true)
    private void chattabs$width(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(ChatTabsConfig.get().width);
    }

    @Inject(method = "getHeight()I", at = @At("HEAD"), cancellable = true)
    private void chattabs$height(CallbackInfoReturnable<Integer> cir) {
        ChatTabsConfig cfg = ChatTabsConfig.get();
        cir.setReturnValue(chattabs$focused() ? cfg.focusedHeight : cfg.unfocusedHeight);
    }

    private static boolean chattabs$focused() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.currentScreen instanceof ChatScreen;
    }

    // ---------------------------------------------------------------- position + tab strip

    @Inject(method = "render", at = @At("HEAD"))
    private void chattabs$shiftBefore(DrawContext context, int currentTick, int mouseX, int mouseY,
                                      boolean focused, CallbackInfo ci) {
        ChatTabsConfig cfg = ChatTabsConfig.get();
        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float) cfg.offsetX, (float) cfg.offsetY);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void chattabs$shiftAfter(DrawContext context, int currentTick, int mouseX, int mouseY,
                                     boolean focused, CallbackInfo ci) {
        context.getMatrices().popMatrix();
        ChatTabBar.render(context, focused);
    }

    // Keep click/hover detection on chat links aligned with the moved window.

    @ModifyVariable(method = "getTextStyleAt(DD)Lnet/minecraft/text/Style;", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double chattabs$unshiftX(double x) {
        return x - ChatTabsConfig.get().offsetX;
    }

    @ModifyVariable(method = "getTextStyleAt(DD)Lnet/minecraft/text/Style;", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private double chattabs$unshiftY(double y) {
        return y - ChatTabsConfig.get().offsetY;
    }
}
