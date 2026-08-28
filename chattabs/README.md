# Chat Tabs (Fabric, Minecraft 1.21.11)

LabyMod-style chat tabs for vanilla Minecraft: multiple tabs with their own filters and
scrollback, keyword/phrase highlighting, and a chat window you can drag around and resize.

## How it works

Rather than reimplementing chat rendering (which breaks on every Minecraft update), the mod
intercepts `ChatHud.addMessage(Text, MessageSignatureData, MessageIndicator)`, files each line
into every tab whose filters accept it, and only re-feeds the **active** tab's lines back into
the vanilla `ChatHud`. Switching tabs clears the hud and replays that tab's buffer. You keep
vanilla word wrap, scrolling, message signing indicators, clickable links and hover text for free.

| File | Purpose |
| --- | --- |
| `mixin/ChatHudMixin` | Routes messages, overrides `getWidth`/`getHeight`, translates the render, draws the strip |
| `mixin/ChatHudAccessor` | Reaches the private `messages` / `visibleMessages` lists and `addMessage` |
| `mixin/ChatScreenMixin` | Tab clicks, drag-to-move, drag-to-resize, Ctrl+Tab, send prefixes |
| `chat/TabManager` | Routing + rebuilding the hud on tab switch |
| `chat/HighlightApplier` | Restyles matched keywords without destroying existing styles/events |
| `config/*` | GSON-backed settings in `config/chattabs.json` |
| `gui/*` | Tab strip geometry/drawing and the two settings screens |

## Controls

- **Left click a tab** – switch to it
- **Right click a tab** – open its filter/highlight editor
- **Middle click a tab** – delete it
- **Scroll over the strip** – cycle tabs
- **Ctrl+Tab / Ctrl+Shift+Tab** – next / previous tab while chat is open
- **Drag the empty part of the strip** – move the whole chat window
- **Drag the white square (top-right of the chat box)** – resize
- **`+` at the end of the strip** – new tab
- **`/chattabs`** – full settings screen

## Filter semantics

Each tab holds a list of rules. Rule types: contains, starts with, ends with, equals, whole word,
regex. Each rule is either **include** or **exclude**.

- Any matching *exclude* rule rejects the line outright.
- If a tab has no *include* rules it is a catch-all (that's how the default "All" tab works).
- Otherwise `match any` / `match all` decides.

Defaults ship with **All**, **Chat** (`^<[^>]+>`), **Whispers**, and **System** (everything that
isn't player chat or a whisper).

## Highlights

Per tab. Each rule has a keyword or regex, a hex colour, bold/underline toggles, a
"word vs whole line" toggle, and an optional ping sound. Matching is done on the flattened plain
text so a phrase still matches when it's split across differently-coloured siblings.

Add your own IGN as a highlight in the **All** tab — that's the usual reason people want this.

## Building

```
./gradlew build      # jar lands in build/libs/
./gradlew runClient  # test in a dev client
```

Requires JDK 21. Check https://fabricmc.net/develop and update `gradle.properties` with the
current `yarn_mappings` and `fabric_version` builds for 1.21.11 before your first build — the
values in there are placeholders.

## Things to check if it doesn't compile

Two spots are version-sensitive and are the first place to look if Loom or Mixin complains:

1. **`ChatHudMixin.chattabs$shiftBefore/shiftAfter`** assume
   `ChatHud.render(DrawContext, int currentTick, int mouseX, int mouseY, boolean focused)` and the
   JOML `Matrix3x2fStack` API (`pushMatrix()` / `translate(x, y)` / `popMatrix()`). If Mojang
   changed the signature, match the handler parameters to the real one.
2. **`TabManager.restoreAge`** constructs `ChatHudLine` / `ChatHudLine.Visible` records to stop old
   messages from popping back to full opacity on a tab switch. It's wrapped in a try/catch and
   self-disables if the record layout changed, so it degrades rather than crashes.

## Known trade-offs

- Tabs buffer messages independently, so a line matching four tabs is stored four times (text
  objects are shared references, so the cost is small). Cap is `maxStoredPerTab`.
- Tab buffers are in-memory only and reset when the game closes.
- Resizing re-wraps by rebuilding the hud, which is O(scrollback) — fine at 1000 lines, noticeable
  if you push the cap to 5000 and drag continuously.
