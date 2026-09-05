# CLAUDE.md

Guidance for Claude Code when working in this repository.

## What this is

`ru.pravbeseda:CurrencyEditText` — an Android library (published to Maven Central) providing two
views that format numeric/monetary input as the user types:

- `CurrencyEditText` — extends `AppCompatEditText`.
- `CurrencyMaterialEditText` — extends `TextInputLayout` and wraps a `CurrencyEditText` instance;
  it delegates almost every method to that inner view and additionally renders validation errors
  via `TextInputLayout.setError`.

Gradle modules: `:library` (the published artifact) and `:sample` (demo app).

## Build & test

```bash
./gradlew build                        # what CI runs (JDK 17)
./gradlew :library:test                # JVM unit tests — the main suite
./gradlew :library:connectedAndroidTest # instrumented tests, needs a device/emulator
./gradlew :library:spotlessApply       # format Kotlin (ktlint 1.8.0 + license header)
./gradlew :library:spotlessCheck
./gradlew installGitHook               # copies scripts/pre-commit into .git/hooks
```

Toolchain: Gradle 8.13, AGP 8.13.0, Kotlin 2.1.10, JDK 17 (JDK 21 also works), minSdk 19,
compileSdk/targetSdk 34. Java source/target stays at 1.8, so `kotlin { compilerOptions { jvmTarget
= JVM_1_8 } }` is set in both modules to keep the two compilers in step.

## Architecture

Formatting is not done by the views. It lives in one place:

`CurrencyInputWatcher.onTextModified()` (`library/src/main/java/.../watchers/CurrencyInputWatcher.kt`)
receives the old text, the new text, the inserted fragment and the cursor position, rebuilds the
whole string (sign → currency prefix → integer part → decimal separator → fractional part) and
writes it back together with a recalculated cursor position. Nearly every bug and feature in this
repo is a change to that single function.

Supporting pieces:

- `EasyTextWatcher` — `TextWatcher` with an `ignore` flag so that writing back into the `EditText`
  does not re-enter the watcher. Subclasses override `onTextModified` only.
- `CurrencyInputWatcherConfig` — immutable config (locale, separators, currency symbol, max decimal
  places, negative values, zero padding, `onValueChanged`).
- `util/Utils.kt` — `parseMoneyValue*`, `formatMoneyValue`, locale/API-level helpers (internal).
- `util/Routines.kt` — public `Routines.bigDecimalToString(value, CurrencyFormatConfig)` for
  formatting outside the view; also the `emptyChar = 'n'` sentinel meaning "no grouping separator".
- `res-public/values/attrs.xml` — XML attributes; `library/build.gradle` adds `src/main/res-public`
  as a second res source dir.

Conventions that matter when editing the views:

- Separators are resolved once in the watcher's constructor, so **any** config change on the view
  (`setLocale`, `setSeparators`, `setNegativeValueAllow`, `setMaxNumberOfDecimalPlaces`, …) must go
  through `invalidateTextWatcher()`, which detaches the old watcher and builds a new one.
- The currency symbol is stored as a prefix *with a trailing space* (`"$ "`) and is part of the
  field text; cursor handling in `onSelectionChanged` and in the watcher assumes this.
- A new XML attribute must be added in three places: `attrs.xml` (both `declare-styleable` blocks),
  the `init` block of `CurrencyEditText`, and the `init` block + delegating setter/getter of
  `CurrencyMaterialEditText`.

## Tests

`library/src/test/.../CurrencyInputWatcherTest.kt` is the primary suite. It mocks `EditText` and
`Editable` with Mockito and asserts the exact `setText`/`setSelection` calls, so a test states both
the resulting string and the resulting cursor position. Helpers live in `Exts.kt`
(`runAllWatcherMethods`, `LocaleVars.toWatcher`) and most tests loop over a fixed list of
`LocaleVars` so behaviour is checked against several separator combinations at once.

Instrumented tests (`src/androidTest`) cover the real views and need a device.

Add a failing test to that suite before changing formatting behaviour.

## Style

- Spotless enforces ktlint 1.8.0, 4-space indent, and the Apache header from `spotless.license.kt`
  at the top of every `.kt` file — a new file without that header fails the build.
- Comments and identifiers in English.

## Releasing

A version lives in three files and all of them must be bumped together:

1. `dependencies.gradle` — `publishVersion`, `publishVersionCode`
2. `gradle.properties` — `VERSION_NAME`
3. `history.txt` — one changelog line

Publishing runs from `.github/workflows/publish.yml` on a GitHub release
(`publishAllPublicationsToMavenCentralRepository`, then `closeAndReleaseRepository`); credentials
come from repository secrets.
