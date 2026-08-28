package com.example.chattabs.mixin;

import com.example.chattabs.chat.TabManager;
import com.example.chattabs.config.ChatTabsConfig;
import com.example.chattabs.config.TabDefinition;
import com.example.chattabs.gui.ChatTabBar;
import com.example.chattabs.gui.TabEditScreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * All input is polled from GLFW inside render rather than hooked through mouseClicked /
 * mouseScrolled / keyPressed. Those signatures changed in 1.21.9 (they now take Click and
 * KeyInput records instead of raw coordinates) and have churned repeatedly across versions;
 * polling keeps this file independent of them.
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Unique private static boolean chattabs$leftDown;
    @Unique private static boolean chattabs$rightDown;
    @Unique private static boolean chattabs$middleDown;
    @Unique private static boolean chattabs$tabDown;

    @Inject(method = "render", at = @At("TAIL"))
    private void chattabs$input(DrawContext context, int mouseX, int mouseY, float delta,
                                CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        long window = client.getWindow().getHandle();

        boolean left = chattabs$button(window, GLFW.GLFW_MOUSE_BUTTON_LEFT);
        boolean right = chattabs$button(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT);
        boolean middle = chattabs$button(window, GLFW.GLFW_MOUSE_BUTTON_MIDDLE);

        if (ChatTabBar.isDragging()) {
            if (left) ChatTabBar.updateDrag(mouseX, mouseY);
            else ChatTabBar.endDrag();
        } else if (left && !chattabs$leftDown) {
            chattabs$leftPress(client, mouseX, mouseY);
        }

        if (right && !chattabs$rightDown) {
            int tab = ChatTabBar.tabAt(mouseX, mouseY, true);
            if (tab >= 0) {
                client.setScreen(new TabEditScreen(client.currentScreen,
                        ChatTabsConfig.get().tabs.get(tab)));
            }
        }

        if (middle && !chattabs$middleDown) {
            int tab = ChatTabBar.tabAt(mouseX, mouseY, true);
            ChatTabsConfig cfg = ChatTabsConfig.get();
            if (tab >= 0 && cfg.tabs.size() > 1) {
                cfg.tabs.remove(tab);
                TabManager.get().setActiveTab(Math.min(cfg.activeTab, cfg.tabs.size() - 1));
            }
        }

        boolean ctrl = chattabs$key(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || chattabs$key(window, GLFW.GLFW_KEY_RIGHT_CONTROL)
                || chattabs$key(window, GLFW.GLFW_KEY_LEFT_SUPER)
                || chattabs$key(window, GLFW.GLFW_KEY_RIGHT_SUPER);
        boolean tabKey = chattabs$key(window, GLFW.GLFW_KEY_TAB);
        if (ctrl && tabKey && !chattabs$tabDown) {
            boolean shift = chattabs$key(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                    || chattabs$key(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
            TabManager.get().cycleTab(shift ? -1 : 1);
        }

        chattabs$leftDown = left;
        chattabs$rightDown = right;
        chattabs$middleDown = middle;
        chattabs$tabDown = tabKey;
    }

    @Unique
    private void chattabs$leftPress(MinecraftClient client, int mouseX, int mouseY) {
        ChatTabsConfig cfg = ChatTabsConfig.get();

        if (ChatTabBar.isResizeHandle(mouseX, mouseY, true)) {
            ChatTabBar.beginDrag(ChatTabBar.Drag.RESIZE, mouseX, mouseY);
            return;
        }

        int tab = ChatTabBar.tabAt(mouseX, mouseY, true);
        if (tab >= 0) {
            TabManager.get().setActiveTab(tab);
            return;
        }

        if (ChatTabBar.isPlusButton(mouseX, mouseY, true)) {
            TabDefinition created = new TabDefinition("Tab " + (cfg.tabs.size() + 1));
            cfg.tabs.add(created);
            ChatTabsConfig.save();
            client.setScreen(new TabEditScreen(client.currentScreen, created));
            return;
        }

        if (ChatTabBar.isBarBackground(mouseX, mouseY, true)) {
            ChatTabBar.beginDrag(ChatTabBar.Drag.MOVE, mouseX, mouseY);
        }
    }

    @Unique
    private static boolean chattabs$button(long window, int button) {
        return GLFW.glfwGetMouseButton(window, button) == GLFW.GLFW_PRESS;
    }

    @Unique
    private static boolean chattabs$key(long window, int key) {
        return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }

    /** Prepends the active tab's send prefix (e.g. "/msg Steve ") to outgoing messages. */
    @ModifyVariable(method = "sendMessage", at = @At("HEAD"), ordinal = 0, argsOnly = true, require = 0)
    private String chattabs$applyPrefix(String text) {
        TabDefinition tab = ChatTabsConfig.get().active();
        if (tab.sendPrefix == null || tab.sendPrefix.isEmpty()) return text;
        if (text.startsWith("/")) return text;
        return tab.sendPrefix + text;
    }
}
