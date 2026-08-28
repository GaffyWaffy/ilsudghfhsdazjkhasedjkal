package com.example.chattabs.gui;

import com.example.chattabs.chat.TabManager;
import com.example.chattabs.config.ChatTabsConfig;
import com.example.chattabs.config.TabDefinition;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.IntConsumer;

public class ChatTabsConfigScreen extends Screen {

    private static final int ROWS = 5;

    private final Screen parent;
    private int scroll = 0;

    public ChatTabsConfigScreen(Screen parent) {
        super(Text.translatable("chattabs.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ChatTabsConfig cfg = ChatTabsConfig.get();
        cfg.clamp();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, cfg.tabs.size() - ROWS)));

        int cx = width / 2;
        int y = 32;

        for (int i = scroll; i < Math.min(cfg.tabs.size(), scroll + ROWS); i++) {
            final int index = i;
            TabDefinition tab = cfg.tabs.get(i);

            addDrawableChild(ButtonWidget.builder(Text.literal(tab.name),
                            b -> client.setScreen(new TabEditScreen(this, tab)))
                    .dimensions(cx - 170, y, 150, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal(tab.enabled ? "On" : "Off"), b -> {
                tab.enabled = !tab.enabled;
                saveAndRefresh();
            }).dimensions(cx - 18, y, 34, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("^"), b -> {
                if (index > 0) { swap(cfg, index, index - 1); saveAndRefresh(); }
            }).dimensions(cx + 20, y, 20, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("v"), b -> {
                if (index < cfg.tabs.size() - 1) { swap(cfg, index, index + 1); saveAndRefresh(); }
            }).dimensions(cx + 42, y, 20, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Delete"), b -> {
                if (cfg.tabs.size() > 1) {
                    cfg.tabs.remove(index);
                    if (cfg.activeTab >= cfg.tabs.size()) cfg.activeTab = cfg.tabs.size() - 1;
                    saveAndRefresh();
                }
            }).dimensions(cx + 66, y, 50, 20).build());

            y += 22;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Scroll up"), b -> {
            scroll = Math.max(0, scroll - 1);
            clearAndInit();
        }).dimensions(cx - 170, y + 2, 80, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Scroll down"), b -> {
            scroll++;
            clearAndInit();
        }).dimensions(cx - 86, y + 2, 80, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Add tab"), b -> {
            TabDefinition created = new TabDefinition("Tab " + (cfg.tabs.size() + 1));
            cfg.tabs.add(created);
            ChatTabsConfig.save();
            client.setScreen(new TabEditScreen(this, created));
        }).dimensions(cx + 2, y + 2, 80, 20).build());

        int sy = y + 32;
        addDrawableChild(new IntSlider(cx - 170, sy, 160, 20, "Width", 80, 640, cfg.width,
                v -> cfg.width = v));
        addDrawableChild(new IntSlider(cx + 2, sy, 160, 20, "Open height", 30, 400, cfg.focusedHeight,
                v -> cfg.focusedHeight = v));

        addDrawableChild(new IntSlider(cx - 170, sy + 24, 160, 20, "Closed height", 20, 400,
                cfg.unfocusedHeight, v -> cfg.unfocusedHeight = v));
        addDrawableChild(new IntSlider(cx + 2, sy + 24, 160, 20, "Scrollback per tab", 100, 5000,
                cfg.maxStoredPerTab, v -> cfg.maxStoredPerTab = v));

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Tab strip: " + (cfg.tabBarAbove ? "above chat" : "below chat")), b -> {
            cfg.tabBarAbove = !cfg.tabBarAbove;
            saveAndRefresh();
        }).dimensions(cx - 170, sy + 48, 160, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Show strip when closed: " + (cfg.showTabBarWhenClosed ? "yes" : "no")), b -> {
            cfg.showTabBarWhenClosed = !cfg.showTabBarWhenClosed;
            saveAndRefresh();
        }).dimensions(cx + 2, sy + 48, 160, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Highlight sound: " + (cfg.highlightSounds ? "on" : "off")), b -> {
            cfg.highlightSounds = !cfg.highlightSounds;
            saveAndRefresh();
        }).dimensions(cx - 170, sy + 72, 160, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Reset position"), b -> {
            cfg.offsetX = 0;
            cfg.offsetY = 0;
            saveAndRefresh();
        }).dimensions(cx + 2, sy + 72, 160, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(cx - 100, Math.min(height - 28, sy + 102), 200, 20).build());
    }

    private static void swap(ChatTabsConfig cfg, int a, int b) {
        TabDefinition tmp = cfg.tabs.get(a);
        cfg.tabs.set(a, cfg.tabs.get(b));
        cfg.tabs.set(b, tmp);
    }

    private void saveAndRefresh() {
        ChatTabsConfig.save();
        TabManager.get().rebuild();
        clearAndInit();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Click a tab name to edit its filters and highlights"),
                width / 2, 22, 0xA0A0A0);
    }

    @Override
    public void close() {
        ChatTabsConfig.save();
        TabManager.get().rebuild();
        client.setScreen(parent);
    }

    /** Small integer slider helper. */
    private static class IntSlider extends SliderWidget {
        private final String label;
        private final int min, max;
        private final IntConsumer setter;

        IntSlider(int x, int y, int w, int h, String label, int min, int max, int value, IntConsumer setter) {
            super(x, y, w, h, Text.literal(label + ": " + value), (double) (value - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
        }

        private int current() {
            return min + (int) Math.round(value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(label + ": " + current()));
        }

        @Override
        protected void applyValue() {
            setter.accept(current());
        }
    }
}
