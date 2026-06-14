# Design task — BP Tracker Android: static HTML mockups of the key screens (for approval)

Produce **static, self-contained HTML mockups** (one file per screen, or one file with each screen in its own ~390×844 phone frame) of the native app's main screens, in the app's existing visual language. These are for visual approval before any Compose code — not production code. Make them look like a real modern Android (Material 3) app, dark theme first, with a light-theme variant of at least the Dashboard.

The native app is the "today + scan + meds" companion to an existing web app. It must feel like the same product as the web (same dark palette, neon zone accents, card style), but with fewer, simpler screens and Android conventions (bottom nav, Material components).

## Use these EXACT design tokens (taken from the live web app)

Dark theme (default):
- bg `#0d1117`; card surface `rgba(255,255,255,0.05)` (or solid `#141920`); border `rgba(255,255,255,0.08)`
- text `#ffffff`; muted `rgba(255,255,255,0.45)`; dim `rgba(255,255,255,0.25)`
- primary accent `#818cf8`
- Zone colors (the signature accent — used for the big reading and the badge):
  - optimal `#22c55e` (bg `rgba(34,197,94,0.12)`)
  - normal `#84cc16` (bg `rgba(132,204,22,0.12)`)
  - stage1 `#f97316` (bg `rgba(249,115,22,0.12)`)
  - stage2 `#ef4444` (bg `rgba(239,68,68,0.12)`)
- danger `#ef4444`, success `#22c55e`, warning `#f97316`

Light theme (for the variant):
- bg `#f2f3f5`; surface `#ffffff`; text `#0f0f12`; border `rgba(0,0,0,0.07)`
- zones darker: optimal `#16a34a`, normal `#65a30d`, stage1 `#c2410c`, stage2 `#dc2626` (bgs `#dcfce7/#ecfccb/#fed7aa/#fee2e2`)

Type: **DM Sans** (UI), **DM Mono** (numbers) — load from Google Fonts.
Radius: cards 14–20px, pills full. Spacing scale ~4/8/12/16/20/28.

Signature detail from the web: **the big SYS/DIA reading is tinted in the current zone color**, with a matching zone pill beside it.

## Screens to mock (the approved MVP set)

1. **Dashboard "Today"** (primary screen)
   - Top bar: "BP Tracker" wordmark + settings gear.
   - Hero card: last measurement — large `SYS/DIA` (mono, zone-tinted), zone pill, `mm Hg · ♡ pulse bpm`, timestamp, a small sparkline trend, and a horizontal "Normal → Danger" position bar.
   - A row of KPI tiles: avg 7d, week change (+/−), % in range (e.g. 11/14, 79%), avg pulse.
   - Preview of last 2 readings with a link to history.
   - "Today's medication" strip: pending intake(s) with a quick "Taken" action.
   - Bottom nav: two tabs **Dashboard / Schedule** with a large central **Scan** FAB.

2. **Schedule (Розклад)** — today's medication plan
   - List of periods (Morning / Day / Evening) with medicines + doses, each with status (taken / pending / missed) and a confirm action.

3. **Scan confirm (OCR review)**
   - Dark capture screen: the photo, recognized `SYS / DIA / PULSE` in editable fields (color dots per value), Cancel / Save, and a "recognize via server" fallback link.

4. **Manual entry** — simple form: SYS, DIA, pulse inputs + Save; inline validation hint (ranges 40–300 / 20–200 / 30–250).

5. **Measurement detail** — one reading enlarged + delete action.

6. **Settings** — theme (auto/light/dark segmented), language (UK/EN), "send photos to improve model" toggle, logout, and a **BP scale help** section (ESC table of 4 levels + the "Normal → Danger" scale + disclaimer).

## Reference for the dashboard feel (from the web app)
Cards on a near-black bg; big neon-green reading when in range; KPI tiles in a 2-col grid; a bottom bar with a glowing green circular scan button between two tabs. Match that energy, but use Material 3 Android conventions (e.g., a real bottom NavigationBar with a center docked FAB).

## BP zones (must match exactly)
optimal sys<120 & dia<80 · normal 120–139 or 80–89 · stage1 140–159 or 90–99 · stage2 ≥160 or ≥100 (tie-break: worse wins).

## Deliverable
- Self-contained HTML/CSS (inline), no build step, openable in a browser. SVG for sparkline/icons is fine.
- Each screen framed at a phone size (~390×844). Provide the Dashboard in both dark and light.
- Realistic sample data showing different zones across the mockups (one optimal, one stage1, etc.).
- This is a mockup for sign-off; once approved it becomes the spec for the Compose design system.

## Out of scope
Reminder notification UI, Health Connect, BLE, onboarding — not in this round.
