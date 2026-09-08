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
- How far the theming fix goes → **reversed after the step-1 gate.** The first ruling was "Material
  theme attributes only, no new public API". Measured on the emulator, that ruling crashes: under
  `Theme.AppCompat.Light.NoActionBar` — the theme the `:sample` app itself uses — `?attr/colorSurface`
  and `?attr/colorOnSurface` do not resolve and inflating the panel throws
  `InflateException: Error inflating class android.widget.TextView`. The panel therefore gets a
  public style contract instead: a theme attribute `currencyCalculatorStyle` pointing at a style
  built from five colour roles (`calculatorPanelBackgroundColor`, `calculatorExpressionTextColor`,
  `calculatorErrorTextColor`, `calculatorKeyTextColor`, `calculatorOperatorTextColor`), with defaults
  that resolve in any theme (`?android:attr/colorBackground`, `?android:attr/textColorPrimary`, an
  AppCompat `?attr/colorPrimary` state list, and the panel's existing error red as a colour
  resource). A host overrides any subset of the five, per field, through the same
  `android:theme` overlay it already puts on the field.
- Where the digit/operator split lives → on `CalculatorKey` in Kotlin rather than in two layout
  styles, because the colours are now resolved in code; that also makes the split unit-testable.

- Only two steps, not the usual three to seven: the run fixes two independent defects in the same
  component, and neither splits into a smaller slice with its own observable criterion.
- Test home → `src/androidTest`. Neither defect is reachable from `src/test`: one is resource
  resolution against the host theme, the other is IME timing in a `PopupWindow`. An emulator is
  attached, so the instrumented suite runs locally in this session.

## Steps

- [x] 1. Theme the panel from the app's Material theme — files: `library/src/main/res/layout/currency_calculator_panel.xml`,
  `library/src/main/res/drawable/currency_calculator_panel_background.xml`, new
  `library/src/main/res/values/styles.xml` — lenses: none — done when: a new instrumented test
  inflates the panel under a Material3 theme and asserts a key's text color is the theme's
  `colorOnSurface` (red before the change), and `:library:connectedAndroidTest` is green.
- [x] 2. Place the panel after the keyboard has closed — files:
  `library/src/main/java/ru/pravbeseda/currencyedittext/calculator/CalculatorPopup.kt` — lenses: none
  — done when: `:library:test` and `:library:connectedAndroidTest` stay green, and in the sample app
  on the emulator, with the keyboard open on the field, the panel opens below the field at its full
  height (screenshot recorded in the PR). No unit test for the glue itself: it is IME timing inside
  a `PopupWindow` and cannot be reached from `src/test`. Reworked after the step-1 gate: the panel
  now reads five colour roles from a public style contract, and the digit/operator split moved to
  `CalculatorKey.isOperator`, which is unit-tested.

## Rulings

- Quality gate, blocking: `?attr/colorSurface` / `?attr/colorOnSurface` are declared only by
  Material, and the `:sample` app runs on `Theme.AppCompat.Light.NoActionBar`. Confirmed on the
  emulator — inflating the panel under that theme throws `InflateException`. Escalated; the theming
  decision was reversed into the style contract recorded under Decisions.
- Spec gate, blocking: the done-criterion named in step 1 ("a key's text colour is the theme's
  `colorOnSurface`, red before the change") was already green before that change, because
  `Theme.Material3` maps `android:textColorPrimary` onto `colorOnSurface`. Accepted. The reworked
  step carries criteria that do fail first: `CalculatorKeyRoleTest` (compile-red without
  `isOperator`) and `thePanelInflatesUnderAnAppCompatTheme` (the crash above).
- Spec gate, blocking: step 1 removed `?android:attr/borderlessButtonStyle` instead of replacing it,
  so the keys silently inherited the theme's raised button style. Fixed: `CurrencyCalculatorKey`
  now has `@android:style/Widget.Material.Button.Borderless` as its parent.
- Quality gate, suggestion: the disabled item in `currency_calculator_key_on_surface.xml` could
  never be selected. Fixed by deleting the file — `?android:attr/textColorPrimary` already carries
  a disabled alpha, and only the operator role, whose sign key is disabled, still needs a state list.
- Kover: `CalculatorColors` reads the host theme through a `Context`, so `src/test` cannot reach it
  and its lines dropped the module below the 80% floor. Moved to its own file and added to the same
  exclusion list as `CalculatorPopup`, for the same stated reason; the bound itself is untouched and
  coverage now reads 81.15%. The class is covered by `CalculatorPanelThemeTest` instead.
- `library/src/main/res-public/values/public.xml` was an empty `<resources/>` and produced no
  `public.txt` in the AAR, so every library resource was public. It now names the sixteen XML
  attributes, which makes the panel's layout, drawables, colours and styles private to the artifact
  — what the calculator plan said it wanted.

## Parked
