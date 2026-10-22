# Architecture — audit 2026-10-22

## ARCH-1 · blocking · Whether the formatting watcher is attached depends on the field's focus history

- **Where:** `library/src/main/java/ru/pravbeseda/currencyedittext/CurrencyEditText.kt:349-363` (detaches on blur at :360, re-attaches on focus at :357), `:276-280` (`invalidateTextWatcher` re-attaches regardless of focus), `:94-95` (init attaches), `:100-112` (`setValue` relies on the watcher), `:239` and `CurrencyMaterialEditText.kt:171` (calculator accept → `setValue`)
- **What this is:** `CurrencyInputWatcher` adds the currency prefix, zero padding and grouping, and fires `onValueChanged`, which runs the validator. `onFocusChanged(false)` removes the watcher so it can clear a bare `"$ "`; only a focus gain or a config setter puts it back.
- **Problem:** A field with `currencySymbol="$"` and `decimalZerosPadding=true`, never focused: `setValue(BigDecimal("1234.5"))` shows `"$ 1,234.50"` and fires the listener. Once focused and blurred, the same call shows `"1,234.5"` — no prefix, no padding, no `onValueChanged`, no validator, so the host misses the value and `CurrencyMaterialEditText`'s error is not updated. With `groupingSeparator = emptyChar` it shows a literal `"1n234.5"`. The calculator reaches this on an everyday path: type into field A, move to field B, tap A's calculator button (the tap does not focus A — `onTouchEvent` consumes ACTION_UP before `super`; the Material end icon is a separate view) and press `=`. The blur detach dates from the first commit (`51fddfd`); no test covers a write after blur.
- **Fix direction:** Keep the watcher attached for the view's whole life and mute it only around the one `setText("")` on blur (`EasyTextWatcher`'s ignore flag, or remove/add around that call), so attachment has one owner.
- **If we skip it:** Programmatic and calculator writes are formatted and validated or not depending on where the user tapped earlier.
- **Effort:** S · **Touches:** `CurrencyEditText.kt`, possibly `watchers/EasyTextWatcher.kt`, an instrumented test · **Depends on:** — · **Status:** new · **Verified:** confirmed (the calculator path rests on framework focus behaviour not run on a device; the `setValue` path proves it alone)

## ARCH-2 · blocking · Carrying the value across a config change is copied three times and skipped by four setters — `setCurrencySymbol` makes `getValue()` return 0

- **Where:** `library/src/main/java/ru/pravbeseda/currencyedittext/CurrencyEditText.kt:117-123`, `:125-131`, `:145-156` (three copies of getValue / `setText("")` / invalidate / setValue), `:162-165`, `:169-172`, `:183-193`, `:198-201` (setNegativeValueAllow, setDecimalZerosPadding, setCurrencySymbol, setMaxNumberOfDecimalPlaces — invalidate only), `:176-179` (a fourth variant), `CurrencyMaterialEditText.kt:74-76` (calls `setLocale(getLocaleFromTag(tag))` though the `String` overload exists), `util/Utils.kt:48-52` (the catch turning the failure into 0)
- **What this is:** AGENTS.md routes every config change through `invalidateTextWatcher()`, which rebuilds the watcher but leaves the text as it is. `setLocale` (both overloads, identical but for `getLocaleFromTag`) and `applySeparators` re-apply the value with their own copies of the same four lines; the other setters do not.
- **Problem:** On a field showing `"$ 100"`, `setCurrencySymbol("€")` (a currency picker) leaves the text `"$ 100"`, and `getValue()` returns `0`: the prefix `"€ "` is not found, `NumberFormat.parse("$ 100")` throws, and the catch returns 0 — whose own comment says it only sees text holding "no number at all". `setNegativeValueAllow(false)` on `"-5"` leaves −5 readable and `setMaxNumberOfDecimalPlaces(0)` on `"1.55"` leaves 1.55, until the next keystroke. No instrumented test changes a setter on a filled field.
- **Fix direction:** One private reconfigure step owns the sequence — read the value under the old config, apply the change, rebuild the watcher, write the value back — and every setter uses it; this deletes the three copies, and `setLocale(String)` becomes a one-line delegation to `setLocale(Locale)`.
- **If we skip it:** A currency switch on a filled form saves 0, other setters leave values the field's own config forbids, and every new setter decides afresh.
- **Effort:** M · **Touches:** `CurrencyEditText.kt`, `CurrencyMaterialEditText.kt`, instrumented tests in `CurrencyEditTextTest.kt` · **Depends on:** — · **Status:** new · **Verified:** confirmed

## ARCH-3 · blocking · `CurrencyMaterialEditText` formats `app:text` before applying the attributes that govern it

- **Where:** `library/src/main/java/ru/pravbeseda/currencyedittext/CurrencyMaterialEditText.kt:85` (setText) against `:86-91` (separators, negative values, decimal places, padding, emptyStringForZero applied afterwards); compare `CurrencyEditText.kt:81-96`, which applies every attribute first and then reformats
- **What this is:** "Configure, then format the initial text" is decided separately in each view's `init`, and the two have drifted.
- **Problem:** The inner field still has its defaults when the text is formatted, and the later setters do not reformat it (ARCH-2). `app:text="-5"` + `app:negativeValueAllow="true"` shows `5`; `app:text="1.234"` + `app:maxNumberOfDecimalPlaces="3"` shows `1.23`; `app:text="1234,5"` + `app:decimalSeparator=","` shows `12345` on a device whose decimal separator is `.`. `setEmptyStringForZero` at `:91` also rewrites the text through `setValue`, so `emptyStringForZero="false"` with no text shows `0` where the plain view stays empty. The plain view gets all four right; no instrumented test inflates `app:text`.
- **Fix direction:** Apply the XML text last, after every attribute, as `CurrencyEditText` does, and stop `:91` from rewriting the text at init.
- **If we skip it:** The same XML gives different values on the two views, depending on the device locale.
- **Effort:** S · **Touches:** `CurrencyMaterialEditText.kt`, `CurrencyMaterialEditTextTest.kt` · **Depends on:** ARCH-2 · **Status:** new · **Verified:** confirmed

## ARCH-4 · blocking · A validator's `null` means "valid" in one view and "keep the old error" in the other

- **Where:** `library/src/main/java/ru/pravbeseda/currencyedittext/CurrencyEditText.kt:304-313` (`null` and `""` both mean valid), `library/src/main/java/ru/pravbeseda/currencyedittext/CurrencyMaterialEditText.kt:195-203` (`null` leaves `TextInputLayout.error` untouched)
- **What this is:** A validator is `(BigDecimal) -> String?`. `CurrencyEditText.validate` turns its result into `state`/`textError`; `CurrencyMaterialEditText` wraps the validator so it also sets the layout's `error`.
- **Problem:** `setValidator { if (it > BigDecimal(10)) "too big" else null }` on a `CurrencyMaterialEditText`: typing 11 shows "too big"; going back to 5 makes the inner state OK (`isValid == true`) but the error stays, because the wrapper skips `null`. `setValidator(null)` after an error leaves it up. The README sample returns `""`, which clears, so the README hides the trap the public `String?` type invites.
- **Fix direction:** Decide the meaning once: the wrapper always assigns `error = result?.ifEmpty { null }`, which removes the null branch.
- **If we skip it:** The Material field shows a stale error while reporting itself valid.
- **Effort:** S · **Touches:** `CurrencyMaterialEditText.kt`, an instrumented test · **Depends on:** — · **Status:** new · **Verified:** confirmed

## ARCH-5 · blocking · The `emptyChar` sentinel is read three ways; `Routines.bigDecimalToString` writes NUL characters and is not "the same formatting" as the view

- **Where:** `library/src/main/java/ru/pravbeseda/currencyedittext/util/Routines.kt:34` (default-locale `DecimalFormatSymbols()`), `:38-39` (`emptyChar` → `'\u0000'`), `:45` (`RoundingMode.FLOOR`); `watchers/CurrencyTextFormatter.kt:230-232` (`emptyChar` → no separator); `util/Utils.kt:56-65` (passes `'n'` through as a literal separator); `library/src/test/java/ru/pravbeseda/currencyedittext/RoutinesTest.kt:146` (pins the NUL output); `README.md:332`, `README.md:348`
- **What this is:** `emptyChar = 'n'` is the public sentinel for "no thousands separator"; the public `Routines.bigDecimalToString` is introduced in the README as "the same formatting … without a view". Each of three formatting paths interprets the sentinel on its own.
- **Problem:** `bigDecimalToString(BigDecimal("1234567.5"), CurrencyFormatConfig(groupingSeparator = emptyChar, decimalSeparator = ','))` returns `"1\u0000234\u0000567,50"` — 12 characters with embedded NULs — although README:348 promises "without a thousands separator", and `RoutinesTest.kt:146` asserts the NUL output. `formatMoneyValue` writes a literal `n` whenever the watcher does not reformat (ARCH-1). Also, the minus sign comes from the device locale's symbols: under `sv` the JVM gives `−1 234,50` with U+2212 while the field shows ASCII `-` (plausible on Android; see Not checked).
- **Fix direction:** Translate the sentinel once, in the formatter SEC-2 introduces, with `DecimalFormat.isGroupingUsed = false`; build the symbols from `Locale.ROOT`; drop the `'\u0000'` substitution and flip the test. The output change is a public-API behaviour change to call out in the PR. Which rounding mode the helper uses (FLOOR today; typing truncates, `setValue` HALF_EVEN, the calculator HALF_UP) is the owner's decision.
- **If we skip it:** Consumers following the README get invisible NUL characters in labels, logs and CSV.
- **Effort:** S · **Touches:** `util/Routines.kt`, `util/Utils.kt`, `RoutinesTest.kt`, `README.md` · **Depends on:** SEC-2 · **Status:** new · **Verified:** confirmed (NUL output); the U+2212 part plausible

## ARCH-6 · blocking · `public.xml` leaves `app:text` and `app:selectAllOnFocus` private, while the README documents them

- **Where:** `library/src/main/res-public/values/public.xml:4-27` (no `text`, no `selectAllOnFocus`), `library/src/main/res-public/values/attrs.xml:57-58`, `README.md:96`, `README.md:355-359`
- **What this is:** Since d236ec5 `public.xml` names the library's public resources, and everything else becomes private to the AAR; its comment says it covers "the XML attributes".
- **Problem:** d236ec5 listed sixteen attributes and left out `CurrencyMaterialEditText`'s two own ones; the packaged `public.txt` lists 22 attributes and neither of these. The README tells users to write `app:text` and `app:selectAllOnFocus`. In a consuming app (the in-repo `:sample` is a project dependency and does not see it) Lint should report `PrivateResource` on them, failing a consumer that runs with `warningsAsErrors`.
- **Fix direction:** Add the two `<public type="attr">` lines.
- **If we skip it:** The documented way to give a Material field its initial text is flagged as private API in every consumer.
- **Effort:** S · **Touches:** `res-public/values/public.xml` · **Depends on:** — · **Status:** new · **Verified:** plausible — needs a consumer app built against the packaged AAR with Lint run on a layout using `app:text`

## ARCH-7 · blocking · The detekt baseline keeps five entries for `!!` calls that no longer exist

- **Where:** `library/detekt-baseline-debug.xml:9-13`; the rule in `AGENTS.md` ("Baselines only ever shrink: an entry goes when its finding is fixed")
- **What this is:** The frozen list of tolerated detekt findings, matched by signature ID.
- **Problem:** The five `UnsafeCallOnNullableType` entries (`CurrencyEditText$localeTag!!`, `CurrencyMaterialEditText$decimalSeparator!!`, `$groupingSeparator!!`, `$localeTag!!`, `$text!!`) match no code: 9296d34 removed the calls and left the baseline alone, breaking the shrink-only rule. They also hide a reintroduction: a probe `localeTag!!` added to `CurrencyEditText` passes `detektDebug` with today's baseline and fails without these lines.
- **Fix direction:** Delete the five lines; `detektAll` and `build` stay green (checked on a clone).
- **If we skip it:** The stated rule stays broken, and those `!!` patterns can come back into the published Views unreported.
- **Effort:** S · **Touches:** `library/detekt-baseline-debug.xml` · **Depends on:** — · **Status:** new · **Verified:** confirmed

## ARCH-8 · suggestion · `String?.firstChar()` reimplements the stdlib's `firstOrNull()`

- **Where:** `library/src/main/java/ru/pravbeseda/currencyedittext/util/Routines.kt:57`; `library/src/main/java/ru/pravbeseda/currencyedittext/CurrencyViewAttributes.kt:20` (import), `:60`, `:62`
- **What this is:** An internal helper in the public `Routines` file, used only to read the two separator attributes.
- **Problem:** `getString(...)?.firstOrNull()` does exactly the same for null, empty and non-empty strings; nothing else references the helper, and it is not in `library.api`.
- **Fix direction:** Use `?.firstOrNull()` at both call sites; delete the extension and its import.
- **If we skip it:** One redundant helper stays next to public API.
- **Effort:** S · **Touches:** `util/Routines.kt`, `CurrencyViewAttributes.kt` · **Depends on:** ARCH-5 · **Status:** new · **Verified:** confirmed
