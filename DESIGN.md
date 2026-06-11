# Akatcha UI System

The app intentionally avoids default Material component styling. Compose is used for rendering and accessibility primitives, while the visual system is custom:

- Gradient canvas background with animated soft color fields.
- Glass-style cards with high contrast text surfaces.
- Custom capsule buttons, segmented controls, status pills, and bottom dock.
- Self-drawn line icons on Canvas to keep icon weight consistent.
- Page slide/fade transitions, animated progress, expandable status cards, animated list placement, and loading pulse states.
- 4/8dp spacing rhythm, 48dp minimum touch targets, large Chinese-readable line heights, and adaptive max-width for tablets.
- Light, dark, and system theme modes using semantic color tokens.

The main hierarchy is:

- Download: link input, environment status, probe status, full format list, per-format download action.
- Login: desktop-UA WebView with right-side cookie save action.
- Settings: theme, output directory, cookie management, and probe strategy.
