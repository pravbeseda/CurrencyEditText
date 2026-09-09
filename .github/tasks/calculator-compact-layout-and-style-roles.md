# Compact calculator layout and two new style roles

## Goal
The panel is 352dp tall: an expression line of 48dp, six rows of keys at 48dp and 16dp of padding.
Dropping the two parenthesis keys frees the cells that let `=` move into the bottom row, so the
panel loses a whole row and stands at 304dp with the key width unchanged at 76dp. In the same pass
the expression line's fill and the `=` key's fill and text colour become style roles, so a host can
lift the panel's main key out of the flat borderless grid.

Target layout, 4 columns and 5 rows:

```
C   ⌫   ±   ÷
7   8   9   ×
4   5   6   −
1   2   3   +
0   0   ,   =
```

## Decisions
- Parentheses go rather than the panel keeping six rows: `ExpressionEvaluator` already binds `×`
  and `÷` tighter than `+` and `−`, so `2 + 3 × 4` is right without them, and a money field rarely
  needs a grouped expression. The alternatives — five or six columns keeping the parens — cost key
  width (61dp and 51dp against today's 76dp) for the same or a little more height.
- The paren handling in `CalculatorState` goes with the keys, because no press can reach it any
  more: `appendOpeningParenthesis`, `appendClosingParenthesis`, the `lastChar == ')'` guards in
  `appendDigit`, `appendDecimal` and `flipSign`, and the `settled.last() == '('` branch of
  `appendOperator`. `OPERAND_MAY_FOLLOW` then equals `OPERATORS` and the two constants merge.
- `ExpressionEvaluator` keeps its parenthesis grammar and its tests. It is not a file this task
  changes, the rule is one line of the primary production, and its tests are the only place where
  operator precedence inside a group is checked.
- Three new colour roles, no geometry and no drawable references: `calculatorExpressionBackgroundColor`,
  `calculatorEqualsBackgroundColor`, `calculatorEqualsTextColor`. A corner radius or a text size
  would have to travel through code into a drawable, and a host-supplied drawable would take the
  panel's readability out of the library's hands. Both are additions we can still make later.
- `calculatorEqualsTextColor` is the one role with no default of its own: where a host does not name
  it, `CalculatorColors` falls back to the resolved `operatorText`, so a host that recolours the
  operators gets `=` for free and only a host that fills the key has to name a text colour.
- The two background defaults are `@android:color/transparent`, so the panel looks exactly as it
  does today after the update. A default fill would have to be `?attr/colorPrimary` with
  `?android:attr/textColorPrimaryInverse` on it, and the library cannot check that pairing against
  a host's own primary colour.
- Both fills are applied as a `GradientDrawable` carrying the role's `ColorStateList`, and the
  borderless button's ripple moves to the `=` key's foreground, which would otherwise be replaced
  along with its background. `View.setForeground` is API 23 and minSdk here is 24.
- A new attribute goes in four places, as in the #46 plan: `res-public/values/attrs.xml`,
  `res-public/values/public.xml`, `res/values/styles.xml` and `CalculatorColors`.

## Steps
- [x] 1. `CalculatorKey` loses `OPEN_PAREN` and `CLOSE_PAREN`, and
  `CalculatorState` loses the handling that only they could reach — files:
  `library/src/main/java/.../calculator/CalculatorKey.kt`,
  `library/src/main/java/.../calculator/CalculatorState.kt`,
  `library/src/test/.../calculator/CalculatorKeyTest.kt`,
  `library/src/test/.../calculator/CalculatorStateTest.kt` — done when: a new test
  `` `the panel has no parenthesis keys` `` fails against the current enum, then passes; the
  parenthesis tests of `CalculatorStateTest` are deleted with the behaviour they covered; the rest
  of the suite is green.
- [x] 2. The layout becomes 4×5 with `=` in the bottom row and `0` spanning two columns — files:
  `library/src/main/res/layout/currency_calculator_panel.xml`,
  `library/src/main/java/.../calculator/CalculatorPopup.kt`,
  `library/src/androidTest/.../CalculatorPanelLayoutTest.kt` (new) — done when: the instrumented
  test inflates the panel and asserts 20 key cells, four columns, no parenthesis ids and a `0` of
  `columnSpan` 2; it fails on the current layout first.
- [x] 3. The three new roles — files: `library/src/main/res-public/values/attrs.xml`,
  `.../public.xml`, `library/src/main/res/values/styles.xml`,
  `library/src/main/java/.../calculator/CalculatorColors.kt`,
  `library/src/main/java/.../calculator/CalculatorPopup.kt`,
  `library/src/androidTest/.../CalculatorPanelThemeTest.kt` — done when: the theme test asserts both
  backgrounds default to transparent, `equalsText` defaults to the resolved operator colour
  including when a host overrides only `calculatorOperatorTextColor`, and a host style naming all
  three wins; the panel paints the `=` fill and keeps its ripple.
- [x] 4. Documentation follows the code — files: `README.md` — done when: the calculator section no
  longer promises parentheses, the colour table carries the three new rows, and the panel's height
  claim, if any, matches.

- [x] 5. The Material layout anchors the panel to the field inside it, not to itself — files:
  `library/src/main/java/.../CurrencyMaterialEditText.kt`,
  `library/src/main/java/.../calculator/CalculatorPopup.kt`,
  `library/src/androidTest/.../TestActivity.kt`,
  `library/src/androidTest/.../CalculatorPanelAnchorTest.kt` (new) — done when: the instrumented
  test clicks the end icon and asserts the panel's anchor is the inner field; it fails against the
  layout first.

## Rulings
- Step 3, on the fills: both are applied with `setBackgroundColor` and the role's `defaultColor`
  rather than a `GradientDrawable` carrying the whole `ColorStateList`, as the plan said. Neither
  surface has states worth painting — the expression line is a `TextView` and the equals key is
  never disabled — and the equals key keeps its press feedback by moving the borderless ripple to
  its foreground, which it needs either way. Cost if that is wrong: a host naming a state list for
  either fill gets its default colour.
- Step 3, on `CalculatorColors.of`: split into `of`, which obtains and recycles the two
  `TypedArray`s, and a private `read`, which builds the value. `of` was 65 lines against detekt's
  `LongMethod` bound of 60 once the three roles were added.
- Step 3 dropped the module under Kover's 80% floor, to 78.1%: the parenthesis handling that left
  `CalculatorState` was covered, and the new roles are not reachable from `src/test`. The exclusion
  list already names `CalculatorColors` with the reason that it reads the host's theme through a
  `Context` and is covered by the instrumented `CalculatorPanelThemeTest`, but the pattern was a
  bare class name and every line of the class lives in its companion object. The pattern becomes
  `CalculatorColors*`, which is what its own comment always claimed; coverage reads 88.9%.
  `CalculatorPopupKt`, 19 uncovered lines inside the measurement for the same reason, is left alone
  and filed as its own issue (#49).
- Step 5, added after the four above: a `PopupWindow` hangs from the bottom of its anchor, and
  `TextInputLayout` keeps a strip below its box for the error text — 21dp of it on a 420dpi phone,
  measured with `uiautomator dump` — so the panel started that far below the field. Anchoring it to
  the inner field closes the gap and costs the error text while the panel is up, which the KDoc on
  `setCalculatorEnabled` used to promise. `CalculatorPopup.anchor` drops `private` so the test can
  name what the layout passed; the class is `internal`, so nothing leaves the artifact.

## Notes for the pull request
- Behaviour change: the panel no longer offers parentheses.
- Behaviour change: on `CurrencyMaterialEditText` the panel hangs from the field rather than from
  the layout, and covers the error text while it is open.
- Public API change: three new XML attributes; no Kotlin ABI change, so `library.api` is untouched.
