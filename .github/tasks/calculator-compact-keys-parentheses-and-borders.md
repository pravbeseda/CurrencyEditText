# Calculator: square keys hugging the button, a `()` key, and borders between keys

Three changes to the calculator panel, in one branch.

## 1. The panel stops stretching to the field's width

Today the panel is as wide as the field (`CalculatorPopup.panelWidth()`), and every key is
`0dp` wide with `layout_columnWeight="1"` and `wrap_content` high — wide, low keys whose size
follows the field.

- Keys become a square of a fixed size, default **56dp**, read from a new style attribute
  `calculatorKeySize` so a host can set its own.
- The popup's width becomes `WRAP_CONTENT`, so the panel is exactly four keys plus its padding.
- The panel hugs the calculator button: `showAsDropDown(..., Gravity.END)`, which aligns the
  panel's end edge with the end edge of the view it is told to hug and handles RTL itself.
- On `CurrencyMaterialEditText` the button is the layout's end icon, which sits to the right of
  the inner field. The panel therefore keeps hanging from the field (vertical placement, #50) but
  hugs a second view: `CalculatorPopup` gains an `alignTo` view, the plain field passes itself,
  the Material layout passes the layout. `showCalculator(anchor)` becomes `showCalculator(alignTo)`
  — the old parameter was always the field and carried nothing.

## 2. A `()` key in the bottom-left corner

`ExpressionEvaluator` never lost its parenthesis grammar; #50 dropped the two keys and the
`CalculatorState` handling only they could reach. Both come back behind one key.

- `CalculatorKey.PARENTHESIS`, drawn as `()`. Its label is not its symbol, so `label()` gains a
  small map for that, next to the one that prints `*` as `×`.
- `CalculatorState.appendParenthesis()` closes where a closing bracket is due — an unclosed `(`
  and a finished operand, the old `appendClosingParenthesis` guard — and otherwise opens where an
  operand may start, the old `appendOpeningParenthesis` guard. A press that can do neither leaves
  the expression alone.
- The guards #50 removed with the keys come back: no digit or decimal point glued to `)`, no sign
  flip after `)`, `(` counted as a place an operand and a unary minus may follow.
- The bottom row becomes `()` `0` `.` `=` and the `0` key loses its double width, so all twenty
  keys are the same size.

## 3. A border between the keys

- The key grid becomes five horizontal `LinearLayout` rows inside a vertical one, replacing the
  `GridLayout`. `showDividers="middle"` then draws a line between keys and between rows, and none
  around the outside — which is what a border *between* the keys means. `GridLayout` has no
  dividers, and faking them with margins doubles the line between two cells and rings the grid.
- The divider is 1dp and takes its colour from a new style attribute `calculatorKeyBorderColor`,
  defaulting to transparent: a host that names nothing sees the panel it has today, only 1dp
  further apart.

## Style plumbing

`CalculatorColors` now carries a dimension as well as colours, so it is renamed
`CalculatorStyle`; the fields keep their names and the layered read — the library's defaults as
one pass, the host's style laid over it role by role — is unchanged. Kover's exclusion and the
instrumented theme test follow the name.

Both new attributes are declared in `attrs.xml`, listed in `public.xml`, given a default in
`Widget.CurrencyEditText.Calculator`, and documented in the README's table of calculator roles.

## Steps

1. Unit tests for the `()` key and the restored guards → verify: red for the right reason.
2. `CalculatorKey` and `CalculatorState` → verify: `:library:test` green.
3. `CalculatorStyle` rename, the two new attributes, their defaults and public entries →
   verify: `:library:test` green, `:library:lintDebug` green.
4. The layout rewrite, the key size, the dividers and `Gravity.END` placement →
   verify: instrumented layout and theme tests rewritten and green on an emulator.
5. README → verify: `./gradlew build` green.

## Out of scope

- The border's width is fixed at 1dp; only its colour is open to the host.
- An expression left with an unclosed `(` still fails on `=` rather than closing itself, exactly
  as it did before #50.
