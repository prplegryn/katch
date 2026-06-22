# Akatcha UI System

Akatcha uses a custom Jetpack Compose interface focused on a downloader workflow rather than a promotional or decorative layout.

## Direction

- Neutral tool palette with semantic success, warning, danger, and accent states.
- Flat app shell with persistent header, bottom navigation, and clear page ownership.
- Low-radius panels only for framed tools and configuration groups.
- Format results are list rows with table-like metadata instead of independent decorative cards.
- No animated color fields, glass surfaces, oversized hero copy, or nested cards.
- 4/8dp spacing rhythm, 42dp+ touch targets, stable row heights, and adaptive max-width for larger screens.
- Light, dark, and system theme modes use the same semantic tokens.

## Main Hierarchy

- Task: input source, environment readiness, probe progress, raw format list, and saved files.
- Login: browser toolbar, desktop-UA WebView, and cookie save state.
- Settings: appearance, output directory, cookie management, and extraction parameters.

## Business Rules Reflected In UI

- Cookie and storage are shown as operational prerequisites, not hidden in settings.
- The format list keeps original order and duplicate rows visible because the downloader intentionally preserves every returned item.
- Download progress and published files stay in the task flow so users can verify output without switching sections.
- Login is separated from task execution, but still reachable from task readiness controls.
