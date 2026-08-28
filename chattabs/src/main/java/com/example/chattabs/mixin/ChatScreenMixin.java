package com.example.chattabs.mixin;

import com.example.chattabs.chat.TabManager;
import com.example.chattabs.config.ChatTabsConfig;
import com.example.chattabs.config.TabDefinition;
import com.example.chattabs.gui.ChatTabBar;
import com.example.chattabs.gui.TabEditScreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {

    protected ChatScreenMixin() { super(null); }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void chattabs$click(double mouseX, double mouseY, int button,
                                CallbackInfoReturnable<Boolean> cir) {
        ChatTabsConfig cfg = ChatTabsConfig.get();
        MinecraftClient client = MinecraftClient.getInstance();

        if (ChatTabBar.isResizeHandle(mouseX, mouseY, true) && button == 0) {
            ChatTabBar.beginDrag(ChatTabBar.Drag.RESIZE, mouseX, mouseY);
            cir.setReturnValue(true);
            return;
        }

        int tab = ChatTabBar.tabAt(mouseX, mouseY, true);
        if (tab >= 0) {
            if (button == 1) {
                client.setScreen(new TabEditScreen(client.currentScreen, cfg.tabs.get(tab)));
            } else if (button == 2) {
                if (cfg.tabs.size() > 1) {
                    cfg.tabs.remove(tab);
                    TabManager.get().setActiveTab(Math.min(cfg.activeTab, cfg.tabs.size() - 1));
                }
            } else {
                TabManager.get().setActiveTab(tab);
            }
            cir.setReturnValue(true);
            return;
        }

        if (ChatTabBar.isPlusButton(mouseX, mouseY, true) && button == 0) {
            TabDefinition created = new TabDefinition("Tab " + (cfg.tabs.size() + 1));
            cfg.tabs.add(created);
            ChatTabsConfig.save();
            client.setScreen(new TabEditScreen(client.currentScreen, created));
            cir.setReturnValue(true);
            return;
        }

        if (ChatTabBar.isBarBackground(mouseX, mouseY, true) && button == 0) {
            ChatTabBar.beginDrag(ChatTabBar.Drag.MOVE, mouseX, mouseY);
            cir.setReturnValue(true);
        }
    }

    /**
     * Drag state is polled here rather than through mouseDragged/mouseReleased so it does not
     * depend on ChatScreen overriding those methods.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void chattabs$pollDrag(DrawContext context, int mouseX, int mouseY, float delta,
                                   CallbackInfo ci) {
        if (!ChatTabBar.isDragging()) return;
        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        if (GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            ChatTabBar.updateDrag(mouseX, mouseY);
        } else {
            ChatTabBar.endDrag();
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void chattabs$scroll(double mouseX, double mouseY, double horizontal, double vertical,
                                 CallbackInfoReturnable<Boolean> cir) {
        if (ChatTabBar.isBarBackground(mouseX, mouseY, true) || ChatTabBar.tabAt(mouseX, mouseY, true) >= 0) {
            TabManager.get().cycleTab(vertical > 0 ? -1 : 1);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void chattabs$keys(int keyCode, int scanCode, int modifiers,
                               CallbackInfoReturnable<Boolean> cir) {
        // Read the modifier bitmask GLFW already hands us rather than relying on
        // Screen's static helpers, which moved in 1.21.11.
        boolean ctrl = (modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER)) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        if (keyCode == GLFW.GLFW_KEY_TAB && ctrl) {
            TabManager.get().cycleTab(shift ? -1 : 1);
            cir.setReturnValue(true);
        }
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
