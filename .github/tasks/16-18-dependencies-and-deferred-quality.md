# Renovate, the dependency sweep, and the deferred quality work (#16, #18)

One pull request closing both issues. Issue #18's third item — binary compatibility
validation — was already done in #20 and #30; only its first two items remain.

## Decisions taken

- **minSdk 19 → 24.** `material 1.14.0` and `appcompat 1.8.0` both require 23,
  `constraintlayout 2.2.2` requires 21, so minSdk 19 froze almost the whole list.
  24 was chosen over the 23 the dependencies demand. `material` is an `api`
  dependency of the published artifact, so this is a change to the library's
  contract: the next release is a minor bump, not a patch.
- **`core-ktx` stops at 1.17.0.** 1.18.0+ requires Android Gradle plugin 9.1.0.
- **Instrumented tests run on push to `main` and as a gate in `Release`**, not on
  every pull request: the emulator costs ~10 minutes, and pull requests stay fast
  while a View regression still surfaces within minutes of the merge and can never
  reach Maven Central.
- **`dependency-analysis` is a manual audit**, wired as `./gradlew buildHealth` and
  deliberately left out of `check` — it has no baseline, and "unused" is a false
  positive for a dependency pulled in for its resources.
- **One pull request**, not one per issue, at the repository owner's request.

## Steps

1. `gradle/libs.versions.toml`: `appcompat 1.6.1 → 1.8.0`, `constraintlayout
   2.1.4 → 2.2.2`, `coreKtx 1.10.1 → 1.17.0`, `material 1.9.0 → 1.14.0`,
   `mockito 5.20.0 → 5.23.0`, `testCoreKtx 1.5.0 → 1.7.0`, `testExtJunitKtx
   1.1.5 → 1.3.0`, `testRules 1.5.0 → 1.7.0`, `testRunner 1.5.2 → 1.7.0`; add the
   `dependencyAnalysis` plugin. → verify: `./gradlew build` is green.
2. `library/build.gradle` and `sample/build.gradle`: `compileSdk 34 → 37`,
   `minSdkVersion 19 → 24`, `targetSdkVersion 34 → 37`. → verify: both modules
   assemble and `:library:compileDebugAndroidTestSources` passes.
   `OldTargetApi` joins `GradleDependency` and `NewerVersionAvailable` as
   `informational`: it judges `targetSdk` against the newest SDK installed on the
   machine, so it was green locally and red on the runner, and a baseline entry
   for it goes stale the moment `targetSdk` changes.
3. `renovate.json5`: weekly schedule, `config:recommended`, vulnerability alerts,
   dependency dashboard, patch updates automerged, minor and major read by a
   person, androidx grouped, the Kotlin/AGP toolchain never automerged.
   → verify: `npx --yes --package renovate renovate-config-validator`.
4. Root build script: apply `dependency-analysis`, keep it out of `check`.
   → verify: `./gradlew buildHealth` runs and `./gradlew check` does not invoke it.
5. `.github/workflows/ci.yml`: an `instrumented` job on `push` to `main` and on
   `workflow_dispatch`, running `:library:connectedDebugAndroidTest` on an API 24
   emulator. → verify: the workflow parses and the job's `if` excludes pull requests.
6. `.github/workflows/release.yml`: the same emulator step between "Build and run
   tests" and "Check that main has not moved", so it precedes the irreversible
   publish. → verify: the step sits above `publishAndReleaseToMavenCentral`.
7. Re-run every gate: `./gradlew build`, `detektAll`, `:library:lintDebug
   :sample:lintDebug`, `:library:checkKotlinAbi`. Refresh the lint baselines only
   where an entry no longer matches, and only downwards.

## Fallout, handled here

Raising minSdk to 24 turned the API-level guards into lint errors (`ObsoleteSdkInt`),
so `isLollipopAndAbove()`, the two `@RequiresApi(LOLLIPOP)` annotations and the
`if (isLollipopAndAbove() && …)` conditions went with the bump rather than into a
baseline. `isApi26AndAbove()` stays: 26 is still above 24. `sample/res/drawable-v24`
was merged into `drawable` for the same reason, and the `OldTargetApi` entry left
both lint baselines.

## Found on the way, filed separately

`CurrencyMaterialEditTextTest.shouldSetDecimalZerosPadding` asserts on a string with
a separator without pinning the separators first, so it fails on a device whose
locale uses `,` as the decimal separator — #31.
