# Lean — audit 2026-10-22

## LEAN-1 · suggestion · `onValueChanged` wraps the lambda in an object whose only extra line is a self-assignment

- **Where:** `library/src/main/java/ru/pravbeseda/currencyedittext/CurrencyEditText.kt:59`, `:292-296`, `:378-391`, `:397-406`
- **What this is:** `onValueChanged(action)` stores the host's callback inside an anonymous `OnValueChanged` object that runs `that.state = state` before calling `action`.
- **Problem:** The only caller (`:295`) passes `state`, the view's own field `validate()` set one line earlier, so `that.state = state` assigns the field to itself. The wrapper object, `val that = this` and the assignment can go; the private field can hold the `(BigDecimal, State, String) -> Unit` lambda directly (net −11 lines, ABI unchanged — checked on a clone). The public `OnValueChanged` interface is then used by nothing in the library; removing it is an ABI break (`library.api:48`), worth it only at the next major.
- **Fix direction:** Make the private field the function type and assign `action` to it; keep the interface until a major release.
- **If we skip it:** Ten lines of indirection stay, and a reader is led to think the callback changes `state`.
- **Effort:** S · **Touches:** `CurrencyEditText.kt` · **Depends on:** — · **Status:** new · **Verified:** confirmed

## LEAN-2 · suggestion · Test dependencies and a unit-test option that nothing uses

- **Where:** `library/build.gradle:32` (`unitTests.includeAndroidResources = true`), `:69`, `:70` (`testImplementation` of `androidx.test.core.ktx` and `ext.junit.ktx`), `:75`, `:77` (`androidTestImplementation` of `ext.junit.ktx` and `rules`); `gradle/libs.versions.toml:42-43`, `:51-52`
- **What this is:** Test-classpath declarations and an AGP option merging Android resources into JVM unit tests.
- **Problem:** `src/test` imports no `androidx.test`, no `R.` and no Robolectric; `src/androidTest` uses only `launchActivity` (core-ktx, still needed at `:74`) and `InstrumentationRegistry`, with no `@RunWith`, `AndroidJUnit4` or rules. With the five lines and four catalog entries removed, `build` and `:library:compileDebugAndroidTestKotlin` stay green (checked on a clone).
- **Fix direction:** Delete the five build-script lines and the four catalog lines.
- **If we skip it:** Slower unit-test configuration and dependencies Renovate keeps bumping for nothing.
- **Effort:** S · **Touches:** `library/build.gradle`, `gradle/libs.versions.toml` · **Depends on:** — · **Status:** new · **Verified:** confirmed (instrumented tests compiled, not executed)

## LEAN-3 · suggestion · A redundant clause in `buildNumber` duplicates the branch below it

- **Where:** `library/src/main/java/ru/pravbeseda/currencyedittext/watchers/CurrencyTextFormatter.kt:158-161`, `:167-170`
- **What this is:** Both `if` blocks turn an empty integer part into `"0"` and move the cursor forward by one.
- **Problem:** A non-empty `fractionalPart` implies a decimal separator and `maxNumberOfDecimalPlaces ≥ 1`, so the early return at `:163` is skipped and the block at `:167` does the same thing; the `|| fractionalPart.isNotEmpty()` half of the first condition never changes the result. The `newPartOfText == "0"` half is still needed.
- **Fix direction:** Delete `|| fractionalPart.isNotEmpty()`; the unit suite passes without it (checked on a clone).
- **If we skip it:** The step everyone edits keeps two branches doing one job.
- **Effort:** S · **Touches:** `watchers/CurrencyTextFormatter.kt` · **Depends on:** COR-1 · **Status:** new · **Verified:** confirmed
