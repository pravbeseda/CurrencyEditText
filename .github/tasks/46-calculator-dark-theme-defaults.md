# Calculator panel defaults under a dark host theme (#46)

## Goal
The calculator panel's two remaining fixed defaults break under a dark host theme: the error line
is a hard-coded `#B00020` at 2.54:1 on a dark panel, and the panel has no edge, so it vanishes on a
background of its own colour. Both defaults become theme-derived, so a host that names no style
still gets a readable panel.

## Decisions
- How to give the panel a visible boundary → a new public colour role `calculatorPanelStrokeColor`
  defaulting to `?android:attr/colorControlHighlight`, drawn as a 1dp stroke, because the issue asks
  for a role and a host with its own `calculatorPanelBackgroundColor` must be able to recolour or
  remove the edge; a strokeless-but-fixed variant only defers the same public attribute to the next
  release.
- The stroke is set on a `GradientDrawable` in code rather than in the shape XML, because
  `setTintList` on the drawable paints the fill and the stroke with one colour.
- `@color/currency_calculator_error` is deleted rather than kept: `res-public/values/public.xml`
  makes every resource but the attributes private to the artifact, so nothing outside can name it.
- The error colour stays `?attr/colorError`, the plain fix the issue prescribes, even though it
  costs contrast on a light AppCompat theme (see the ruling below), because no fixed colour can
  clear 4.5:1 against both a light and a dark panel and the role is there for a host that disagrees.
- A new attribute goes in four places here, not the three the README's view attributes take:
  `res-public/values/attrs.xml`, `res-public/values/public.xml`, `res/values/styles.xml` and
  `CalculatorColors`.

## Steps
- [x] 1. The error line follows the theme: `calculatorErrorTextColor` defaults to `?attr/colorError`
  (an AppCompat attribute, present in `Base.V7.Theme.AppCompat`), and `@color/currency_calculator_error`
  goes — files: `library/src/main/res/values/styles.xml`, `library/src/main/res/values/colors.xml`,
  `library/src/androidTest/.../CalculatorPanelThemeTest.kt` — lenses: compatibility — done when:
  `CalculatorPanelThemeTest` asserts the error colour equals the host theme's `colorError` under
  AppCompat light, Material3 dark and AppCompat dark, and goes green on a device.
- [x] 2. The panel gets an edge: new role `calculatorPanelStrokeColor` (default
  `?android:attr/colorControlHighlight`), a 1dp stroke applied with the fill in `CalculatorPopup` —
  files: `library/src/main/res-public/values/attrs.xml`, `.../public.xml`,
  `library/src/main/res/values/styles.xml`, `.../calculator/CalculatorColors.kt`,
  `.../calculator/CalculatorPopup.kt`, `library/src/androidTest/.../CalculatorPanelThemeTest.kt` —
  lenses: compatibility — done when: the theme test asserts the new role's default and a host
  override of it, and the panel drawable carries a non-zero stroke of that colour.
- [x] 3. The README's calculator style table names both changed defaults and the new role — files:
  `README.md` — lenses: none — done when: the table has a `calculatorPanelStrokeColor` row and the
  error row no longer says `#B00020`.

## Rulings
- The compatibility lens called step 1 `blocking`: under `Theme.AppCompat.Light`, `?attr/colorError`
  is `#FF5722`, which gives 3.03:1 on the default panel background `#FAFAFA` where the old fixed
  `#B00020` gave 7.02:1 — a light-theme regression on the very metric the issue cites, and the
  sample app is such a host (`sample/src/main/res/values/styles.xml:4`). Escalated; the decision was
  to keep `?attr/colorError`. Cost if that is wrong: a consumer on a bare AppCompat light theme sees
  a less readable error line than in 2.0.2 until it names `calculatorErrorTextColor` itself. The
  dark side, which is what the issue is about, improves from 1.80:1 to 4.81:1 (AppCompat dark) and
  10.89:1 (Material3 dark), and Material light hosts are unchanged.

- Step 2, spec reviewer, `suggestion`: the drawable construction moved out of `CalculatorPopup` into
  `CalculatorColors.panelDrawable(context)`, which the step did not name. Kept: it is what lets the
  theme test reach the drawable without standing up a popup, and a top-level function in
  `CalculatorPopup.kt` was measured by Kover (`CalculatorPopupKt` is not in the exclude list) and
  dropped the module under its 80% floor. Cost if wrong: one more member on a class that is
  otherwise a plain value holder.
- Step 2, spec reviewer, `suggestion`: `checkNotNull(... as? GradientDrawable)` turns a path that
  used to pass `null` to `setBackgroundDrawable` into a throw. Kept: CLAUDE.md forbids silently
  swallowed failures, the resource ships inside the artifact, and the message names the cause.
- Step 2, spec reviewer, `suggestion`: the comment in
  `library/src/main/res/drawable/currency_calculator_panel_background.xml` was edited although the
  step's file list did not name it. Kept: the diff made the old sentence ("the panel tints this
  drawable") false, and leaving it would have been a contradiction between code and documentation.
- Step 2, compatibility lens, `blocking`: the README's role table still lists five roles and gives a
  consumer no way to find the opt-out (`calculatorPanelStrokeColor` = `@android:color/transparent`).
  Dropped as already planned: step 3 of this plan is exactly that edit.

## Parked
- The Kover exclude list in `library/build.gradle:106-108` names the classes
  `…calculator.CalculatorPopup` and `…calculator.CalculatorColors`, but the report still measures
  `CalculatorColors$Companion` (0/36) and `CalculatorPopupKt` (0/21) — both unreachable from
  `src/test` for the very reason the two named classes are excluded. `CalculatorColors.of()` lives
  in that companion, so the code the exclude comment says the instrumented test covers instead
  counts against the 80% floor today; the module verifies at 80.52%. To see it: `./gradlew
  :library:koverXmlReportDebug` and grep the two class names in
  `library/build/reports/kover/reportDebug.xml`.
