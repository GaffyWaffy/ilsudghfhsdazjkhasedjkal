package com.example.chattabs.config;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** A keyword/phrase that gets restyled (and optionally pinged) inside a tab. */
public class HighlightRule {

    public String pattern = "";
    public boolean regex = false;
    public boolean ignoreCase = true;
    /** RGB, no alpha. */
    public int color = 0xFFFF55;
    public boolean bold = false;
    public boolean underline = false;
    public boolean italic = false;
    /** Restyle the whole chat line instead of only the matched text. */
    public boolean wholeLine = false;
    public boolean playSound = true;

    private transient Pattern compiled;
    private transient String compiledFor;
    private transient boolean compiledIgnoreCase;

    public HighlightRule() {}

    public HighlightRule(String pattern, int color) {
        this.pattern = pattern;
        this.color = color;
    }

    public Pattern compiled() {
        if (pattern == null || pattern.isEmpty()) return null;
        String src = regex ? pattern : Pattern.quote(pattern);
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

    public void invalidate() {
        compiled = null;
        compiledFor = null;
    }
}
