package com.example.chattabs.chat;

import com.example.chattabs.config.HighlightRule;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rebuilds a chat line with keyword styling applied.
 *
 * The text is flattened into a plain string plus a per-character style array, so matches
 * can safely span sibling boundaries (e.g. a phrase split across a coloured prefix).
 * Original styles (including click/hover events) are preserved and only overridden where a
 * highlight rule actually matches.
 */
public final class HighlightApplier {

    public record Result(Text text, boolean matched, boolean ping) {}

    private HighlightApplier() {}

    public static Result apply(Text input, List<HighlightRule> rules) {
        if (rules == null || rules.isEmpty()) return new Result(input, false, false);

        StringBuilder sb = new StringBuilder();
        List<Style> charStyles = new ArrayList<>();
        input.visit((style, string) -> {
            sb.append(string);
            for (int i = 0; i < string.length(); i++) charStyles.add(style);
            return Optional.empty();
        }, Style.EMPTY);

        String plain = sb.toString();
        if (plain.isEmpty()) return new Result(input, false, false);

        int len = plain.length();
        int[] ruleAt = new int[len];
        java.util.Arrays.fill(ruleAt, -1);

        boolean matched = false;
        boolean ping = false;
        boolean wholeLine = false;
        int wholeLineRule = -1;

        for (int r = 0; r < rules.size(); r++) {
            HighlightRule rule = rules.get(r);
            Pattern pattern = rule.compiled();
            if (pattern == null) continue;
            Matcher m = pattern.matcher(plain);
            boolean ruleHit = false;
            while (m.find()) {
                if (m.end() == m.start()) { // guard against zero-width regexes
                    if (m.end() >= len) break;
                    continue;
                }
                ruleHit = true;
                for (int i = m.start(); i < m.end(); i++) ruleAt[i] = r;
            }
            if (ruleHit) {
                matched = true;
                if (rule.playSound) ping = true;
                if (rule.wholeLine && !wholeLine) { wholeLine = true; wholeLineRule = r; }
            }
        }

        if (!matched) return new Result(input, false, false);
        if (wholeLine) java.util.Arrays.fill(ruleAt, wholeLineRule);

        MutableText out = Text.empty();
        int start = 0;
        for (int i = 1; i <= len; i++) {
            boolean boundary = i == len
                    || ruleAt[i] != ruleAt[start]
                    || !charStyles.get(i).equals(charStyles.get(start));
            if (!boundary) continue;
            Style base = charStyles.get(start);
            Style effective = ruleAt[start] < 0 ? base : decorate(base, rules.get(ruleAt[start]));
            out.append(Text.literal(plain.substring(start, i)).setStyle(effective));
            start = i;
        }
        return new Result(out, true, ping);
    }

    private static Style decorate(Style base, HighlightRule rule) {
        Style style = base.withColor(TextColor.fromRgb(rule.color & 0xFFFFFF));
        if (rule.bold) style = style.withBold(true);
        if (rule.italic) style = style.withItalic(true);
        if (rule.underline) style = style.withUnderline(true);
        return style;
    }
}
