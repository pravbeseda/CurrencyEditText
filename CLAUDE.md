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
./gradlew build                        # everything CI runs (JDK 17)
./gradlew :library:test                # JVM unit tests — the main suite
./gradlew :library:connectedAndroidTest # instrumented tests, needs a device/emulator
./gradlew spotlessApply                # format Kotlin (ktlint + license header)
./gradlew spotlessCheck
./gradlew :library:koverVerifyDebug :library:koverLogDebug  # coverage + the bound
./gradlew detektAll                    # detekt with type resolution, both modules
./gradlew :library:lintDebug :sample:lintDebug              # Android Lint
./gradlew :library:checkKotlinAbi      # public API against library/api/library.api
./gradlew :library:updateKotlinAbi     # rewrite that file after an intended API change
./gradlew buildHealth                  # dependency audit, run by hand — not part of `check`
```

Toolchain: Gradle 8.14.5, AGP 8.13.0, Kotlin 2.4.10, JDK 17 (JDK 21 also works), minSdk 24,
compileSdk/targetSdk 37. AGP 8.13.0 prints a warning that it has not been tested against compile
SDK 37; it is left visible rather than silenced with `android.suppressUnsupportedCompileSdk`, and
it goes away with the next AGP upgrade. Java source/target stays at 1.8, so `kotlin { compilerOptions { jvmTarget
= JVM_1_8 } }` is set in both modules to keep the two compilers in step.

Every version lives in `gradle/libs.versions.toml`. A coordinate written into a build script
instead is a bug, not a shortcut.

Updates arrive through Renovate, configured in `renovate.json5`: weekly, patch releases
automerged, minor and major read by a person, androidx grouped, the toolchain never automerged.
The file only configures the bot — the GitHub App itself is installed on the repository by hand.

`androidx.core` is held at 1.17.0 on purpose: 1.18.0 and above require Android Gradle plugin
9.1.0. Renovate will keep offering the newer one; the pull request waits for the AGP upgrade.

Git hooks live in `.githooks/` and are installed by `settings.gradle`, which points
`core.hooksPath` at that directory on every Gradle invocation (skipped when `CI` is set). Nothing
has to be run by hand on a fresh clone.

## Quality gates

All six block the build, and `./gradlew build` runs all six:

| Gate | Where it is configured | What is frozen |
|---|---|---|
| Spotless (ktlint + license header) | `spotless.gradle` | nothing — formatting is fixed, not frozen |
| detekt 1.23.8, type resolution | `config/detekt/detekt.yml`, root `build.gradle` | `*/detekt-baseline-*.xml` |
| Kover 0.9.9, `minBound(80)` | `library/build.gradle` | — the bound is a floor, not a baseline |
| Android Lint, `warningsAsErrors` | root `build.gradle` | `*/lint-baseline.xml` |
| Unit tests | `library/src/test` | — |
| ABI validation (Kotlin plugin's `abiValidation {}`) | `library/build.gradle` | `library/api/library.api` — the public API, not a debt list |

Coverage is measured on `:library` only and filtered to
`ru.pravbeseda.currencyedittext.watchers.*` and `…util.*`. The two Views cannot be reached from
`src/test`, so an unfiltered figure would report the share of View code in the module rather than
the quality of the tests.

The plain `detekt` task is disabled on purpose: it analyses without type resolution and would offer
a green run that checks a fraction of what the gate checks. Use `detektAll`, which is the same list
of tasks each module's `check` is wired from.

`library/api/library.api` is a dump of the published artifact's public signatures, and
`checkKotlinAbi` is the only gate here that looks at the library's outward boundary rather than at
the code inside it. It is not a baseline: an intended API change is made by running
`./gradlew :library:updateKotlinAbi` and committing the rewritten file, so the change arrives in
the diff as a reviewable line instead of reaching Maven Central unnoticed — where it cannot be
taken back. The Kotlin Gradle Plugin wires it into `check` itself, and CI runs it as its own step
because that job runs the gate tasks one by one, not `check`.

The gate is the Kotlin plugin's own `abiValidation {}`, enabled in `library/build.gradle`, rather
than the standalone `binary-compatibility-validator` plugin it replaced in #21. Same reference
file, byte for byte: the swap changed no line of the dump. `checkLegacyAbi` and `updateLegacyAbi`
exist as aliases of the two tasks above — do not use them, they are the deprecated spelling.

`dependency-analysis` is the one check here that is **not** a gate. `./gradlew buildHealth` reports
misdeclared dependencies — unused, used but undeclared, on the wrong configuration — and is run by
hand, never from `check`: it has no baseline, and "unused" is a false positive by construction for a
dependency pulled in only for its resources (`material` in `:sample` is reported today, and is
correct as declared). Read its advice, do not obey it. It exists because this repository has already
shipped `androidx.test:monitor` on `implementation` of the published library (`6b75231`).

## Architecture

Formatting is not done by the views. It lives in one place:

`CurrencyTextFormatter.format()` (`library/src/main/java/.../watchers/CurrencyTextFormatter.kt`)
receives the old text, the new text, the inserted fragment and the cursor position, and returns the
text the field should show together with the position the cursor should end up at. It is a pipeline
of named steps, each taking and returning a `TextWithCursor`: `normalizeInput` (fixes that depend on
what was just typed) → `extractSign` → `restoreCurrencyPrefix` → `keepLastDecimalSeparator` →
`buildNumber` (integer and fractional parts) → `composeResult` (currency symbol, sign, grouping
separators, cursor clamped to the result). The class is `internal` and holds no reference to a view,
so it is unit-testable on its own. Nearly every bug and feature in this repo is a change to one of
those steps.

Supporting pieces:

- `CurrencyInputWatcher` — the `EditText` adapter: resolves the separators once, calls the formatter
  from `onTextModified`, writes the result back and fires `onValueChanged`.
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

Instrumented tests (`src/androidTest`) cover the real views and need a device. Every CI run
compiles them; they are executed on an API 24 emulator — this library's minSdk — by the
`instrumented` job of `ci.yml` on every push to `main`, and again by `release.yml` as a gate before
the irreversible publish. They deliberately do not run on pull requests: the emulator costs about
ten minutes and the two Views change rarely.

### Testing and Definition of Done

**Tests are written first. This is a rule, not a preference.**

A change is done only when all of the following hold:

1. `./gradlew build` passes locally and you have seen the output.
2. New or changed logic in `library/src/main` is covered by a test written **before** the
   implementation (red → green → refactor). Watch the test fail for the expected reason first, and
   report that red run in the summary.
3. A bug fix starts with a test that reproduces the bug and fails before the fix. Name the issue in
   the test: `` `Issue #42 — cursor jumps after deleting the grouping separator` ``.
4. Behaviour changes are reflected in tests, not only in the code.
5. Logic that cannot be tested from `src/test` is a signal to move it out of the View into the
   watcher or into `util/` — not a licence to skip the test.

The only exceptions are pure renames, comment/doc edits and build-script changes. If a test could
not be written, say so in the PR description: "No tests, because …". A stated reason is acceptable;
silence is not.

## Rules for new code

Each rule carries its reason, because a rule without one gets optimised away.

**Never weaken a check to make it pass.** No `@Suppress`, no new baseline entries, no lowered
coverage bound, no `@Ignore` — without explicit permission. Baselines only ever shrink: an entry
goes when its finding is fixed.

**Do not grow the formatter's steps.** A new formatting rule is a new step of
`CurrencyTextFormatter.format()` with a speaking name, not another `if` in the middle of an existing
one. Reason: this pipeline is the single point almost every change in this project touches, and it
has already been ~145 lines in one function once.

**No `Array<Any?>` and no casting the result back out** — a composite result is returned as a data
class, the way every step of the formatter returns a `TextWithCursor`.

**No silently swallowed exceptions.** A `catch` that returns a default needs a comment saying why
losing the information is safe (`util/Utils.kt` `parseMoneyValue` is the example of how not to).

**Public API changes get their own bullet in the PR description**, headed "Public API change",
and the `updateKotlinAbi` diff in the same commit. Reason: the artifact goes to Maven Central and a
release cannot be taken back.

**Test libraries belong in `testImplementation` / `androidTestImplementation`, never in
`implementation`.** Reason: this repository has already shipped that bug — `androidx.test:monitor`
leaked into the published library's runtime dependencies (`6b75231`).

A new XML attribute goes in three places (see Architecture above) and gets a test for its default
value in both components.

## Style

- Spotless enforces ktlint 1.8.0, 4-space indent, and the Apache header from `spotless.license.kt`
  at the top of every `.kt` file — a new file without that header fails the build.
- Comments and identifiers in English.

## Releasing

Releases are cut by the `Release` workflow (`.github/workflows/release.yml`), started by hand from
the Actions tab with a `patch` / `minor` / `major` choice. It runs `scripts/bump-version.sh`, which
raises `VERSION_NAME` and `VERSION_CODE` in `gradle.properties` — the single place a version lives,
read by the publish plugin and by `sample/build.gradle` alike; then builds and tests, publishes to
Maven Central, pushes the bump commit and the tag to `main`, and creates a GitHub release with
auto-generated notes. Tags carry no `v` prefix (`1.0.4`), matching every tag since 0.4.0.

The workflow refuses to run from any branch but `main`. Before publishing it checks that `main` has
not moved, so a release cannot silently omit something merged while it was building; and after
publishing it rebases the bump commit onto `main` before pushing, so a merge landing during the
publish itself cannot leave the released version without its commit and tag.

Publishing goes through the Central Portal (`SONATYPE_HOST=CENTRAL_PORTAL`) and is irreversible, so
it runs before anything is pushed — a failure there leaves the repository untouched. Credentials
come from the repository secrets `OSSRH_USERNAME` / `OSSRH_PASSWORD`, which must hold a Central
Portal user token, and `SIGNING_IN_MEMORY_KEY` / `SIGNING_IN_MEMORY_KEY_PASSWORD`.
