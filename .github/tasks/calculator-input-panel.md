# Optional calculator panel for CurrencyEditText and CurrencyMaterialEditText

An opt-in calculator, opened from a button at the right edge of the field, that evaluates an
arithmetic expression and writes the result back into the field. One key toggles the sign of the
current operand.

## Survey of ready-made options (2026-09-07)

Nothing off the shelf fits, so the panel and the evaluator are written here:

- [CalculatorInputView](https://github.com/Gperez88/CalculatorInputView) — Java, marked *retired*,
  last change 2018.
- [number-keyboard](https://github.com/davidmigloz/number-keyboard) 5.0.2 — alive, Apache-2.0, but
  Compose-only since 4.0.0; the View version is frozen at v3. `:library` has no Compose dependency
  and should not gain one.
- [Android-CustomKeyboard](https://github.com/DonBrody/Android-CustomKeyboard) — unmaintained.
- [mXparser](https://mathparser.org/) 6.1.1 — alive, but dual-licensed: commercial apps must buy a
  licence. Unacceptable for an artifact published to Maven Central.
- [exp4j](https://github.com/fasseg/exp4j) — Apache-2.0, ~50 KB, no dependencies, but evaluates in
  `double` and is not in active development. This project handles money in `BigDecimal`.

## Decisions

1. Own View-based panel, own expression evaluator on `BigDecimal`. No new dependency.
2. The panel is a `PopupWindow` anchored under the field. The system keyboard is not replaced —
   `showSoftInputOnFocus` stays untouched, so existing input behaviour is unchanged.
3. Full expression with operator precedence and parentheses. Keys: `0`–`9`, decimal separator,
   `+ − × ÷`, `( )`, `±`, `⌫`, `C`, `=`. No percent, no memory, no history.
4. Division uses `MathContext(16, HALF_UP)` inside the expression; the value written back on `=`
   is rounded to the field's `maxNumberOfDecimalPlaces`, `HALF_UP`.
5. `±` flips the sign of the operand currently being entered, and never writes a double sign: a
   binary `+` or `-` in front of that operand is flipped instead, so `100-50` becomes `100+50`.
   Only `*` and `/`, which cannot absorb a sign, get a unary minus written after them. When
   `negativeValueAllow` is false the key is disabled (visible but greyed), so the panel layout
   does not shift between fields.
6. Opt-in through a new XML attribute `calculatorEnabled`, default `false`, plus
   `setCalculatorEnabled` / `isCalculatorEnabled` on both views. Ships in the existing artifact.

## Assumptions (routine calls, change if wrong)

- The panel opens seeded with the field's current value as the first operand.
- `=` writes through the view's existing `setValue(BigDecimal)`, so grouping, currency prefix and
  padding all come from `CurrencyInputWatcher` as usual, then closes the popup.
- Division by zero, an incomplete expression, or a negative result in a field with
  `negativeValueAllow = false`: the panel shows an error state and writes nothing. No exception
  escapes to the host app.
- The panel renders digits and the decimal key using the field's own resolved separators.
- The popup closes on outside tap and on back press, leaving the field unchanged.
- Panel layout and drawables stay private resources (`res/`, not `res-public/`).

## Non-goals

- Replacing the system keyboard (that could be added later as a second display mode without
  breaking this API).
- Scientific functions, percent, memory keys, expression history.
- Exposing the evaluator or a `MathContext` as public API.

## Architecture

New package `ru.pravbeseda.currencyedittext.calculator`, all `internal`, all reachable from
`src/test`:

- `CalculatorKey` — enum of the keys above.
- `ExpressionEvaluator` — recursive descent over `BigDecimal`, taking a canonical ASCII string.
  Output: a sealed `EvalResult` (`Success(BigDecimal)` / `Failure`). Recursive descent rather than
  shunting-yard: precedence, parentheses and the unary minus all fall out of the grammar, so it is
  shorter than a tokeniser plus an RPN pass and has no intermediate representation to get wrong.
  A single `Failure` carries no reason — nothing consumes one, and a per-reason message would need
  user-facing strings to localise.
- `CalculatorState` — immutable state machine: `press(key): CalculatorState`, holding the canonical
  expression and answering `result(scale)`. Holds no view reference and knows nothing about locale.
- `CalculatorPopup` — the only class that touches Android: inflates the layout, wires clicks to
  `CalculatorState`, anchors the `PopupWindow`.

The views stay thin: they own the button and hand `CalculatorPopup` a value in and a value out.
All arithmetic, formatting of the expression line, and key semantics live in the two testable
classes.

`library/build.gradle` — add `ru.pravbeseda.currencyedittext.calculator.*` to the Kover include
filter next to `watchers.*` and `util.*`, so the new code counts towards `minBound(80)`.

## PR 1 — evaluator and state machine, no UI

Branch: `feat/calculator-core`.

1. Write `ExpressionEvaluatorTest` first and watch it fail: precedence (`100 + 2 × 50 = 200`),
   parentheses, unary minus, `100 ÷ 3 × 3 = 100.00` at scale 2, division by zero, unbalanced
   parentheses, trailing operator.
   Verify: red run recorded, then `./gradlew :library:testDebugUnitTest` green (the plain
   `:library:test` task takes no `--tests` filter).
2. Implement `CalculatorKey`, `EvalResult`, `ExpressionEvaluator`.
3. Write `CalculatorStateTest` first: key sequences, `±` on the current operand, `±` twice,
   `⌫` across an operator, `C`, `=` on an incomplete expression, seeding from an initial value.
   Verify: red run recorded, then green.
4. Implement `CalculatorState`.
5. Add the Kover filter entry.
   Verify: `./gradlew :library:koverVerifyDebug :library:koverLogDebug` — the new package is listed
   and the bound holds.

Done when `./gradlew build` passes locally and `library/api/library.api` is unchanged (everything
here is `internal`).

## PR 2 — the panel, the button and the attribute

Branch: `feat/calculator-panel`.

1. Add `calculatorEnabled` to both `declare-styleable` blocks in `res-public/values/attrs.xml`.
2. Add the panel layout and the button drawable under `library/src/main/res/`.
   Verify: `library/src/main/res-public/values/public.xml` still lists nothing, so the new
   resources stay private to the artifact.
3. Implement `CalculatorPopup`.
4. `CurrencyEditText`: read the attribute in `init`, add `setCalculatorEnabled` /
   `isCalculatorEnabled`, draw the button as a compound drawable and handle the tap. The
   attribute changes no watcher configuration, so it must **not** call `invalidateTextWatcher()`.
5. `CurrencyMaterialEditText`: read the attribute in `init`, delegate the setter and getter, and
   render the button through `TextInputLayout`'s `END_ICON_CUSTOM` rather than a compound drawable.
6. Instrumented tests in both `CurrencyEditTextTest` and `CurrencyMaterialEditTextTest`: the
   default value of `calculatorEnabled` is `false`, and enabling it shows the button.
   Verify: `./gradlew :library:connectedAndroidTest` on an API 24 emulator.
7. `./gradlew :library:updateKotlinAbi` and commit the diff.
   Verify: the diff contains only the four new public members, nothing else.
8. Update `CLAUDE.md` (Architecture: the new package and the fact that `calculatorEnabled` is the
   one attribute that does not go through `invalidateTextWatcher`) and the README.

Done when `./gradlew build` passes locally, the instrumented tests pass, and the ABI diff is
reviewed.

**Public API change** (for the PR description): `calculatorEnabled` XML attribute on both views;
`setCalculatorEnabled(Boolean)` and `isCalculatorEnabled(): Boolean` on both views. Additive only —
no existing signature changes.

## Risks

- Popup placement: not enough room below the field means flipping above it. Check on a short
  screen in landscape before merging PR 2.

`res-public/values/public.xml` was suspected of marking every resource in the AAR private. It does
not: an empty `<resources>` element makes AGP emit no `public.txt` at all, and the published AAR
contains none, so every resource stays public. The file is inert, not harmful.
