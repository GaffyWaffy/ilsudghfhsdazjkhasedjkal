package com.example.chattabs.gui;

import com.example.chattabs.chat.TabManager;
import com.example.chattabs.config.ChatTabsConfig;
import com.example.chattabs.config.FilterRule;
import com.example.chattabs.config.HighlightRule;
import com.example.chattabs.config.TabDefinition;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/** Per-tab editor: name, send prefix, match mode, filter rules and highlight rules. */
public class TabEditScreen extends Screen {

    private static final int PAGE = 4;

    private final Screen parent;
    private final TabDefinition tab;
    private int filterPage = 0;
    private int highlightPage = 0;

    public TabEditScreen(Screen parent, TabDefinition tab) {
        super(Text.translatable("chattabs.screen.edit"));
        this.parent = parent;
        this.tab = tab;
    }

    @Override
    protected void init() {
        int left = width / 2 - 190;
        int y = 26;

        TextFieldWidget nameField = new TextFieldWidget(textRenderer, left, y, 140, 18, Text.literal("Name"));
        nameField.setMaxLength(24);
        nameField.setText(tab.name);
        nameField.setChangedListener(s -> tab.name = s);
        addDrawableChild(nameField);

        TextFieldWidget prefixField = new TextFieldWidget(textRenderer, left + 150, y, 140, 18, Text.literal("Prefix"));
        prefixField.setMaxLength(64);
        prefixField.setText(tab.sendPrefix == null ? "" : tab.sendPrefix);
        prefixField.setChangedListener(s -> tab.sendPrefix = s);
        addDrawableChild(prefixField);

        addDrawableChild(ButtonWidget.builder(Text.literal(tab.mode.label()), b -> {
            tab.mode = tab.mode.next();
            refresh();
        }).dimensions(left + 300, y - 1, 80, 20).build());

        // ---------------- filters ----------------
        y += 34;
        int filterTop = y;
        int start = filterPage * PAGE;
        for (int i = start; i < Math.min(tab.filters.size(), start + PAGE); i++) {
            final int index = i;
            FilterRule rule = tab.filters.get(i);

            addDrawableChild(ButtonWidget.builder(Text.literal(rule.type.label()), b -> {
                rule.type = rule.type.next();
                rule.invalidate();
                refresh();
            }).dimensions(left, y, 78, 20).build());

            TextFieldWidget pattern = new TextFieldWidget(textRenderer, left + 82, y + 1, 170, 18, Text.literal("Pattern"));
            pattern.setMaxLength(200);
            pattern.setText(rule.pattern);
            pattern.setChangedListener(s -> { rule.pattern = s; rule.invalidate(); });
            addDrawableChild(pattern);

            addDrawableChild(ButtonWidget.builder(Text.literal(rule.ignoreCase ? "aA" : "Aa"), b -> {
                rule.ignoreCase = !rule.ignoreCase;
                rule.invalidate();
                refresh();
            }).dimensions(left + 256, y, 26, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal(rule.invert ? "exclude" : "include"), b -> {
                rule.invert = !rule.invert;
                refresh();
            }).dimensions(left + 286, y, 62, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("X"), b -> {
                tab.filters.remove(index);
                refresh();
            }).dimensions(left + 352, y, 20, 20).build());

            y += 22;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Add filter"), b -> {
            tab.filters.add(new FilterRule());
            filterPage = Math.max(0, (tab.filters.size() - 1) / PAGE);
            refresh();
        }).dimensions(left, filterTop + PAGE * 22, 80, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
            filterPage = Math.max(0, filterPage - 1);
            refresh();
        }).dimensions(left + 84, filterTop + PAGE * 22, 20, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
            if ((filterPage + 1) * PAGE < tab.filters.size()) filterPage++;
            refresh();
        }).dimensions(left + 106, filterTop + PAGE * 22, 20, 20).build());

        // ---------------- highlights ----------------
        y = filterTop + PAGE * 22 + 40;
        int highlightTop = y;
        int hstart = highlightPage * PAGE;
        for (int i = hstart; i < Math.min(tab.highlights.size(), hstart + PAGE); i++) {
            final int index = i;
            HighlightRule rule = tab.highlights.get(i);

            TextFieldWidget pattern = new TextFieldWidget(textRenderer, left, y + 1, 150, 18, Text.literal("Keyword"));
            pattern.setMaxLength(200);
            pattern.setText(rule.pattern);
            pattern.setChangedListener(s -> { rule.pattern = s; rule.invalidate(); });
            addDrawableChild(pattern);

            addDrawableChild(ButtonWidget.builder(Text.literal(rule.regex ? ".*" : "abc"), b -> {
                rule.regex = !rule.regex;
                rule.invalidate();
                refresh();
            }).dimensions(left + 154, y, 30, 20).build());

            TextFieldWidget colour = new TextFieldWidget(textRenderer, left + 188, y + 1, 60, 18, Text.literal("Colour"));
            colour.setMaxLength(7);
            colour.setText(String.format("#%06X", rule.color & 0xFFFFFF));
            colour.setChangedListener(s -> {
                try {
                    rule.color = Integer.parseInt(s.replace("#", "").trim(), 16) & 0xFFFFFF;
                } catch (NumberFormatException ignored) {
                }
            });
            addDrawableChild(colour);

            addDrawableChild(ButtonWidget.builder(Text.literal(rule.bold ? "B" : "b"), b -> {
                rule.bold = !rule.bold;
                refresh();
            }).dimensions(left + 252, y, 20, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal(rule.underline ? "U" : "u"), b -> {
                rule.underline = !rule.underline;
                refresh();
            }).dimensions(left + 274, y, 20, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal(rule.wholeLine ? "line" : "word"), b -> {
                rule.wholeLine = !rule.wholeLine;
                refresh();
            }).dimensions(left + 296, y, 42, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal(rule.playSound ? "ping" : "mute"), b -> {
                rule.playSound = !rule.playSound;
                refresh();
            }).dimensions(left + 340, y, 40, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("X"), b -> {
                tab.highlights.remove(index);
                refresh();
            }).dimensions(left + 384, y, 20, 20).build());

            y += 22;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Add highlight"), b -> {
            tab.highlights.add(new HighlightRule("", 0xFFFF55));
            highlightPage = Math.max(0, (tab.highlights.size() - 1) / PAGE);
            refresh();
        }).dimensions(left, highlightTop + PAGE * 22, 90, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
            highlightPage = Math.max(0, highlightPage - 1);
            refresh();
        }).dimensions(left + 94, highlightTop + PAGE * 22, 20, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
            if ((highlightPage + 1) * PAGE < tab.highlights.size()) highlightPage++;
            refresh();
        }).dimensions(left + 116, highlightTop + PAGE * 22, 20, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(width / 2 - 100, Math.min(height - 26, highlightTop + PAGE * 22 + 26), 200, 20).build());
    }

    private void refresh() {
        ChatTabsConfig.save();
        TabManager.get().rebuild();
        clearAndInit();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int left = width / 2 - 190;
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Filters"), left, 52, 0xA0C8FF);
        context.drawTextWithShadow(textRenderer,
                Text.literal("(no include rules = catch-all; exclude rules always win)"),
                left + 50, 52, 0x808080);
        context.drawTextWithShadow(textRenderer, Text.literal("Highlights"),
                left, 26 + 34 + PAGE * 22 + 28, 0xA0C8FF);
    }

    @Override
    public void close() {
        ChatTabsConfig.save();
        TabManager.get().rebuild();
        client.setScreen(parent);
    }
}
