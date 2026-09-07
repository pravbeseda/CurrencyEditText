# Issue #21 — Upgrade Kotlin, then move ABI validation into the Kotlin plugin

Two pull requests, in this order. PR 2 cannot start before PR 1: the Kotlin Gradle Plugin's
built-in `abiValidation {}` needs Kotlin 2.2+.

## Facts checked before planning (2026-09-07)

- Latest stable Kotlin is **2.4.10** (2.4.20 is still an RC).
- KGP 2.4.10 runs on Gradle 8.13, but warns that it is deprecated and that Kotlin 2.5.0 will
  require **Gradle 8.14.4+**. The wrapper therefore moves to **8.14.5** in PR 1: it removes the
  warning, clears the ceiling before Kotlin 2.5, and lifts the Gradle floor detekt 2.x asks for.
  Gradle 9.x is a separate job — the build still reports pre-existing Gradle 9 deprecations.
- detekt: `io.gitlab.arturbosch:detekt-gradle-plugin` is still at **1.23.8**; detekt 2.x lives
  under the `dev.detekt` group and is still `2.0.0-alpha.6`. detekt therefore **stays at 1.23.8**,
  compiled against Kotlin 2.0.21 — four minor lines behind, so its typed run has to be proven.
- AGP 8.13.0 is not the newest patch (8.13.2 exists), but AGP is outside this issue's scope.

## PR 1 — Kotlin 2.1.10 → 2.4.10

Branch: `chore/21-kotlin-2.4.10`.

1. Raise `kotlin` in `gradle/libs.versions.toml` to `2.4.10`, and the wrapper to Gradle 8.14.5
   via `./gradlew wrapper --gradle-version 8.14.5 --distribution-type bin`.
   Verify: `./gradlew build` is green and the KGP deprecated-Gradle warning is gone.
2. Keep `jvmTarget = JVM_1_8` in `library/build.gradle` and `sample/build.gradle`, and keep
   `sourceCompatibility`/`targetCompatibility` at 1.8. Java target and minSdk 19 are unchanged.
   Verify: no `inconsistent JVM target` warning in the build log.
3. Confirm the detekt gate still works rather than degrading to a warning-only run.
   Verify: `./gradlew detektAll` runs the typed tasks, reports, and a deliberately introduced
   finding (temporary, reverted) is reported. Record the outcome in the PR description.
4. Run `./gradlew :library:apiDump` and confirm the dump is **byte-identical** to the committed
   `library/api/library.api`. A changed signature here is a Kotlin-caused API change and must be
   understood before it is committed, not dumped away.
   Verify: `git diff --exit-code library/api/library.api`.
5. Update the comments that state the Kotlin version and the detekt mismatch — the plugins block
   of the root `build.gradle`, the `binaryCompatibilityValidator` and `detekt` notes in
   `gradle/libs.versions.toml`, and the toolchain paragraph in `CLAUDE.md`.

Done when `./gradlew build` passes locally, the API dump is unchanged, and detekt still reports.

## PR 2 — standalone BCV → the Kotlin plugin's `abiValidation {}`

Branch: `chore/21-builtin-abi-validation`.

1. Enable `abiValidation { enabled = true }` in `:library` and confirm the real task names and the
   real dump path by running the plugin, not from memory (expected: `checkLegacyAbi` /
   `updateLegacyAbi`, dump under `library/api/`). Record what they actually are.
2. Generate the new dump, then diff it against the old `library/api/library.api` line by line.
   The diff must be a **format change only**: no signature may appear or disappear. If a signature
   moves or changes shape, stop and explain it before going further — the artifact goes to Maven
   Central and a release cannot be withdrawn.
3. Remove the standalone plugin: the `library/build.gradle` `plugins` entry, the root
   `build.gradle` `apply false` line and its comment, and the `binaryCompatibilityValidator`
   version plus the `binary-compatibility-validator` plugin alias in `gradle/libs.versions.toml`.
4. Wire the new check into `check` if the plugin does not do it itself, and update the CI step
   `Check binary compatibility` in `.github/workflows/ci.yml` to the new task name.
5. Watch the gate fail once, the way #20 was verified: add a public method, run the check task,
   see it fail with a diff, revert.
6. Update `CLAUDE.md`: the gates table row, the `apiCheck`/`apiDump` commands in "Build & test",
   and the "Public API changes" rule under "Rules for new code".

Done when `./gradlew build` is green, the dump diff is format-only, and the gate has been seen
failing on a deliberate signature change.
