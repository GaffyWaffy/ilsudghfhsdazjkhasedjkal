package com.example.chattabs.gui;

import com.example.chattabs.chat.TabManager;
import com.example.chattabs.config.ChatTabsConfig;
import com.example.chattabs.config.TabDefinition;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Geometry + drawing for the tab strip, and the move/resize drag handling.
 *
 * Everything here works in raw (scaled-GUI) screen pixels. The chat itself is laid out in
 * "chat units" which vanilla multiplies by the chat scale option, so widths/heights are
 * converted with {@link #chatScale()}.
 */
public final class ChatTabBar {

    public enum Drag { NONE, MOVE, RESIZE }

    private static Drag drag = Drag.NONE;
    private static double dragStartX, dragStartY;
    private static int startOffsetX, startOffsetY, startWidth, startHeight;

    public static final int PLUS_WIDTH = 14;
    public static final int HANDLE = 8;

    private ChatTabBar() {}

    // ------------------------------------------------------------------ geometry

    private static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    public static double chatScale() {
        return mc().options.getChatScale().getValue();
    }

    public static int left() {
        return 4 + ChatTabsConfig.get().offsetX;
    }

    public static int bottom() {
        return mc().getWindow().getScaledHeight() - 40 + ChatTabsConfig.get().offsetY;
    }

    public static int widthPx() {
        return (int) (ChatTabsConfig.get().width * chatScale());
    }

    public static int heightPx(boolean focused) {
        ChatTabsConfig cfg = ChatTabsConfig.get();
        return (int) ((focused ? cfg.focusedHeight : cfg.unfocusedHeight) * chatScale());
    }

    public static int right() { return left() + widthPx(); }

    public static int top(boolean focused) { return bottom() - heightPx(focused); }

    public static int barY(boolean focused) {
        ChatTabsConfig cfg = ChatTabsConfig.get();
        return cfg.tabBarAbove ? top(focused) - cfg.tabBarHeight - 1 : bottom() + 2;
    }

    public static boolean visible(boolean focused) {
        ChatTabsConfig cfg = ChatTabsConfig.get();
        return cfg.showTabBar && (focused || cfg.showTabBarWhenClosed);
    }

    private static String label(TabDefinition tab, int index, ChatTabsConfig cfg) {
        String text = tab.name;
        if (tab.showUnreadBadge && tab.unread > 0 && index != cfg.activeTab) {
            text += " (" + Math.min(tab.unread, 99) + ")";
        }
        return text;
    }

    private static int tabWidth(TextRenderer tr, TabDefinition tab, int index, ChatTabsConfig cfg) {
        return tr.getWidth(label(tab, index, cfg)) + 10;
    }

    // ------------------------------------------------------------------ rendering

    public static void render(DrawContext context, boolean focused) {
        if (!visible(focused)) return;

        MinecraftClient client = mc();
        TextRenderer tr = client.textRenderer;
        ChatTabsConfig cfg = ChatTabsConfig.get();
        cfg.clamp();

        int y = barY(focused);
        int h = cfg.tabBarHeight;
        int x = left();
        int maxX = right();

        for (int i = 0; i < cfg.tabs.size(); i++) {
            TabDefinition tab = cfg.tabs.get(i);
            int w = tabWidth(tr, tab, i, cfg);
            if (x + w > maxX && i > 0) break; // strip is full
            boolean active = i == cfg.activeTab;
            context.fill(x, y, x + w, y + h, active ? cfg.activeColor : cfg.inactiveColor);
            if (active) context.drawBorder(x, y, w, h, cfg.borderColor);
            int colour = !tab.enabled ? 0xFF808080
                    : active ? 0xFFFFFFFF
                    : (tab.unread > 0 ? 0xFFFFD24A : 0xFFB0B0B0);
            context.drawText(tr, label(tab, i, cfg), x + 5, y + (h - 8) / 2, colour, false);
            x += w + 1;
        }

        // "+" button
        if (x + PLUS_WIDTH <= maxX) {
            context.fill(x, y, x + PLUS_WIDTH, y + h, cfg.inactiveColor);
            context.drawText(tr, "+", x + 5, y + (h - 8) / 2, 0xFF9FD98F, false);
        }

        if (focused) {
            int hx = right() - HANDLE;
            int hy = top(true);
            context.fill(hx, hy, hx + HANDLE, hy + HANDLE, 0x88FFFFFF);
            context.drawBorder(hx, hy, HANDLE, HANDLE, cfg.borderColor);
        }
    }

    // ------------------------------------------------------------------ hit testing

    /** @return tab index under the cursor, or -1. */
    public static int tabAt(double mouseX, double mouseY, boolean focused) {
        if (!visible(focused)) return -1;
        ChatTabsConfig cfg = ChatTabsConfig.get();
        int y = barY(focused);
        if (mouseY < y || mouseY >= y + cfg.tabBarHeight) return -1;

        TextRenderer tr = mc().textRenderer;
        int x = left();
        int maxX = right();
        for (int i = 0; i < cfg.tabs.size(); i++) {
            int w = tabWidth(tr, cfg.tabs.get(i), i, cfg);
            if (x + w > maxX && i > 0) break;
            if (mouseX >= x && mouseX < x + w) return i;
            x += w + 1;
        }
        return -1;
    }

    public static boolean isPlusButton(double mouseX, double mouseY, boolean focused) {
        if (!visible(focused)) return false;
        ChatTabsConfig cfg = ChatTabsConfig.get();
        int y = barY(focused);
        if (mouseY < y || mouseY >= y + cfg.tabBarHeight) return false;

        TextRenderer tr = mc().textRenderer;
        int x = left();
        int maxX = right();
        for (int i = 0; i < cfg.tabs.size(); i++) {
            int w = tabWidth(tr, cfg.tabs.get(i), i, cfg);
            if (x + w > maxX && i > 0) break;
            x += w + 1;
        }
        return mouseX >= x && mouseX < x + PLUS_WIDTH && x + PLUS_WIDTH <= maxX;
    }

    public static boolean isBarBackground(double mouseX, double mouseY, boolean focused) {
        if (!visible(focused)) return false;
        ChatTabsConfig cfg = ChatTabsConfig.get();
        int y = barY(focused);
        return mouseY >= y && mouseY < y + cfg.tabBarHeight
                && mouseX >= left() && mouseX < right();
    }

    public static boolean isResizeHandle(double mouseX, double mouseY, boolean focused) {
        if (!focused) return false;
        int hx = right() - HANDLE;
        int hy = top(true);
        return mouseX >= hx && mouseX < hx + HANDLE && mouseY >= hy && mouseY < hy + HANDLE;
    }

    // ------------------------------------------------------------------ dragging

    public static void beginDrag(Drag mode, double mouseX, double mouseY) {
        ChatTabsConfig cfg = ChatTabsConfig.get();
        drag = mode;
        dragStartX = mouseX;
        dragStartY = mouseY;
        startOffsetX = cfg.offsetX;
        startOffsetY = cfg.offsetY;
        startWidth = cfg.width;
        startHeight = cfg.focusedHeight;
    }

    public static boolean isDragging() { return drag != Drag.NONE; }

    public static void updateDrag(double mouseX, double mouseY) {
        if (drag == Drag.NONE) return;
        ChatTabsConfig cfg = ChatTabsConfig.get();
        double dx = mouseX - dragStartX;
        double dy = mouseY - dragStartY;

        if (drag == Drag.MOVE) {
            cfg.offsetX = startOffsetX + (int) dx;
            cfg.offsetY = startOffsetY + (int) dy;
            int sw = mc().getWindow().getScaledWidth();
            int sh = mc().getWindow().getScaledHeight();
            cfg.offsetX = Math.max(-4, Math.min(cfg.offsetX, sw - widthPx() - 4));
            cfg.offsetY = Math.max(-(sh - 60), Math.min(cfg.offsetY, 30));
        } else {
            double scale = Math.max(0.1, chatScale());
            cfg.width = startWidth + (int) (dx / scale);
            cfg.focusedHeight = startHeight - (int) (dy / scale);
            cfg.clamp();
        }
    }

    public static void endDrag() {
        if (drag == Drag.NONE) return;
        drag = Drag.NONE;
        ChatTabsConfig.save();
        TabManager.get().rebuild(); // re-wrap lines at the new width
    }
}
