# BGOllee Refactoring Plan — Modern UI, Simplified Screens, Multi-Watch Support

Status: **draft for review — no code changed yet**

## 0. Executive summary

Three requested workstreams are entangled through one shared surface: the main
screen. They are sequenced into phases so that shared foundations are built once
and the three workstreams converge on them instead of each rebuilding overlapping
plumbing.

| # | Workstream | Depends on |
|---|---|---|
| 1 | Modernize UI (design tokens, components, "Ollee" look) | nothing — foundation |
| 2 | Reorganize main screen + dialogs (Settings, Provider config, Watch pairing as full-screen views) | needs (1)'s components; needs (3)'s data model for the "Watches" section |
| 3 | Multi-watch support (BLE fan-out to N watches, pairing/renaming) | nothing structurally, but its UI surface is screen (2) |

Because (2) needs both (1) and (3) to be *fully* real, the plan front-loads (1)
and (3) as independent phases that can proceed in parallel (different files,
no shared state), then lands (2) as the integration phase that wires them
together, then closes with per-screen detail passes and cleanup.

## 1. Current state (as of `19c830d`)

- **UI**: 100% imperative Android Views. `MainActivity.kt` builds a single
  `ScrollView → LinearLayout` tree in code (logo `ImageView`, `GlycemiaGraphView`,
  a `TableLayout` of label/value rows, three `Button`s). All dialogs
  (device picker, provider picker, provider config, graph options) are
  `AlertDialog`s built ad hoc. There is no navigation, no screen concept.
- **Compose is already a dependency** (`compose = true`, Material3, BOM,
  tooling) but **completely unused** — `ui/theme/{Color,Theme,Type}.kt` is
  the untouched Android Studio template (purple Material3 palette). This is
  the natural seam to build on.
- **Branding**: `drawable/olleexdrip.xml` (the real app logo) already uses
  `#ff0000` for the wordmark/frame and `#34363c` for text — i.e. the brand is
  already red, not orange. `themes.xml` / `colors.xml` are unrelated legacy
  Material-purple leftovers, not the real brand.
- **Single-watch assumption is baked into every layer**:
  - `SharedPreferences("data")` key `"device_address"` (singular).
  - `BleService`: one `gatt`, one `deviceAddress`, one set of connection
    flags (`isConnecting`, `isConnected`, `servicesReady`, `pendingBg`,
    `lastSent`, `isInErrorState`), one reconnect loop, one notification.
  - `MainActivity.showPairedDevices()` picks exactly one bonded device
    matching `"Ollee"` and overwrites the stored address.
  - `BootReceiver` restarts the service with a single address extra.
- **Provider architecture is already pluggable and mostly UI-agnostic**:
  `GlycemiaProvider` / `ConfigurableGlycemiaProvider` / `GlycemiaProviderManager`
  / `ProviderConfigSpec`/`Field` / `ProviderConfigStore` — this layer needs
  **no structural change**, only a new Compose-based renderer for
  `ProviderConfigSpec` instead of the current dynamically-built `AlertDialog`.
- **History/graph**: `GlycemiaHistoryStore` (JSON file in cache dir) +
  `GlycemiaGraphView` (custom `View`, Canvas-drawn). Values and colors here
  (`#F44336` red / `#4CAF50` green / `#FFC107` amber, `#6B6B6B` label grey)
  are a reasonable starting point for design tokens.
- **i18n**: `values/` (English, most complete), `values-pl/` (partial —
  missing all provider/graph strings), `values-fr/` (colors/themes only, no
  strings.xml overrides found beyond what was listed). New UI text must be
  added to `values/strings.xml` at minimum; full parity across `pl`/`fr` is a
  cleanup-phase task, not a blocker for each feature phase.
- **Tests**: Robolectric-based JVM unit tests exist for
  `GlycemiaHistoryStore`, provider config, `XdripProvider`, synthetic
  generator. No instrumented BLE tests (expected — needs real hardware).

## 2. Reference UI analysis (`project/screens/*.png`)

Observed structure across all four reference screenshots (dashboard, watch
pairing, alarm/settings, graphs):

- **Header** (persistent across screens): logo top-left in a rounded
  orange-outlined box; 2–3 circular icon buttons top-right (mute/off icon
  greyed when disabled, sync/refresh icon, gear icon). On sub-screens the
  logo is replaced by a back-arrow + screen title.
- **Status banner**: full-width colored bar directly under the header —
  green for "Connecting…"/nominal, crimson/magenta for
  "Timed out / error". Always present, changes color+text with state.
- **Section labels**: bold, all-caps, small (e.g. "PAIRED DEVICES",
  "PROFILES", "SNOOZE", "SOUND", "EVENTS"), sometimes with a trailing
  chevron (collapsible) or icon (info, list).
- **Rich two-line selector/list row** (the single most reused pattern —
  needed for provider selector, watch list, and pairing list): rounded
  light-grey pill/card, leading icon, bold primary line, grey secondary line
  underneath, trailing action icon(s) (pencil=edit, trash=delete, or
  gear+switch for provider). Active/selected row gets an orange outline and
  orange text.
- **Big pill buttons**: fully rounded, full-width, color-coded by intent —
  green "+ Pair Ollee Watch" (add/positive), orange filled "Send to watch"
  (primary CTA with icon), dark navy "Save Current Profile" (secondary),
  grey disabled "Load Profile".
- **Toggles**: white track, orange filled thumb with a check glyph.
  **Dropdown-ish rows**: light-grey card, label + value line, trailing
  vertical-3-dot menu. **Range slider**: dotted orange track, two orange
  circular thumbs, numeric labels at both ends.
- **Segmented tabs**: text tabs with an orange underline under the active
  one (e.g. "Alarm | Daily Alarm").
- **Graph cards**: light-grey rounded card per metric, big numeric value +
  unit top-left, colored small icon top-right, sparkline with dots,
  min/max axis labels, bottom time-axis labels; a top mini-bar-chart card
  and a "YESTERDAY ‹ ›" date header with calendar icon.
- **Bottom icon nav row** (watch-app chrome, not part of this phone app —
  ignored for our purposes).

### Design tokens to extract

| Token | Value (proposed) | Source |
|---|---|---|
| `color.accentPrimary` (single token, not user-facing) | **red**, seeded from logo `#FF0000` → practical UI shade `#E5342A` | user requirement + `olleexdrip.xml` |
| `color.accentPrimaryAlt` (reserved token, unused today) | Ollee orange `#F5821F` — kept only as a documented alternative for a future brand-color change, no UI to select it | reference screenshots |
| `color.statusPositive` | `#43A047` (green banner) | dashboard banner |
| `color.statusNegative` | `#C2255C` (crimson banner) | settings/graphs banner |
| `color.statusWarning` | `#FFC107` | existing `GlycemiaGraphView` amber |
| `color.surfaceCard` | `#F1F1F3` light grey pill/card bg | pairing + settings screenshots |
| `color.textPrimary` / `color.textSecondary` | `#1A1A1A` / `#6B6B6B` | matches existing graph label paint |
| `shape.card` / `shape.pill` | 20dp corner / fully-rounded | screenshots |
| `spacing` scale | 4/8/12/16/24/32 dp | derived |
| `typography.sectionLabel` | bold, caps, ~13sp, letter-spacing | screenshots |

Accent color is **not** a user-facing setting — it is a single design-token
constant (`OlleeColors.accentPrimary` or equivalent) shipped as **red**,
directly reusing the logo's `#FF0000` rather than the ecosystem's stock
orange. The requirement is code-level swappability: every component must
reference the token, never a hardcoded hex, so changing the brand color
later is a one-line edit with no component changes required.

## 3. Target architecture

- **UI framework**: migrate `MainActivity` to Jetpack Compose (`setContent`),
  since the dependency is already present and unused. Single-activity,
  Navigation-Compose with routes: `Main`, `Settings`, `ProviderConfig`,
  `WatchPairing`, `EditWatchName` (dialog).
- **State management**: replace the three `BroadcastReceiver`s + direct
  `SharedPreferences` reads in `MainActivity` with a singleton in-process
  state holder (`AppState`, exposing `StateFlow`s) that `BleService` and the
  providers publish to, and Compose screens `collectAsState()` from. This is
  a same-process app (service + activity), so a `StateFlow` singleton is
  simpler and richer than broadcasts (it can carry a `List<WatchStatus>`
  natively, which broadcast extras cannot without custom Parcelables).
  Broadcasts (`BG_UPDATED`, `GLYCEMIA_HISTORY_UPDATED`, `BLE_STATUS`) can be
  kept firing in parallel during migration for safety, then removed once the
  new state flow is proven, to keep every phase independently shippable.
- **Multi-watch data model** (new):
  ```kotlin
  data class PairedWatch(
      val address: String,        // BT MAC — stable key
      val name: String,           // user-assigned or default "Ollee Watch #N"
      val isCustomName: Boolean
  )
  data class WatchStatus(
      val watch: PairedWatch,
      val state: WatchConnState   // SYNCED / OFFLINE / ERROR / CONNECTING
  )
  ```
  Persisted via a new `WatchStore` (mirrors `ProviderConfigStore`'s style:
  a small JSON blob in `SharedPreferences`, since `GlycemiaHistoryStore`
  already establishes the JSON-in-a-file pattern for structured data — a
  `SharedPreferences` string holding a JSON array is enough here given the
  small N). One-time migration: if legacy `device_address` exists and the
  new watch list is empty, migrate it into a single `PairedWatch` entry
  named `"Ollee Watch #1"`.
- **BLE service rearchitecture**: `BleService` moves from one `gatt`/one set
  of flags to a `Map<String, WatchConnection>` (one `WatchConnection` per
  paired address), each owning its own `BluetoothGatt`, connection flags,
  timeout/error state, and `lastSent` dedup — extracted into its own class
  so per-watch logic isn't duplicated inline. On every glucose reading, the
  service iterates all `WatchConnection`s and tries to send to each
  independently and non-blockingly; a watch that's out of range simply stays
  `OFFLINE` without affecting delivery to the others. Connections are
  attempted with a small stagger/sequential backoff rather than all at once,
  since Android BLE stacks commonly cap concurrent GATT links (~4–7
  depending on OEM) and hammering all of them simultaneously on every
  service restart is a known source of stack lockups.
- **Watch discovery**: keep the current, low-risk approach — enumerate
  `BluetoothAdapter.bondedDevices` filtered by name containing `"Ollee"`
  (already implemented in `MainActivity.showPairedDevices()`), extended to
  multi-select-friendly semantics: devices already in our `WatchStore` are
  labeled "Paired" (with the user's assigned name if renamed), others are
  labeled "New" (tap to add). This assumes watches are OS-bonded first (as
  today) — matches current behavior and needs no new permission surface.
  *Optional future enhancement, not in this plan's core scope*: active BLE
  advertisement scanning to find not-yet-bonded watches.

## 4. Phased plan

Phases are labeled **[F]oundation / independent** vs **[I]ntegration**.
F-phases can be worked in parallel by different people; I-phases require the
F-phases they depend on to have landed.

### Phase 0 — Foundation: design tokens & component library **[F]**
No dependency on the other phases. Pure additive Compose scaffolding.

- `ui/theme/Color.kt`, `Type.kt`, `Shape.kt` (new), `Spacing.kt` (new),
  `Theme.kt` — replace purple Material template with the Ollee token set
  from §2. Accent color is a plain constant in `Color.kt`
  (`val AccentPrimary = Color(0xFFE5342A)`), not read from preferences or
  exposed in any UI — the only requirement is that no component hardcodes a
  hex value outside this file, so the brand color can be changed later by
  editing one constant.
- `ui/components/`: `OlleeHeader`, `StatusBanner`, `SectionLabel`,
  `RichSelectorRow` (the reusable two-line row — the single highest-value
  component, consumed by Provider selector, Watch rows, and the pairing
  list), `PillButton` (primary/secondary/success/disabled variants),
  `HeaderIconButton`, `ToggleRow`, `FoldableSection` (chevron + animated
  expand, used to wrap the graph), `FullScreenScaffold` (title + back
  button, used by Settings/ProviderConfig/WatchPairing).
- `ui/nav/`: route definitions + `AppNavHost` shell (empty placeholder
  screens are fine at this stage).
- Decide and document the graph strategy: **wrap the existing
  `GlycemiaGraphView` via `AndroidView` interop** inside the new
  `FoldableSection`/card styling rather than rewriting the Canvas drawing
  logic — this reuses already-correct, tested drawing code and confines the
  redesign to the surrounding chrome (card background, fold behavior,
  header labels). A native-Compose rewrite of the graph itself is a
  reasonable stretch goal but is out of scope for this refactor to limit
  risk.
- Update `AndroidManifest.xml` theme + `themes.xml`/`colors.xml` to align
  with the new tokens (or mark old ones deprecated pending Phase 5 cleanup).

**Exit criteria**: a components showcase (temporary debug screen or
`@Preview`s) rendering every new component with the red accent theme, with
no behavior wired up yet.

### Phase 1 — Foundation: multi-watch BLE backend **[F]**
No dependency on Phase 0; can proceed fully in parallel. Touches
`BleService.kt`, `BootReceiver.kt`, adds `WatchStore.kt`, `PairedWatch.kt`,
`WatchConnection.kt`, `WatchStatus.kt`, `AppState.kt` (or extends whatever
state-holder Phase 0/2 settles on — see integration note below).

- Add `WatchStore` (persistence + one-time migration from legacy
  `device_address`).
- Extract per-connection logic into `WatchConnection` (gatt lifecycle,
  timeout watcher, `formatBg`/CRC/send — the packet-building code in
  `BleService` is already device-agnostic and can move as-is).
- Rework `BleService` to hold `Map<String, WatchConnection>`, react to
  `WatchStore` changes (watch added/removed/renamed) by
  starting/stopping the corresponding `WatchConnection`, and fan out every
  `GlycemiaReading` to all active connections.
- Rework the foreground notification to summarize multi-watch status (e.g.
  "2/3 watches synced") instead of a single connected/reconnecting string.
- Update `BootReceiver` to start the service whenever the watch list is
  non-empty (instead of checking a single address).
- Publish per-watch status (`SYNCED`/`OFFLINE`/`ERROR`/`CONNECTING`) to a
  `StateFlow<List<WatchStatus>>` for the UI to consume later.
- Unit tests (Robolectric, matching existing style): `WatchStore` migration
  and CRUD, `WatchConnection` state-machine transitions (mockable
  BluetoothGatt callback), fan-out logic (N connections, some
  connected/some not, reading reaches only connected ones).

**Exit criteria**: service manages N watches correctly with unit tests
green; no UI wired to it yet (can be validated via logs/tests, and/or a
temporary debug trigger).

### Phase 2 — Integration: Main screen rebuild **[I]**
Depends on Phase 0 (components) and Phase 1 (watch state/data). The
Provider-selector half of this screen has no dependency on Phase 1 and can
start as soon as Phase 0 lands; the Watches section needs Phase 1.

- `MainScreen.kt` composable assembled from Phase 0 components:
  `OlleeHeader` (logo + gear icon whose state comes from a permission
  check — green gear vs. red-Bluetooth-with-exclamation, per spec §2.1) +
  `StatusBanner` + **Glycemia sources** section (`RichSelectorRow` for the
  active provider showing name + live glucose/error line, gear icon → route
  to `ProviderConfig`, switch icon → provider-type popup, `FoldableSection`
  wrapping the existing graph) + **Watches** section (list of
  `RichSelectorRow`s driven by Phase 1's `StateFlow<List<WatchStatus>>`,
  each showing user/default name + status + grey HW address, with edit/
  delete trailing icons, plus a green "+ Pair Ollee Watch" pill navigating
  to `WatchPairing`).
- Replace `MainActivity`'s manual view tree with `setContent { AppTheme {
  AppNavHost(...) } }`; retire the `TableLayout`/`Button` code once the
  Compose screen has parity.
- Wire first-run/no-permission auto-navigation to `Settings` (spec §2.1).

**Exit criteria**: main screen visually matches the reference layout with
live data, provider switching works, and 1–N watches render with correct
per-watch status.

### Phase 3 — Integration: Settings screen **[I]**
Depends on Phase 0 only (permission logic already exists in
`MainActivity.arePermissionsGranted`/`requestPermissions`, just needs a new
home).

- `SettingsScreen.kt` via `FullScreenScaffold`: **Permissions** section —
  one row per permission group with a colored frame (granted/not) and a
  request button; **About** section — app version (`PackageInfo`/
  `BuildConfig.VERSION_NAME`) only, per spec §2.1. No appearance/color
  settings — accent color stays a design-time constant (§3).
- Header gear-icon logic (green vs. red-Bluetooth+exclamation) lives here
  conceptually but is rendered in `MainScreen`'s header — shared permission
  state should come from one place (e.g. a small `PermissionState` state
  holder) to avoid duplicating the permission-list logic that already
  exists in `MainActivity`.

### Phase 4 — Integration: Provider config screen **[I]**
Depends on Phase 0 only. No changes needed to `GlycemiaProvider`/
`ConfigurableGlycemiaProvider`/`ProviderConfigSpec`/`ProviderConfigStore` —
this phase only replaces the rendering layer.

- `ProviderConfigScreen.kt`: full-screen renderer for `ProviderConfigSpec`
  (choice fields → dropdown/segmented component, text/int/long fields →
  labeled text field component), reusing Phase 0's form components instead
  of the current dynamically-built `AlertDialog`.
- Provider-switch popup (tapping the "switch" icon on the selector row) as
  a lightweight list picker, opening `ProviderConfigScreen` immediately
  after switching to a configurable provider (matches current behavior in
  `showProviderPicker`).

### Phase 5 — Integration: Watch pairing screen & row actions **[I]**
Depends on Phase 0 (components) and Phase 1 (`WatchStore`/`WatchConnection`
lifecycle) and lands after Phase 2 has the Watches section shell in place.

- `WatchPairingScreen.kt`: full-screen list of bonded `"Ollee"` devices,
  each row showing name (assigned name if already paired, else raw BT name)
  + secondary line (`"Paired"`/`"New"` + grey HW address); tapping a "New"
  row adds it to `WatchStore` (default name `"Ollee Watch #N"`, N = next
  unused index) and returns to Main.
- Edit-name flow: small dialog/inline field on the Watches row's pencil
  icon, persisting the custom name keyed by BT address in `WatchStore`.
- Delete flow: trash icon → confirm → remove from `WatchStore` → `BleService`
  tears down that `WatchConnection` (already covered by Phase 1's
  store-driven start/stop).

### Phase 6 — Cleanup & polish **[I]**
- Delete dead code: legacy `AlertDialog`-based dialogs in `MainActivity`,
  unused purple `themes.xml`/`colors.xml` entries, any broadcast plumbing
  made redundant by the `StateFlow` state holder (only after confirming
  nothing else still depends on the broadcasts).
- i18n: add all new UI strings to `values/strings.xml` and bring
  `values-pl/strings.xml` and `values-fr/strings.xml` back to parity
  (currently missing most provider/graph strings).
- Manual QA pass with real hardware (BLE behavior is not unit-testable):
  multi-watch fan-out with 0/1/N watches in range, permission-revoked
  recovery, provider switching mid-connection, rename/delete while
  connected, boot-start with N watches.
- Update `README`/in-repo docs if any reference the old single-device flow.

## 5. Dependency graph

```
Phase 0 (tokens+components) ──┐
                               ├─► Phase 2 (Main screen) ──► Phase 5 (Pairing screen)
Phase 1 (multi-watch BLE)  ────┘         │
                               │         └─► Phase 3 (Settings)   [Phase 0 only]
                               └─────────────► Phase 4 (Provider config) [Phase 0 only]

Phase 6 (cleanup) after all of the above.
```

Phases 0 and 1 have no dependency on each other and are the two workstreams
best suited to parallel work. Phases 3 and 4 only need Phase 0 and can be
built alongside Phase 2 rather than strictly after it.

## 6. Risks & open questions

- **BLE concurrent-connection limits**: multi-watch fan-out must stagger
  connection attempts and tolerate partial failure gracefully — this is the
  highest-risk piece and warrants real-device testing early (start Phase 1
  hardware validation before Phase 5 UI is ready, using a debug trigger).
- **Graph component**: keeping the Canvas `View` via `AndroidView` interop
  vs. a full Compose rewrite is a scope/risk tradeoff; this plan recommends
  interop to avoid re-deriving the existing min/max/grid math.
- **Watch discovery scope**: bonded-devices-only (current behavior) vs.
  active BLE scan for unbonded devices — this plan scopes to the former;
  confirm that's acceptable before Phase 5.
- **i18n coverage**: `values-pl`/`values-fr` are already behind current
  `main`; this refactor will widen that gap further until Phase 6's cleanup
  pass, which should be flagged to whoever owns translations.

## 7. Testing strategy

- Unit tests (Robolectric, existing convention in `app/src/test`):
  `WatchStore` (migration, CRUD), `WatchConnection` state machine,
  `BleService` fan-out logic, `PermissionState` helper.
- Compose: `@Preview`s per component (Phase 0) for fast visual iteration
  without a device; consider `androidTest` Compose UI tests for
  navigation/critical flows (main → settings → back, add/rename/delete
  watch) once screens stabilize — lower priority than the BLE-side unit
  tests given the app's size.
- No automated coverage is possible for real BLE hardware behavior
  (multi-radio range/timeout scenarios) — Phase 6 calls for a manual QA
  checklist instead.
