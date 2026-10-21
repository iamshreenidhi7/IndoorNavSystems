# DESIGN: Indoor Navigation and Path Optimization System

| Field | Value |
|---|---|
| Status | Draft v0.1, awaiting owner approval on flagged decisions |
| Depends on | PRD.md (screens, states, roles, requirement IDs) |
| Audience | Mobile developer, AI coding agents, reviewers |
| Scope | Visual design and UX of the Flutter mobile app (Android and iOS). No code is defined here. |

## 0. How to use this document

- This file is the single source of truth for look, feel and interaction. If a screen or component is not covered, extend the system here first, then build.
- **Tokens are semantic.** Components refer to roles such as "route" or "on-surface", never to raw colour values or magic numbers. Raw values appear only in this document's token tables and in one central theme definition.
- **[ASSUMPTION DA-n]** marks something assumed. **[DECISION DD-n]** marks a choice needing owner approval; all are collected in Section 15 with a recommended default. Agents follow the default until a decision is made.
- **Platform note [ASSUMPTION DA-1]:** the PRD defines a mobile app (Flutter, Android and iOS) with no web target. "Responsive" in this document therefore means phone sizes, orientations, tablets and font scaling, not browser layouts.
- The product is a **generic platform**. Nothing in the design may be specific to shopping malls. Environment-specific content (names, categories, floor labels, plans) comes from data.
- Material 3 is the component foundation (per the Master Project Prompt). It is customised through the tokens below; components are not restyled one-off.

---

## 1. Visual direction

### 1.1 Direction in one sentence

**A calm, paper-like map with transit-signage clarity: quiet neutrals everywhere, and one assertive colour reserved for the route.**

### 1.2 What this means in practice

- **Signage legibility over decoration.** Large type, strong contrast, simple pictograms, clear hierarchy, the way good airport and transit wayfinding works.
- **Map first.** The map fills the screen. Controls live in bottom sheets and a small number of floating controls, within thumb reach.
- **Flat and bordered.** Structure comes from spacing, thin outlines and tonal surfaces. Shadows are used only for elements floating above the map.
- **One accent for the route.** The route colour appears nowhere else, so the route is always the most visible thing on screen.
- **Functional icons only.** Icons communicate meaning (stairs, elevator, restroom). No decorative icons or illustrations beyond empty and error states.

### 1.3 Explicitly avoided

Gradients, glassmorphism, neon or purple "AI" palettes, oversized hero banners, stock illustrations, decorative animation, card-inside-card stacking, colour-coded rainbow category schemes, and dashboard-style widgets on visitor screens.

### 1.4 Branding

[ASSUMPTION DA-2] The product has no name or logo yet. The splash and home screens use a neutral text wordmark placeholder. Per-environment branding (a building's own colours or logo) is out of scope for the MVP; the platform uses one theme.

---

## 2. Design principles

1. **Be honest about what the app knows.** The start location is user-selected, never "You are here". Times are estimates. No language implies live tracking (PRD FR-12, FR-21).
2. **The route is the hero.** Every route-related screen makes the route, its distance and time, and any floor changes the clearest elements.
3. **Thumb first.** Primary actions sit in the lower half of the screen. Nothing essential requires reaching the top corners.
4. **Stay calm under disruption.** Blocked routes are normal events. Warnings state what happened, what changed, and what to do next, in plain words and without alarm styling beyond what the situation needs.
5. **Never rely on colour alone.** Every state pairs colour with an icon, a label or a pattern.
6. **Plain language.** Short sentences, everyday words, one consistent term per concept.
7. **Consistency over novelty.** The same pattern is used for the same job everywhere. New patterns require an update to this document.
8. **Platform-neutral.** No mall-specific visuals, wording or assumptions in the core design.

---

## 3. Colour

### 3.1 Palette

Light theme only for the MVP **[DECISION DD-2]**. Tokens are named by role so a dark theme can be added later without changing components.

**Brand and action**

| Token | Value | Use |
|---|---|---|
| primary | #0B5D6B | Primary buttons, selected states, links, focus ring, stairs/elevator map icons |
| on-primary | #FFFFFF | Text and icons on primary |
| primary-container | #CFE8EC | Selected list items, tonal buttons, chips |
| on-primary-container | #052E35 | Text and icons on primary-container |

**Route (reserved)**

| Token | Value | Use |
|---|---|---|
| route | #D9480F | Route line, destination marker, floor-transition marker. **Graphics only. Never used for body text, buttons or decoration.** |
| route-casing | #FFFFFF | 2 dp outline around the route line for separation from the map |
| route-container | #FFE3D1 | Backgrounds for route-related chips and callouts |
| on-route-container | #5A1E00 | Text on route-container |

**Neutrals**

| Token | Value | Use |
|---|---|---|
| background | #F7F5F0 | App background |
| surface | #FFFFFF | Sheets, cards, app bars, dialogs |
| surface-variant | #ECE8DF | Subtle grouping, skeleton blocks, disabled fills |
| on-surface | #1C1B17 | Primary text and icons |
| on-surface-secondary | #55524A | Secondary text, captions |
| outline | #7A7568 | Input borders, dividers that must be visible |
| outline-subtle | #D9D4C8 | Decorative separators only (never the sole indicator of a boundary) |

**Semantic status**

| Token | Value | Container | On-container | Use |
|---|---|---|---|---|
| error | #B3261E | #F9DEDC | #410E0B | Failed operations, validation errors, blocked connections |
| warning | #8A5A00 | #FFF4D6 | #3A2600 | Route affected by an obstacle, detour notices |
| success | #1B6E3C | #D6F0DF | #0A2E18 | Confirmations (obstacle resolved, saved) |

**Map-specific**

| Token | Value | Use |
|---|---|---|
| map-background | #EFEBE2 | Area outside the floor plan |
| map-walkable | #FFFFFF | Corridors and open walkable areas |
| map-space | #E4DFD3 | Rooms, shops and other spaces |
| map-wall | #5C584E | Walls and space outlines |
| map-start | primary | Start marker |
| map-destination | route | Destination marker |
| map-blocked | error | Blocked connection overlay |

### 3.2 Colour rules

- **Contrast:** text and meaningful icons meet at least 4.5:1 (3:1 for text 18 sp and larger or bold 14 sp and larger). Graphics that carry meaning (route line, markers, input borders) meet at least 3:1 against their background. The pairs above are chosen for this; verify with a contrast tool when implemented.
- **Text on the route colour:** white on route falls below 4.5:1 for small text, so text never sits directly on route. Use route-container with on-route-container.
- **Categories are not colour-coded.** Destinations are distinguished by icon and label. Spaces share one neutral fill.
- **Status uses container pairs.** A warning is warning-container with an icon, a title and on-container text. Never coloured text alone on the background.
- **Colour-blind safety:** route (orange), primary (teal) and blocked (red) are always accompanied by different shapes or patterns (Section 7.3), so they remain distinguishable without hue.

---

## 4. Typography

### 4.1 Typeface

**Atkinson Hyperlegible**, bundled with the app, with the platform system font as the fallback **[DECISION DD-1]**. It is open-licensed and designed for character distinction at small sizes, which suits a utility used on the move, and avoids the generic default look.

Fallback if the owner prefers zero custom fonts: the platform default (Roboto on Android, San Francisco on iOS). The scale below applies unchanged.

### 4.2 Type scale

Sizes are in scalable pixels (sp) and must respect the user's system font size.

| Role | Size / line height | Weight | Use |
|---|---|---|---|
| display | 32 / 40 | Bold | Splash wordmark, key route figures (distance, time) on route summary |
| headline | 24 / 32 | Bold | Screen titles when large, empty-state titles |
| title | 20 / 28 | Medium | App bar titles, sheet titles, destination names on selection |
| body-large | 18 / 26 | Regular | **Navigation instructions** (the most-read text in the app) |
| body | 16 / 24 | Regular | Default body, list primary text, form inputs |
| body-small | 14 / 20 | Regular | Secondary text, descriptions, helper text |
| label | 14 / 20 | Medium | Buttons, chips, tabs, form labels |
| caption | 12 / 16 | Regular | Map labels at minimum, timestamps. **Nothing smaller than 12 sp.** |

### 4.3 Typography rules

- Sentence case everywhere. No all-caps except floor indicators such as "G" where the data demands it.
- Maximum two weights per screen (regular and medium or bold).
- Numbers with units use a single space and standard abbreviations: "145 m", "2 min".
- Line length on tablets is capped at about 70 characters; use the centred content width in Section 6.
- Map labels use a light halo in map-walkable colour for legibility over lines and fills.
- Text must remain usable at 200% system font scale (see Section 12).

---

## 5. Spacing, shape and elevation

### 5.1 Spacing

Base unit is **4 dp**. Only these steps are used.

| Token | Value | Typical use |
|---|---|---|
| space-1 | 4 | Icon-to-label gap, tight inline gaps |
| space-2 | 8 | Gap between related items, chip spacing |
| space-3 | 12 | List item vertical padding, compact gaps |
| space-4 | 16 | Screen margin (compact), card padding, standard gap |
| space-6 | 24 | Section separation, screen margin (medium and above) |
| space-8 | 32 | Large separation, empty-state spacing |
| space-12 | 48 | Hero spacing on splash, empty states |

No other values. Padding inside a component is defined by the component, not by the screen.

### 5.2 Touch targets

- Minimum interactive size: **48 × 48 dp**, with at least 8 dp between adjacent targets.
- Map controls and the floor switcher use **56 dp** targets because they are used one-handed while moving.
- Small visual elements (map markers, 12 dp nodes) receive a 48 dp invisible hit area.

### 5.3 Shape

| Token | Value | Use |
|---|---|---|
| radius-small | 8 dp | Chips, banners, text fields, small cards |
| radius-medium | 12 dp | Cards, list containers, dialogs |
| radius-large | 28 dp | Top corners of bottom sheets |
| radius-full | Pill | Buttons, search field, floor switcher |

### 5.4 Elevation

- Default surfaces are flat with an outline-subtle border where separation is needed.
- **Floating over the map** (bottom sheet, search entry, map controls, floor switcher): one shadow level, identical for all.
- Dialogs use the standard Material 3 modal elevation and a scrim.
- No additional shadow levels.

### 5.5 Iconography

- One icon family: Material Symbols, outlined style, consistent weight. 24 dp default, 20 dp in dense lists, 32 dp for map markers.
- Fixed icon meanings, used the same everywhere: stairs, elevator, ramp, entrance, restroom, information, search, floor, warning, blocked, accessible.
- Icons for connection types always appear with a text label wherever space allows, and always in instructions.

---

## 6. Layout and grid

### 6.1 Window classes

| Class | Width | Layout |
|---|---|---|
| Compact | Under 600 dp | Phones in portrait. Single column. Map full-bleed with bottom sheet. |
| Medium | 600 to 839 dp | Large phones landscape, small tablets. Side panel plus map. |
| Expanded | 840 dp and up | Tablets. Persistent side panel plus map, content centred on non-map screens. |

### 6.2 Layout rules

- Screen margin: 16 dp (compact), 24 dp (medium and expanded).
- Non-map screens (lists, forms) cap content width at **600 dp**, centred on larger windows.
- All layouts respect system safe areas (notches, home indicators, system bars).
- Vertical rhythm uses space-4 between groups and space-2 within groups.
- Primary action buttons sit at the bottom of their container, full width on compact, never hidden behind the keyboard.

### 6.3 The map screen shell

The indoor map screen is the hub of the app. It always has this structure:

1. **Map canvas**, full-bleed, below the status bar.
2. **Top overlay**: back/menu button on the left, search entry (pill) filling the remaining width.
3. **Floating controls**, right edge: floor switcher (vertical) and zoom in/out and "fit route" below it.
4. **Bottom sheet**, persistent, with defined snap points (Section 8.4), holding the contextual panel.

On medium and expanded windows the bottom sheet becomes a **left side panel** (320 to 400 dp wide, full height, flat surface) and the controls stay on the map's right edge.

---

## 7. Map design

The map is the product's core visual. These rules apply to every environment.

### 7.1 Layers (bottom to top)

1. map-background
2. Floor plan (walkable areas, spaces, walls)
3. Space and POI labels
4. Facility icons (restrooms, information, entrances, stairs, elevators, ramps)
5. Blocked-connection overlay
6. Route line (current floor segment)
7. Floor-transition markers
8. Start and destination markers
9. Navigation nodes (debug mode only, off by default)

### 7.2 Route rendering

| Element | Rule |
|---|---|
| Route line | 6 dp wide, route colour, round caps and joins, with 2 dp route-casing outline |
| Segment on another floor | Not drawn. The floor switcher marks floors that contain route segments (7.4) |
| Floor-transition marker | 32 dp circular marker in route colour with white connection-type icon (stairs or elevator) and a text label such as "Elevator to Floor 2" |
| Start marker | 32 dp circle, primary colour fill, white ring, with "Start" label |
| Destination marker | 40 dp pin shape, route colour, white outline, destination name label |
| Initial view | Fit route bounds with 24 dp padding plus the sheet height; fit building bounds when no route |
| Reveal | Route line draws from start to end once on first display (Section 9), static if reduced motion is on |

### 7.3 Blocked connections

- Drawn in map-blocked colour as a **dashed line** (8 dp dash, 4 dp gap, 4 dp wide) with a **✕ badge** (24 dp) at the midpoint.
- The badge is tappable (48 dp hit area) and opens a small sheet naming the obstacle type and description.
- The pattern plus badge ensures it is identifiable without colour.

### 7.4 Floor switcher

- Vertical pill, 56 dp wide, listing floors top to bottom in descending level order (highest at top), labels taken from data.
- Selected floor: primary fill with on-primary text. Others: surface with on-surface text.
- A small route-colour dot at the corner of a floor button indicates "route has a segment here". The dot is accompanied by the accessible label "route on this floor".
- Switching floors cross-fades the plan (Section 9) and keeps zoom and pan position.
- Maximum visible floors without scrolling: 6. Beyond that, the switcher scrolls within its pill.

### 7.5 Labels, zoom and gestures

- Zoom range: fit-to-building up to 4× that level. Pan is constrained so the plan can never leave the screen entirely.
- Label visibility by zoom: facility icons always visible; space names appear once their space is large enough to hold the text at 12 sp; POI names appear at medium zoom and above. Labels never overlap; higher-priority items (destination, start, transition, blocked badge) win.
- Gestures: pinch to zoom, drag to pan, double-tap to zoom in. **Every gesture has a button alternative** (zoom in, zoom out, fit route).
- Tapping a destination, POI or space selects it and opens the destination panel. Tapping empty map clears the selection.
- Selecting a **start** on the map requires an explicit confirmation ("Set as start") to prevent accidental changes. Selection highlights the tapped item with a 3 dp primary outline and shows its label.

---

## 8. Components

Each component has one definition. Variants are listed; no others may be created without updating this section.

### 8.1 Buttons

| Variant | Use | Style |
|---|---|---|
| Primary (filled) | The single main action on a screen or sheet ("Get route", "Start navigation", "Save") | primary fill, on-primary label, pill, minimum height 48 dp |
| Tonal | Secondary important action ("Choose start") | primary-container fill, on-primary-container label |
| Outlined | Alternative actions ("Cancel", "Change") | outline border, primary label |
| Text | Low-emphasis actions, inline links | primary label, no container |
| Destructive | Delete confirmations only | error fill, white label, only inside confirmation dialogs |

Rules: one primary button per view; buttons carry a text label (icon-only buttons only for map controls and app bar actions, with accessible names); disabled buttons use surface-variant fill with on-surface-secondary text and are explained by nearby helper text when the reason is not obvious.

### 8.2 App bar

- Standard top app bar, surface colour, title in title style, back button on the left when applicable.
- Map screen uses the floating top overlay instead of an app bar (6.3).
- Admin screens use the standard app bar with a visible "Admin" label to distinguish context.

### 8.3 Search

- **Search entry** (pill on the map screen): leading search icon, placeholder "Search destinations", opens the full-screen search on tap.
- **Search screen:** auto-focused field in the app bar, category chips directly beneath, results list below. Results update as the user types after a short delay. Clear button appears when text is present.
- **Result tile:** leading icon (category), primary text (name), secondary text (floor and category), minimum height 56 dp, whole tile tappable.

### 8.4 Bottom sheet (persistent, on the map screen)

A single sheet whose content changes by **state**, with three snap points: **collapsed** (shows summary only, about 120 dp), **half** (about 45% of height), **expanded** (up to 90%, only for long instruction lists).

| State | Content | Primary action |
|---|---|---|
| Explore | Short prompt and search shortcut, recent or nearby categories | None (search entry is above) |
| Destination selected | Destination name, floor, category, "Start" selector row | Get route |
| Route preview | Route summary card, preference selector row, warnings | Start navigation |
| Navigating | Current instruction and step list, progress through steps | Next step, plus "End navigation" as text action |
| Rerouted / No route | Warning or no-route state (Section 11) | Contextual (see 11) |

The sheet has a drag handle (visible, 32 × 4 dp) and can also be expanded and collapsed via tap on the handle for users who cannot drag.

### 8.5 Route summary card

Used on the route preview and as the sheet header while navigating.

- Row 1: **display-style** figures: estimated time ("Est. 3 min walk") and distance ("145 m"). The word "Est." is always present.
- Row 2: preference chip (for example "Fastest") that opens the preference selector.
- Row 3 (conditional): floor-change summary chip ("Via elevator, Floor 1 to Floor 2").
- Row 4 (conditional): warning banner (8.8).

### 8.6 Preference selector

Presented as a **modal bottom sheet** (screen 7). A single-choice radio list with five options in a fixed order: Shortest, Fastest, Accessible, Avoid stairs, Prefer elevator.

- Each option shows a leading icon, a title (label) and a one-line description in plain language (for example "Accessible: only step-free routes. Stairs are never used.").
- The currently selected option is highlighted with primary-container.
- Accessibility setting (device-level) shows a notice when enabled: "Accessible is selected by default. You can change it for this route."
- Confirming applies the selection and recalculates the route immediately.

### 8.7 Instruction step

- Leading icon (walk, turn, stairs, elevator, ramp, arrival), instruction text in **body-large**, secondary line with segment distance.
- **Floor-transition steps** are visually distinct: route-container background, the transition icon, and bold text ("Take the elevator to Floor 2"). They are the only steps with a tinted background.
- The current step is highlighted with an outline in primary and is scrolled into view; completed steps are shown in on-surface-secondary with a check icon.
- Steps are manual: the user advances with Next and Previous; the UI never implies automatic progress.

### 8.8 Banners and messages

| Component | Use | Style |
|---|---|---|
| Inline banner | Warning on an active route, obstacle notice, non-blocking error | Container colour by status, leading status icon, title (label) + one-line message, optional text action; radius-small |
| Snackbar | Brief confirmation after an action (saved, resolved) | Standard Material snackbar, 4 seconds, optional single action |
| Dialog | Confirmations and destructive actions only | Title, short message, two buttons (outlined cancel, primary or destructive confirm) |
| Full-state view | Whole-screen or whole-panel empty, error, no-route | See Section 11 |

Rules: banners never stack more than two; a persistent problem stays as a banner, a transient confirmation is a snackbar; errors never use snackbars alone when the user must act.

### 8.9 Chips

- **Filter chips** (categories): selectable, 32 dp visual height with 48 dp hit area, primary-container when selected.
- **Info chips** (preference, floor change): non-interactive or opening a selector; label style.

### 8.10 Lists and tiles

- Standard list tile: 56 dp minimum, leading icon, primary and secondary text, optional trailing element (status badge, chevron).
- Dividers use outline-subtle, inset to align with text. Lists have no card-per-row decoration.
- **Status badge** (obstacle status): text label always present ("Active", "Resolved"), with container colour; never colour alone.

### 8.11 Form fields

See Section 10 for behaviour. Visual rules: outlined text field, radius-small, 56 dp height (multi-line grows), label above the field inside the container when focused (standard Material behaviour), helper text in body-small beneath, error text in error colour with an error icon beneath.

### 8.12 Loading components

- **Skeleton list:** surface-variant blocks matching tile layout, 3 to 6 placeholder rows, a gentle shimmer-free pulse (Section 9).
- **Spinner:** circular indicator, primary colour, used for short waits inside buttons and small areas.
- **Blocking progress:** a centred spinner with a short message and a Cancel action, used for route calculation.

### 8.13 Empty and error views

A reusable structure: optional icon (48 dp, on-surface-secondary), title (headline or title), one-sentence explanation (body), and up to two actions (primary, text). Used for empty, error and no-route states (Section 11).

---

## 9. Interactions and animation

### 9.1 Principles

Motion explains change (a sheet moving, a floor changing, a route appearing). It is never decorative. Everything is skippable by a **reduce-motion** setting.

### 9.2 Timing

| Token | Duration | Use |
|---|---|---|
| motion-fast | 100 ms | Selection feedback, chip toggles |
| motion-standard | 200 ms | Floor cross-fade, banner appear, sheet content swap |
| motion-slow | 300 ms | Sheet snap, screen transitions |
| route-reveal | 600 ms | One-time route line draw |

Easing: standard Material 3 emphasised easing for entrances, standard easing for exits. No bounces or springs.

### 9.3 Behaviours

| Interaction | Behaviour |
|---|---|
| Screen navigation | Standard platform transitions |
| Floor change | Plan cross-fades; zoom and pan preserved; the switcher selection moves instantly |
| Route appears | Line draws from start to end once; map fits to route bounds with animated camera (motion-slow) |
| Reroute after obstacle | Previous route fades out, new route draws, warning banner slides in; camera refits |
| Sheet snap | Smooth to nearest snap point after release |
| Next / previous step | Highlight moves; the map recentres on the step's segment (motion-standard) |
| Loading to content | Skeletons cross-fade to content (motion-fast) |
| Long-press | Not used for essential actions |
| Haptics | Light haptic on floor change and step advance only, where the platform supports it |

### 9.4 Reduced motion

When the system reduce-motion setting is on: no route draw animation, no camera animation (jump cut), cross-fades become instant swaps, skeletons are static. Information is never conveyed by motion alone.

---

## 10. Navigation structure and forms

### 10.1 Navigation model

Stack-based navigation with the map as the hub; no bottom tab bar. The app is a single-task tool, so tabs would add chrome without value.

```
Splash
 └─ Home
     ├─ Building selection ─┐
     └─ (continue last) ────┴─ Indoor map (hub)
                               ├─ Destination search (full screen, returns to map)
                               ├─ Preference selector (modal sheet)
                               ├─ Route details / Navigation (sheet states)
                               └─ Obstacle warning / no route (sheet and banner states)
     └─ Admin sign-in ─ Admin obstacle management ─ Obstacle form
```

Rules:

- Floor selection is **not a separate page** in normal use; it is the floor switcher on the map. The PRD's "Floor selection" screen is satisfied by this control, with a floor list sheet as the accessible alternative (long lists, screen-reader users).
- Route details and navigation are **states of the map's bottom sheet** (8.4), not separate pages, so the route stays visible.
- The system back action steps back through sheet states first (navigating → preview → destination selected → explore), then leaves the map.
- Ending navigation asks for confirmation only if more than one step remains.
- State survives rotation and brief backgrounding: selected building, floor, destination, start and preference are retained.
- **Admin entry [DECISION DD-6]:** a low-emphasis "Admin" text action in the Home screen's overflow menu, not visible to normal visitors in the primary flow.

### 10.2 Form principles

- Labels are always visible (persistent label or floating label). Placeholders are examples, not labels.
- Validation runs when a field loses focus and on submit; errors never appear while the user is still typing the first entry.
- Error text sits directly below its field, in plain language ("Enter a description of at least 3 characters").
- On submit with errors, focus moves to the first invalid field and the error is announced to screen readers.
- Required fields are marked with "required" in the label text, not an asterisk alone.
- The keyboard type matches the input; the keyboard never covers the primary button (the layout scrolls).
- Destructive actions always use a confirmation dialog naming the item.

### 10.3 Specific forms

**Search:** single text field, no submit button needed; clear control; results update live.

**Admin sign-in:** username and password fields; password visibility toggle (with accessible name); primary button "Sign in"; failed attempt shows a banner "Username or password is incorrect", never reveals which one is wrong; button shows in-button progress while submitting.

**Obstacle form (create and edit):**

| Field | Control | Notes |
|---|---|---|
| Building | Read-only chip (building in context) | Not editable after creation |
| Target type | Segmented control: Connection or Node | |
| Target | "Choose on map" button plus a searchable list alternative | Displays a summary of the chosen target (floor, names or coordinates). Required |
| Obstacle type | Dropdown or radio list with the types from PRD FR-30 | Required |
| Description | Multi-line text, 200 character counter | Optional or required per PRD refinement; helper text shows the visitor-facing wording |
| Status | Switch: Active / Resolved | Defaults to Active on create |

Primary action "Save obstacle"; text action "Cancel". Deleting is a separate text action on the edit screen opening a destructive confirmation dialog.

---

## 11. Loading, empty and error states

These patterns implement PRD Section 10. Copy below is the default wording; it may be refined but must stay short, plain and action-oriented.

### 11.1 Patterns

| Pattern | When | Presentation |
|---|---|---|
| Skeleton | List or content areas loading, expected under 3 s | Skeleton rows replacing the list |
| Spinner | Short operations inside a button or small area | Circular indicator in place or inside the button |
| Blocking progress | Route calculation | Centred in the sheet: spinner, "Finding your route…", Cancel |
| Empty view | Valid response with no data | Empty/error structure (8.13) with a helpful next action |
| Error view | Load failure | Same structure, with Retry as the primary action |
| Banner | Partial or non-blocking problem (a warning on a valid route) | Inline banner |

### 11.2 Default copy

| Situation | Title | Message | Actions |
|---|---|---|---|
| Server unreachable (splash, loads) | Can't connect | Check your connection and try again. | Retry |
| No buildings | No buildings yet | There are no buildings available right now. | Retry |
| Floor map unavailable | Map unavailable | We can't show this floor right now. | Retry, Choose another floor |
| No search results | No matches | Nothing matches "{query}". Check the spelling or browse categories. | Clear search |
| Route request failed | Couldn't get a route | Something went wrong. Check your connection and try again. | Retry |
| No route available | No route available | With your current settings, there is no way to reach {destination}. {reason, if known} | Change preference, Choose another destination |
| Route detoured | Route changed | {Area} is closed. This route avoids it and adds about {n} m. | Dismiss (non-blocking) |
| Route affected, no alternative | Part of this route is closed | {Area} is closed and there is no other route. | See details, Change destination |
| Obstacle list empty | No active obstacles | Everything is open. | Add obstacle |
| Obstacle save failed | Couldn't save | Your changes weren't saved. Try again. | Retry |
| Session expired | Signed out | Sign in again to continue. | Sign in |

### 11.3 State rules

- Errors always say what happened in plain words and offer a way forward; they never show codes or stack traces.
- A "no route" outcome is a **designed state, not an error**. It uses the empty view structure with neutral styling (on-surface-secondary icon), not error red.
- "Route changed" is a warning, not an error, and is dismissible but remains available in the route summary.
- While content reloads after Retry, the previous state is replaced by a skeleton, not a blank screen.
- Loading that exceeds about 10 seconds shows an additional line: "This is taking longer than usual."

---

## 12. Accessibility

Target: **WCAG 2.2 level AA** adapted to mobile (PRD NFR-09).

### 12.1 Requirements

| Area | Requirement |
|---|---|
| Contrast | As in Section 3.2. Verified for every text and meaningful graphic pair. |
| Colour independence | Status, blocked connections, selection and floor-route indication always use an icon, label or pattern in addition to colour. |
| Text scaling | Works from 100% to 200% system font size without clipped text or hidden actions; layouts reflow, scroll when needed; sheets allow expansion to show scaled content. |
| Touch targets | 48 dp minimum, 56 dp for map controls, 8 dp spacing between targets. |
| Screen readers | Every interactive element has a meaningful accessible name, role and state. Decorative graphics are hidden from assistive tech. |
| Focus order | Follows visual and logical order: top overlay, map controls, sheet content. Focus moves to the sheet heading when its state changes. |
| Gestures | No path-dependent or multi-finger gestures are required; button alternatives exist for zoom, pan resets and sheet expansion. |
| Motion | Reduce-motion supported (9.4); no flashing content. |
| Orientation | Portrait and landscape both supported. |
| Time limits | None, except session expiry for admin, which announces itself and preserves entered data where possible. |
| Language | Plain language, short sentences, consistent terms. |

### 12.2 The map for non-visual users

A visual map cannot be the only way to complete a task. Therefore:

- **Everything achievable on the map is achievable through lists:** destinations through search and category lists, start through a searchable list, floors through the floor list sheet.
- **The instruction list is the accessible form of the route.** Every route has complete, ordered, text instructions including floor changes (FR-27).
- The route summary is announced on route creation: "Route found, estimated 3 minutes, 145 metres, via elevator to Floor 2."
- Floor switcher buttons are labelled in full: "Floor 2, selected, route on this floor".
- Blocked connections are in a list inside the route warning (what, where), not only drawn.

### 12.3 Live announcements

Announce, without moving focus: route found, route changed (with the reason), no route available, obstacle saved, obstacle deleted, and errors on submit. Loading states announce start and completion once.

### 12.4 Accessibility settings feature

The PRD's device-level "accessible routes" setting (FR-36, FR-37) is described to users in terms of outcome ("Only show step-free routes"), is reachable from Home and from the preference selector, and its effect is stated wherever it influences a result.

### 12.5 Verification

Before a screen is considered done: check at 200% font scale, with a screen reader (TalkBack and VoiceOver where available), with reduce-motion on, in landscape, and with a contrast checker on any new colour pair.

---

## 13. Responsive behaviour

| Aspect | Compact (phone portrait) | Medium | Expanded (tablet) |
|---|---|---|---|
| Map and panel | Full-bleed map, bottom sheet | Map with left side panel (320 dp) | Map with left side panel (360 to 400 dp) |
| Search | Full-screen | Full-screen or inline in panel | Inline in panel |
| Floor switcher | Right edge, vertical | Right edge, vertical | Right edge, vertical |
| Non-map lists and forms | Full width | Centred, 600 dp max | Centred, 600 dp max |
| Dialogs | Standard width | Capped at 560 dp | Capped at 560 dp |
| Obstacle management | Single column list, form on a separate screen | List with form beside it on wide windows | List with form beside it |

Additional rules:

- **Small screens (320 dp wide):** the top overlay keeps search at full remaining width; floating controls reduce to floor switcher plus zoom; no horizontal scrolling anywhere except chip rows (which scroll with visible edge fade).
- **Landscape phones:** use the side-panel layout from 600 dp wide; below that, the sheet keeps collapsed and half snap points only.
- **Keyboard visible:** sheets and forms scroll; the focused field and its primary button remain visible.
- **Large display scaling:** component sizes in dp stay fixed; text follows the system scale; layouts adapt by wrapping, not shrinking.

---

## 14. Page-level layout guidance

Screen numbers match PRD Section 6.

| # | Screen | Layout | Primary action | Notes |
|---|---|---|---|---|
| 1 | Splash | Centred wordmark placeholder, spinner below, background colour | None (auto) | Shows connection error view in place of spinner on failure; maximum two seconds unless loading |
| 2 | Home | Title area (headline) with short descriptor; below: "Continue in {building}" tonal card (if a last building exists), "Choose a building" primary button; accessibility toggle row ("Only show step-free routes"); overflow menu with Admin | Choose a building or Continue | Calm and sparse; no marketing imagery |
| 3 | Building selection | App bar, scrolling list of tiles (name, short description, floor count), skeleton and empty and error states | Tap a tile | Search field only if more than about 8 buildings exist |
| 4 | Floor selection | Floor switcher on map; floor list sheet (modal) with floor names and indicators for route presence | Select floor | List sheet is the accessible alternative |
| 5 | Indoor map | Map shell (6.3) with bottom sheet in Explore state | Search entry at top | Hub screen; no other chrome |
| 6 | Destination search | App bar with search field, category chip row, results list | Tap a result | Returns to map with the destination selected |
| 7 | Route preferences | Modal bottom sheet with title, five radio rows, "Apply" primary button | Apply | Opens from the preference chip |
| 8 | Route details | Sheet in Route preview state: route summary card, floor-change chip, warnings, instruction preview (first three steps) | Start navigation | Map shows full route fit |
| 9 | Navigation instructions | Sheet in Navigating state: current step large, step list below, Previous and Next controls | Next step | Map shows the current floor segment and recentres on the active step; "End navigation" as text action |
| 10 | Obstacle warning and rerouting | Warning banner within the sheet over the route summary, plus map blocked-connection overlay; or no-route full-state view in the sheet | Contextual | Banner persists until dismissed; details open a sheet listing obstacles that affected the route |
| 11 | Admin sign-in | Centred form within 600 dp, app bar labelled Admin | Sign in | Back returns to Home |
| 12 | Admin obstacle management | App bar with building context; filter row (Active, Resolved, All); list of obstacle tiles with status badge; floating "Add obstacle" button | Add obstacle | Tapping a tile opens the edit form; deletion only through the edit form |

### Layout rules for all screens

- A screen has **one clear primary action**.
- Titles describe the content ("Choose a building"), not the feature name.
- Related controls are grouped using spacing before lines or boxes.
- Lists are preferred over grids; there are no dashboard card grids on visitor screens.
- Admin screens may be denser but use the same components and tokens; they are not a separate visual style.

---

## 15. Content and tone

- Voice: direct, calm, neutral. Second person ("Choose a destination").
- Terms (use consistently): **building**, **floor**, **destination**, **start**, **route**, **blocked** or **closed** for obstacles (never "obstacle" in visitor copy), **estimated time**, **step-free** for accessible routing.
- Never use: "You are here", "live", "tracking", "arriving in", or any phrase implying positioning or real-time progress.
- Numbers: distances in metres ("145 m"); time in whole minutes at one minute or more, seconds below ("45 s").
- Error and warning messages: what happened, what it means for the user, what they can do.
- No emoji, no exclamation marks, no humour in status messages.

---

## 16. Governance

- New components need an entry in Section 8; new colours or sizes need a token entry in Sections 3 to 5.
- Pull requests that add UI include screenshots at normal and 200% font scale, and note any accessibility checks done.
- No hard-coded colours, font sizes or spacing values in screens; they come from tokens through the central theme.
- If a screen needs something this document forbids, raise it as a decision rather than working around it.
- When this document and a screen disagree, the document wins until it is deliberately updated.

---

## 17. Assumptions and decisions

### Assumptions

| ID | Assumption |
|---|---|
| DA-1 | "Web application" wording in the brief is read as the mobile app defined in the PRD; there is no web target. |
| DA-2 | The product has no name, logo or brand colours yet; a neutral wordmark and the palette above stand in. |
| DA-3 | Only English is supported; copy is centralised so it can be localised later. |
| DA-4 | Phones in portrait are the primary device; tablet support is functional rather than richly customised. |
| DA-5 | Floor labels, category names and building descriptions come from data and may be of any reasonable length; layouts must tolerate long names (wrap to two lines, then truncate with ellipsis). |

### Decisions requiring approval

| ID | Decision | Options | Recommended default |
|---|---|---|---|
| **DD-1** | Typeface | Atkinson Hyperlegible (bundled); platform default fonts | Atkinson Hyperlegible, for legibility and a less generic look. |
| **DD-2** | Dark theme | Light only in the MVP; light and dark | Light only. Tokens are role-based so dark can follow later. |
| **DD-3** | Brand identity | Neutral placeholder wordmark; commission a name and logo now | Neutral placeholder until the team picks a name. |
| **DD-4** | Map visual style | Schematic flat plan (as defined); realistic or decorative rendering | Schematic flat plan. Faster to author, clearer, and works across environments. |
| **DD-5** | Floor switcher placement | Right-edge vertical rail; top tabs; in the sheet only | Right-edge vertical rail with a floor list sheet as the accessible alternative. |
| **DD-6** | Admin entry point | Overflow menu on Home; separate hidden gesture; visible button | Overflow menu on Home. Discoverable for staff, quiet for visitors. |
| **DD-7** | Navigation steps | Manual Next/Previous; automatic progression | Manual, since the app has no positioning (PRD FR-12). |
| **DD-8** | Primary map layout on phones | Bottom sheet with snap points; separate screens per step | Bottom sheet states, so the route stays visible throughout. |
