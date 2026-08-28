package com.example.chattabs.config;

import com.example.chattabs.chat.StoredMessage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** A single chat tab: what it accepts, how it highlights, and its own scrollback. */
public class TabDefinition {

    public enum MatchMode {
        ANY, ALL;
        public MatchMode next() { return this == ANY ? ALL : ANY; }
        public String label() { return this == ANY ? "match any" : "match all"; }
    }

    public String name = "New Tab";
    public boolean enabled = true;
    public MatchMode mode = MatchMode.ANY;
    /** Optional text prepended to anything you send while this tab is active, e.g. "/msg Steve ". */
    public String sendPrefix = "";
    public boolean showUnreadBadge = true;
    public List<FilterRule> filters = new ArrayList<>();
    public List<HighlightRule> highlights = new ArrayList<>();

    private transient Deque<StoredMessage> messages;
    public transient int unread;

    public TabDefinition() {}

    public TabDefinition(String name) { this.name = name; }

    public Deque<StoredMessage> messages() {
        if (messages == null) messages = new ArrayDeque<>();
        return messages;
    }

    public void push(StoredMessage message, int cap) {
        Deque<StoredMessage> q = messages();
        q.addLast(message);
        while (q.size() > cap) q.removeFirst();
    }

    /**
     * Exclusion rules win outright. If there are no inclusion rules the tab is a catch-all,
     * otherwise ANY/ALL decides.
     */
    public boolean accepts(String plainLine) {
        boolean sawInclude = false;
        boolean anyMatched = false;
        boolean allMatched = true;

        for (FilterRule rule : filters) {
            if (rule.pattern == null || rule.pattern.isEmpty()) continue;
            boolean matched = rule.matches(plainLine);
            if (rule.invert) {
                if (matched) return false;
            } else {
                sawInclude = true;
                if (matched) anyMatched = true; else allMatched = false;
            }
        }
        if (!sawInclude) return true;
        return mode == MatchMode.ALL ? allMatched : anyMatched;
    }

    public void invalidateCaches() {
        filters.forEach(FilterRule::invalidate);
        highlights.forEach(HighlightRule::invalidate);
    }
}
