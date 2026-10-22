# Correctness — audit 2026-10-22

## COR-1 · blocking · A currency symbol containing the decimal separator turns typed 12 into 0.12

- **Where:** `library/src/main/java/ru/pravbeseda/currencyedittext/watchers/CurrencyTextFormatter.kt:126-138` (`keepLastDecimalSeparator`), `:145` (`buildNumber` digit filter over the whole text, prefix included); compare `:181`, which already corrects for a *grouping* separator inside the symbol
- **What this is:** The formatter steps run over the full field text, currency prefix included.
- **Problem:** With `currencySymbol="Rs."` (en-IN, decimal `.`) or `"Fr."` (de-CH) the prefix is `"Rs. "`. Typing `1` gives `Rs. 0.1`, typing `12` gives `Rs. 0.12` (JVM probe): `buildNumber`'s regex keeps the prefix's `.` as the first character, so it is taken as the decimal separator and `getValue()` returns 0.12.
- **Fix direction:** Strip the prefix once, right after `restoreCurrencyPrefix` guarantees it, and run the number steps on the remainder only — which also removes the `separatorsInCurrencySymbol` correction.
- **If we skip it:** Common abbreviated currency symbols produce values 100× too small.
- **Effort:** M · **Touches:** `watchers/CurrencyTextFormatter.kt`, `CurrencyInputWatcherTest.kt` · **Depends on:** — · **Status:** new · **Verified:** confirmed

## COR-2 · blocking · With zero padding on, deleting the decimal separator multiplies the value unless the field has exactly 2 places

- **Where:** `library/src/main/java/ru/pravbeseda/currencyedittext/watchers/CurrencyTextFormatter.kt:78-84`
- **What this is:** A `normalizeInput` rule dropping the padding zeros when the user deletes the decimal separator.
- **Problem:** The rule hard-codes `"00"`. With `decimalZerosPadding=true`: 3 places — deleting the separator in `1.000` gives `1,000` (1 becomes 1000, since `"1.000"` does not end in `".00"` as the guard expects); 2 places — `1.00` gives `1`; 1 place — `1.0` gives `10`.
- **Fix direction:** Build the suffix from `config.maxNumberOfDecimalPlaces` instead of the literal.
- **If we skip it:** On padded fields with 1 or 3+ places one backspace silently scales the amount.
- **Effort:** S · **Touches:** `watchers/CurrencyTextFormatter.kt`, `CurrencyInputWatcherTest.kt` · **Depends on:** — · **Status:** new · **Verified:** confirmed

## COR-3 · blocking · `emptyStringForZero` ignores a zero with a scale, and the calculator always produces one

- **Where:** `library/src/main/java/ru/pravbeseda/currencyedittext/CurrencyEditText.kt:101`; producer `calculator/ExpressionEvaluator.kt:45` via `CalculatorPopup.kt:236-240` and `CurrencyEditText.kt:239`; promise at `README.md:148`
- **What this is:** `setValue` writes an empty string for zero when `emptyStringForZero` (default `true`) is on.
- **Problem:** `value == BigDecimal.ZERO` uses `equals`, which compares scale. The calculator returns `setScale(maxDecimalPlaces)`, so `5-5=` on the default 2-place field yields `0.00`, the check fails and the field shows `0` instead of empty; `setValue(BigDecimal("0.00"))` from a host does the same. Only `maxNumberOfDecimalPlaces = 0` escapes. `CalculatorState.seededWith` already uses `signum()`.
- **Fix direction:** Test `value.signum() == 0`, with a test for the scaled zero. Required by SEC-1's exact parse in any case.
- **If we skip it:** The calculator and any host passing a scaled zero contradict the documented default.
- **Effort:** S · **Touches:** `CurrencyEditText.kt`, a test · **Depends on:** — · **Status:** new · **Verified:** confirmed

## COR-4 · blocking · An ill-formed locale tag silently falls back to the device locale

- **Where:** `library/src/main/java/ru/pravbeseda/currencyedittext/util/Utils.kt:68-73`; callers `CurrencyEditText.kt:92,128`, `CurrencyMaterialEditText.kt:75`
- **What this is:** `getLocaleFromTag` parses `app:localeTag` and the public `setLocale(String)`.
- **Problem:** `setLocale("en_US")` or `app:localeTag="da_DK"` (underscore, not BCP 47) throws `IllformedLocaleException`, which is caught and replaced with `Locale.getDefault()` (probe: `"en_US"` → the JVM default). The separators then follow the device and the developer is never told. The catch has no comment, so the AGENTS.md rule "No silently swallowed exceptions" is broken today.
- **Fix direction:** Let the exception propagate (delete the try/catch), or keep the fallback and state why in the required comment.
- **If we skip it:** A typo in a locale tag produces wrong separators only on some devices.
- **Effort:** S · **Touches:** `util/Utils.kt` · **Depends on:** — · **Status:** new · **Verified:** confirmed
