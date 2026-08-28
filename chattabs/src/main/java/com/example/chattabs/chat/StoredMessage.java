package com.example.chattabs.chat;

import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;

import org.jetbrains.annotations.Nullable;

/** A raw chat line kept in a tab's scrollback, before per-tab highlighting is applied. */
public record StoredMessage(Text text,
                            @Nullable MessageSignatureData signature,
                            @Nullable MessageIndicator indicator,
                            int creationTick,
                            String plain) {
}
