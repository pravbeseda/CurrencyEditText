# Calculator panel: placement after the keyboard closes, and theming

## Goal

The calculator panel opens clipped to a few key rows and paints itself in the platform's default
purple instead of the host app's Material theme. Both are library bugs, reproduced on a Material3
dark-themed app (see the issue screenshot): fix the placement race and make the panel take its
colors from the app's theme.

## The two defects

1. **Stale measurement.** `CalculatorPopup.showAnchored` (`CalculatorPopup.kt:104-118`) reads
   `getWindowVisibleDisplayFrame()` while the soft keyboard is still open, so `spaceBelow` is small,
   `CalculatorPlacement.choose` flips the panel above the field and caps its height to `spaceAbove`.
   The panel is then shown with `PopupWindow(..., focusable = true)`, which takes focus and closes
   the IME — the screen frees up, and the panel keeps hanging at the height measured for a screen
   that no longer exists. Inside the `ScrollView` that shows as ~2.5 visible rows.
2. **Framework theme attributes.** Every key uses `style="?android:attr/borderlessButtonStyle"`
   (`currency_calculator_panel.xml`, 21 occurrences) and the popup background uses
   `?android:attr/colorBackground` (`currency_calculator_panel_background.xml:20`). Framework
   attributes do not see a Material3 app theme, so the panel falls back to the platform defaults —
   the purple labels in the screenshot. `library/build.gradle:65` already declares
   `api libs.material`, so Material attributes (`?attr/colorSurface`, `?attr/colorOnSurface`,
   `?attr/colorPrimary`, `?attr/materialButtonOutlinedStyle` / `borderlessButtonStyle`) are
   available without a new dependency.

## Decisions

- How to fix the stale measurement → close the IME before showing the panel and place it only once
  the window frame has settled (hide the soft input, then a one-shot layout listener with a timeout
  fallback for the case where no keyboard was open), because the panel then appears once, already in
  the right place. Re-placing the panel on every frame change while it is open is the more robust
  option and was rejected as overkill for a short-lived popup that visibly jumps on open.
- How far the theming fix goes → Material theme attributes only, no new public API: `?attr/colorSurface`
  for the background, `?attr/colorOnSurface` for the key labels, `?attr/colorPrimary` for the
  operators and `=`, plus the repeated key block extracted into a private `res/values/styles.xml`.
  Public attributes or a public style overlay would freeze a resource contract nobody has asked for
  yet, and either can still be added later without breaking this.
- Only two steps, not the usual three to seven: the run fixes two independent defects in the same
  component, and neither splits into a smaller slice with its own observable criterion.
- Test home → `src/androidTest`. Neither defect is reachable from `src/test`: one is resource
  resolution against the host theme, the other is IME timing in a `PopupWindow`. An emulator is
  attached, so the instrumented suite runs locally in this session.

## Steps

- [ ] 1. Theme the panel from the app's Material theme — files: `library/src/main/res/layout/currency_calculator_panel.xml`,
  `library/src/main/res/drawable/currency_calculator_panel_background.xml`, new
  `library/src/main/res/values/styles.xml` — lenses: none — done when: a new instrumented test
  inflates the panel under a Material3 theme and asserts a key's text color is the theme's
  `colorOnSurface` (red before the change), and `:library:connectedAndroidTest` is green.
- [ ] 2. Place the panel after the keyboard has closed — files:
  `library/src/main/java/ru/pravbeseda/currencyedittext/calculator/CalculatorPopup.kt` — lenses: none
  — done when: `:library:test` and `:library:connectedAndroidTest` stay green, and in the sample app
  on the emulator, with the keyboard open on the field, the panel opens below the field at its full
  height (screenshot recorded in the PR). No unit test for the glue itself: it is IME timing inside
  a `PopupWindow` and cannot be reached from `src/test`.

## Rulings

## Parked
