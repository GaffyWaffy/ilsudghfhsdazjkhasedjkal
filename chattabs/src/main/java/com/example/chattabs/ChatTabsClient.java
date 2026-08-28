package com.example.chattabs;

import com.example.chattabs.config.ChatTabsConfig;
import com.example.chattabs.gui.ChatTabsConfigScreen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.client.MinecraftClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatTabsClient implements ClientModInitializer {

    public static final String MOD_ID = "chattabs";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        ChatTabsConfig.load();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
                dispatcher.register(ClientCommandManager.literal("chattabs").executes(ctx -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    // deferred so the chat screen closes first
                    client.send(() -> client.setScreen(new ChatTabsConfigScreen(null)));
                    return 1;
                })));

        LOGGER.info("Chat Tabs ready - type /chattabs for settings");
    }
}
