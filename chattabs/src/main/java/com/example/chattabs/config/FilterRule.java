package com.example.chattabs.config;

import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** One matching rule used to decide whether a chat line belongs to a tab. */
public class FilterRule {

    public enum Type {
        CONTAINS, STARTS_WITH, ENDS_WITH, EQUALS, WORD, REGEX;

        public Type next() { return values()[(ordinal() + 1) % values().length]; }

        public String label() {
            return switch (this) {
                case CONTAINS -> "contains";
                case STARTS_WITH -> "starts with";
                case ENDS_WITH -> "ends with";
                case EQUALS -> "equals";
                case WORD -> "whole word";
                case REGEX -> "regex";
            };
        }
    }

    public Type type = Type.CONTAINS;
    public String pattern = "";
    public boolean ignoreCase = true;
    /** When true this becomes an exclusion: any match immediately rejects the line. */
    public boolean invert = false;

    private transient Pattern compiled;
    private transient String compiledFor;
    private transient boolean compiledIgnoreCase;

    public FilterRule() {}

    public FilterRule(Type type, String pattern, boolean invert) {
        this.type = type;
        this.pattern = pattern;
        this.invert = invert;
    }

    public boolean matches(String line) {
        if (pattern == null || pattern.isEmpty()) return false;
        if (type == Type.REGEX || type == Type.WORD) {
            Pattern p = compile();
            return p != null && p.matcher(line).find();
        }
        String hay = ignoreCase ? line.toLowerCase(Locale.ROOT) : line;
        String needle = ignoreCase ? pattern.toLowerCase(Locale.ROOT) : pattern;
        return switch (type) {
            case CONTAINS -> hay.contains(needle);
            case STARTS_WITH -> hay.startsWith(needle);
            case ENDS_WITH -> hay.endsWith(needle);
            case EQUALS -> hay.equals(needle);
            default -> false;
        };
    }

    private Pattern compile() {
        String src = type == Type.WORD ? "\\b" + Pattern.quote(pattern) + "\\b" : pattern;
        if (compiled != null && src.equals(compiledFor) && compiledIgnoreCase == ignoreCase) return compiled;
        try {
            compiled = Pattern.compile(src, ignoreCase ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0);
        } catch (PatternSyntaxException e) {
            compiled = null;
        }
        compiledFor = src;
        compiledIgnoreCase = ignoreCase;
        return compiled;
    }

    /** Drops cached state so edits in the GUI take effect immediately. */
    public void invalidate() {
        compiled = null;
        compiledFor = null;
    }
}
