package com.example.chattabs.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ChatTabsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("chattabs.json");
    private static ChatTabsConfig instance;

    // --- window geometry (chat units; the vanilla chat scale is applied on top) ---
    public int width = 320;
    public int focusedHeight = 180;
    public int unfocusedHeight = 90;
    /** Offset in raw screen pixels from the vanilla anchor. */
    public int offsetX = 0;
    public int offsetY = 0;

    // --- tab bar ---
    public boolean showTabBar = true;
    public boolean showTabBarWhenClosed = false;
    public boolean tabBarAbove = true;
    public int tabBarHeight = 12;
    public int activeColor = 0xFF3C3C3C;
    public int inactiveColor = 0x99101010;
    public int borderColor = 0xFF6A6A6A;

    // --- behaviour ---
    public int maxStoredPerTab = 1000;
    public boolean highlightSounds = true;
    public boolean preserveMessageAgeOnSwitch = true;
    public int activeTab = 0;

    public List<TabDefinition> tabs = new ArrayList<>();

    public static ChatTabsConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        ChatTabsConfig loaded = null;
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
                loaded = GSON.fromJson(reader, ChatTabsConfig.class);
            } catch (Exception e) {
                loaded = null;
            }
        }
        if (loaded == null) loaded = defaults();
        if (loaded.tabs == null || loaded.tabs.isEmpty()) loaded.tabs = defaults().tabs;
        loaded.clamp();
        instance = loaded;
        instance.tabs.forEach(TabDefinition::invalidateCaches);
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(get(), writer);
            }
        } catch (IOException ignored) {
        }
    }

    public void clamp() {
        width = Math.max(80, Math.min(width, 1000));
        focusedHeight = Math.max(30, Math.min(focusedHeight, 800));
        unfocusedHeight = Math.max(20, Math.min(unfocusedHeight, 800));
        if (tabs.isEmpty()) tabs.add(new TabDefinition("All"));
        if (activeTab < 0 || activeTab >= tabs.size()) activeTab = 0;
    }

    public TabDefinition active() {
        clamp();
        return tabs.get(activeTab);
    }

    private static ChatTabsConfig defaults() {
        ChatTabsConfig cfg = new ChatTabsConfig();

        TabDefinition all = new TabDefinition("All");
        HighlightRule name = new HighlightRule("", 0xFFFF55);
        name.playSound = true;
        all.highlights.add(name); // fill in your own IGN in the settings screen

        TabDefinition chat = new TabDefinition("Chat");
        chat.filters.add(new FilterRule(FilterRule.Type.REGEX, "^<[^>]+>", false));

        TabDefinition whispers = new TabDefinition("Whispers");
        whispers.filters.add(new FilterRule(FilterRule.Type.CONTAINS, "whispers to you", false));
        whispers.filters.add(new FilterRule(FilterRule.Type.CONTAINS, "You whisper to", false));
        whispers.sendPrefix = "";

        TabDefinition system = new TabDefinition("System");
        system.filters.add(new FilterRule(FilterRule.Type.REGEX, "^<[^>]+>", true));
        system.filters.add(new FilterRule(FilterRule.Type.CONTAINS, "whispers to you", true));

        cfg.tabs.add(all);
        cfg.tabs.add(chat);
        cfg.tabs.add(whispers);
        cfg.tabs.add(system);
        return cfg;
    }
}
