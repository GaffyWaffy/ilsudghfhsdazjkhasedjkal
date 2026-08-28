package com.example.chattabs.chat;

import com.example.chattabs.ChatTabsClient;
import com.example.chattabs.config.ChatTabsConfig;
import com.example.chattabs.config.TabDefinition;
import com.example.chattabs.mixin.ChatHudAccessor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Routes incoming chat into tabs and swaps the vanilla ChatHud contents when you switch tabs. */
public final class TabManager {

    private static final TabManager INSTANCE = new TabManager();
    public static TabManager get() { return INSTANCE; }

    private boolean suppress;
    private boolean warnedAboutAgeFixup;

    private TabManager() {}

    /** True while we are re-feeding messages into the ChatHud ourselves. */
    public boolean isSuppressed() { return suppress; }

    /**
     * Called from the ChatHud mixin for every incoming line. The original call is always
     * cancelled; if the active tab accepts the message we re-add a highlighted copy.
     */
    public void handleIncoming(ChatHud hud, Text message,
                               @Nullable MessageSignatureData signature,
                               @Nullable MessageIndicator indicator) {
        ChatTabsConfig cfg = ChatTabsConfig.get();
        cfg.clamp();

        MinecraftClient client = MinecraftClient.getInstance();
        String plain = message.getString();
        int tick = currentTick(client);
        StoredMessage stored = new StoredMessage(message, signature, indicator, tick, plain);

        List<TabDefinition> tabs = cfg.tabs;
        for (int i = 0; i < tabs.size(); i++) {
            TabDefinition tab = tabs.get(i);
            if (!tab.enabled || !tab.accepts(plain)) continue;
            tab.push(stored, cfg.maxStoredPerTab);
            if (i != cfg.activeTab) tab.unread++;
        }

        TabDefinition active = tabs.get(cfg.activeTab);
        if (!active.enabled || !active.accepts(plain)) return; // filtered out of view

        HighlightApplier.Result result = HighlightApplier.apply(message, active.highlights);
        if (result.ping() && cfg.highlightSounds) playPing(client);
        addRaw(hud, result.text(), signature, indicator);
    }

    public void setActiveTab(int index) {
        ChatTabsConfig cfg = ChatTabsConfig.get();
        if (cfg.tabs.isEmpty()) return;
        index = Math.floorMod(index, cfg.tabs.size());
        cfg.activeTab = index;
        cfg.tabs.get(index).unread = 0;
        ChatTabsConfig.save();
        rebuild();
    }

    public void cycleTab(int delta) {
        ChatTabsConfig cfg = ChatTabsConfig.get();
        if (cfg.tabs.size() < 2) return;
        setActiveTab(cfg.activeTab + delta);
    }

    /** Clears the vanilla chat and replays the active tab's scrollback into it. */
    public void rebuild() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.inGameHud == null) return;

        ChatHud hud = client.inGameHud.getChatHud();
        ChatTabsConfig cfg = ChatTabsConfig.get();
        TabDefinition tab = cfg.active();
        ChatHudAccessor accessor = (ChatHudAccessor) hud;

        suppress = true;
        try {
            hud.clear(false);
            List<ChatHudLine> lines = accessor.chattabs$getMessages();
            List<ChatHudLine.Visible> visible = accessor.chattabs$getVisibleMessages();

            for (StoredMessage stored : tab.messages()) {
                int before = visible.size();
                Text text = HighlightApplier.apply(stored.text(), tab.highlights).text();
                accessor.chattabs$addMessage(text, stored.signature(), stored.indicator());
                if (cfg.preserveMessageAgeOnSwitch) {
                    restoreAge(lines, visible, visible.size() - before, stored.creationTick());
                }
            }
        } finally {
            suppress = false;
        }
    }

    /**
     * Re-adding a message resets its fade timer. This rewrites the freshly inserted entries so
     * old messages stay faded out after a tab switch instead of all popping back into view.
     */
    private void restoreAge(List<ChatHudLine> lines, List<ChatHudLine.Visible> visible,
                            int addedVisible, int tick) {
        try {
            if (!lines.isEmpty()) {
                ChatHudLine line = lines.get(0);
                lines.set(0, new ChatHudLine(tick, line.content(), line.signature(), line.indicator()));
            }
            for (int i = 0; i < addedVisible && i < visible.size(); i++) {
                ChatHudLine.Visible v = visible.get(i);
                visible.set(i, new ChatHudLine.Visible(tick, v.content(), v.indicator(), v.endOfEntry()));
            }
        } catch (Throwable t) {
            if (!warnedAboutAgeFixup) {
                warnedAboutAgeFixup = true;
                ChatTabsClient.LOGGER.warn("ChatHudLine layout changed; disabling message-age restore", t);
            }
            ChatTabsConfig.get().preserveMessageAgeOnSwitch = false;
        }
    }

    private void addRaw(ChatHud hud, Text text,
                        @Nullable MessageSignatureData signature,
                        @Nullable MessageIndicator indicator) {
        suppress = true;
        try {
            ((ChatHudAccessor) hud).chattabs$addMessage(text, signature, indicator);
        } finally {
            suppress = false;
        }
    }

    private void playPing(MinecraftClient client) {
        // Entity#playSound is far more stable across versions than the
        // PositionedSoundInstance.master(...) overloads, which keep changing shape.
        if (client.player == null) return;
        client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.6F);
    }

    private int currentTick(MinecraftClient client) {
        try {
            return client.inGameHud.getTicks();
        } catch (Throwable t) {
            return 0;
        }
    }
}
