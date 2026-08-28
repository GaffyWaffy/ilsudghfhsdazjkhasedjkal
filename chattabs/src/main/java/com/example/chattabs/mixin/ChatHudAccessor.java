package com.example.chattabs.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ChatHud.class)
public interface ChatHudAccessor {

    @Accessor("messages")
    List<ChatHudLine> chattabs$getMessages();

    @Accessor("visibleMessages")
    List<ChatHudLine.Visible> chattabs$getVisibleMessages();

    @Invoker("addMessage")
    void chattabs$addMessage(Text message,
                             @Nullable MessageSignatureData signature,
                             @Nullable MessageIndicator indicator);
}
