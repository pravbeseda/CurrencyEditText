# Duplication — audit 2026-10-22

## DUP-1 · suggestion · The calculator states "operand character" and "operator key" in three places

- **Where:** `library/src/main/java/ru/pravbeseda/currencyedittext/calculator/CalculatorKey.kt:52-53` (`isOperator = !symbol.isDigit() && symbol != '.'`); `library/src/main/java/ru/pravbeseda/currencyedittext/calculator/CalculatorState.kt:34` (`isOperandChar`), `:20-26` (`OPERATOR_KEYS`) next to `:32` (`OPERATORS = "+-*/"`) and `:62`; `library/src/main/java/ru/pravbeseda/currencyedittext/calculator/ExpressionEvaluator.kt:141`
- **What this is:** The key enum, the state machine and the parser each decide which characters belong to a number and which keys are binary operators.
- **Problem:** `isOperator` is exactly `!symbol.isOperandChar()`, the parser writes the same predicate inline a third time, and `OPERATOR_KEYS` lists the four keys whose symbols are `OPERATORS`. They agree today; applying this removes the 7-line set and one inline predicate (the `when (key)` at `:62` needs a guard in its `else` branch).
- **Fix direction:** One internal `Char.isOperandChar()` next to `CalculatorKey`, used in all three places; replace `OPERATOR_KEYS` with a symbol check against `OPERATORS`. `CalculatorKeyRoleTest.kt:41` pins `isOperator`.
- **If we skip it:** A new digit-like character has to be added in three places, and missing one makes key colour, state machine and parser disagree.
- **Effort:** S · **Touches:** `calculator/CalculatorKey.kt`, `calculator/CalculatorState.kt`, `calculator/ExpressionEvaluator.kt` · **Depends on:** — · **Status:** new · **Verified:** confirmed

## DUP-2 · suggestion · SDK levels and the Java/JVM target are written into both modules although the root already configures both

- **Where:** `library/build.gradle:10,13-14,18-19,38`; `sample/build.gradle:8,11-12,21-22,30`; the shared hook at `build.gradle:77` (`plugins.withId('com.android.base')`); `AGENTS.md:36`
- **What this is:** `compileSdk 37`, `minSdkVersion 24`, `targetSdkVersion 37`, Java 1.8 `compileOptions` and `jvmTarget = JVM_1_8` appear identically in both module scripts.
- **Problem:** The root script already configures both Android modules from one hook for lint; these lines can move there too (about 20 lines out, 14 in), and each SDK or toolchain bump stops being two edits.
- **Fix direction:** Move them into the existing root hook, plus a `plugins.withId('org.jetbrains.kotlin.android')` block for `jvmTarget`; update AGENTS.md's "set in both modules" sentence.
- **If we skip it:** Each SDK or toolchain bump is two edits nothing forces to stay equal.
- **Effort:** S · **Touches:** `build.gradle`, `library/build.gradle`, `sample/build.gradle`, `AGENTS.md` · **Depends on:** — · **Status:** new · **Verified:** confirmed
