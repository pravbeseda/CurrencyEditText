# Docs — audit 2026-10-22

## DOC-1 · blocking · AGENTS.md says Renovate automerges nothing; the shared preset automerges every minor and patch update, and already has

- **Where:** `AGENTS.md:42-46`, `renovate.json5:4` (`extends: ['github>pravbeseda/renovate-config']`, no `automerge` override); `.github/workflows/ci.yml` (`instrumented` job: `if: github.event_name != 'pull_request'`)
- **What this is:** AGENTS.md describes how dependency updates arrive: weekly, grouped, majors behind Dependency Dashboard approval, "nothing automerged".
- **Problem:** The preset's `default.json` has carried `{"matchUpdateTypes": ["minor","patch","digest"], "groupName": "{{manager}} non-major", "automerge": true}` since pravbeseda/renovate-config#8 (2026-10-10) — two days after 9b8d215 wrote "nothing automerged" here — and `renovate.json5` does not opt out. It has already happened: PR #64 ("chore(deps): update gradle non-major", Kotlin 2.4.20 → 2.4.21, Gradle wrapper 8.14.5 → 8.14.6) was merged by `app/renovate` on 2026-10-17 with no review. PR CI skips the instrumented tests, so these bumps — which include `material`, an `api` dependency of the published artifact — reach `main` without the emulator run.
- **Fix direction:** The owner picks the side: `automerge: false` in `renovate.json5` so the guidance holds, or rewrite the AGENTS.md sentence to state the automerge.
- **If we skip it:** Toolchain and `api`-dependency bumps land on `main` unread while the guidance says they cannot.
- **Effort:** S · **Touches:** `renovate.json5` or `AGENTS.md` · **Depends on:** — · **Status:** new · **Verified:** confirmed

## DOC-2 · blocking · AGENTS.md's recipe for a new XML attribute leaves out `public.xml`

- **Where:** `AGENTS.md:153-156`, `AGENTS.md:226`, `AGENTS.md:143-144`, `library/src/main/res-public/values/public.xml:1-24`
- **What this is:** AGENTS.md says a new attribute shared by both views goes in three places: `attrs.xml` (both blocks), `CurrencyViewAttributes`, and each view's `init`.
- **Problem:** Since d236ec5 `public.xml` lists every public attribute by name and makes every other resource private to the AAR. An attribute added per AGENTS.md is missing from it, so consumers get a lint `PrivateResource` warning and no IDE completion (it still works at runtime) — exactly how ARCH-6 happened. Line 143 also describes `res-public/` as holding only `attrs.xml`; nothing outside `.github/tasks` mentions `public.xml`.
- **Fix direction:** Add `public.xml` to the list at both places (four places) and to the `res-public` bullet.
- **If we skip it:** The next attribute ships to Maven Central as a private resource, and a release cannot be taken back.
- **Effort:** S · **Touches:** `AGENTS.md` · **Depends on:** — · **Status:** new · **Verified:** confirmed

## DOC-3 · blocking · AGENTS.md says "any" config change must go through `invalidateTextWatcher()`, with `calculatorEnabled` "the one" exception

- **Where:** `AGENTS.md:148-150`, `AGENTS.md:157-158`; `library/src/main/java/ru/pravbeseda/currencyedittext/CurrencyEditText.kt:176-179` (`setEmptyStringForZero`), `:393` (`setValidator`)
- **What this is:** The convention for config setters on the views.
- **Problem:** `setEmptyStringForZero` (a shared XML attribute) and `setValidator` correctly do not invalidate the watcher, because neither setting is in `CurrencyInputWatcherConfig`. Read literally, the rule calls both setters bugs and the "the one" exception is false.
- **Fix direction:** Scope the rule to settings carried by `CurrencyInputWatcherConfig` (every setter of those fields does invalidate); the `calculatorEnabled` special case then folds into it. Write it against the reconfigure step ARCH-2 introduces.
- **If we skip it:** A reader either "fixes" a correct setter or stops trusting the rule.
- **Effort:** S · **Touches:** `AGENTS.md` · **Depends on:** ARCH-2 · **Status:** new · **Verified:** confirmed

## DOC-4 · blocking · AGENTS.md names `parseMoneyValue` as the swallowed-exception anti-example; that catch is documented now, and the real offender is `getLocaleFromTag`

- **Where:** `AGENTS.md:215-216`, `library/src/main/java/ru/pravbeseda/currencyedittext/util/Utils.kt:47-53` (catch now commented, since 1d27fa4 / #23), `util/Utils.kt:68-73` (`getLocaleFromTag`)
- **What this is:** The "No silently swallowed exceptions" rule points at `util/Utils.kt` `parseMoneyValue` as "the example of how not to".
- **Problem:** The catch in `parseMoneyValueWithLocale` carries the comment the rule asks for, so the document holds up compliant code as the violation; the only uncommented catch in `library/src/main` is `getLocaleFromTag` (COR-4).
- **Fix direction:** Point the example at whatever remains after SEC-1 (which deletes the `parseMoneyValue` catch) and COR-4, or drop the parenthetical.
- **If we skip it:** A reader concludes that a commented catch is still wrong, or that a silent one is fine.
- **Effort:** S · **Touches:** `AGENTS.md` · **Depends on:** SEC-1, COR-4 · **Status:** new · **Verified:** confirmed

## DOC-5 · blocking · README says the decimal and grouping separators are set together; the XML attributes have honoured either one alone since 9296d34

- **Where:** `README.md:143-144`; `library/src/main/java/ru/pravbeseda/currencyedittext/CurrencyEditText.kt:82-83`, `CurrencyMaterialEditText.kt:86`, `watchers/CurrencyInputWatcher.kt:66-83`; `library/src/androidTest/res/layout/separator_attrs.xml:17-52`
- **What this is:** The XML attribute table says `decimalSeparator` and `groupingSeparator` are each "set together with" the other.
- **Problem:** Since 9296d34/c268588 either XML attribute may be absent ("the locale's own stands in"), `resolveSeparators` moves the locale-filled separator on a collision, and `separator_attrs.xml` tests the single-separator case on both views. The README still says to set both. (The runtime `setSeparators` does take both.)
- **Fix direction:** Change the two table cells to say the other separator comes from the locale when only one is given.
- **If we skip it:** The documented contract is narrower than the tested one.
- **Effort:** S · **Touches:** `README.md` · **Depends on:** — · **Status:** new · **Verified:** confirmed

## DOC-6 · blocking · `release.yml` says the instrumented gate is "nine tests"; it is 63

- **Where:** `.github/workflows/release.yml:68`
- **What this is:** The comment on the release gate step explaining why the emulator run guards the irreversible publish.
- **Problem:** `library/src/androidTest` holds 63 `@Test` methods (18 + 19 in the two View suites, 26 across five `CalculatorPanel*Test`).
- **Fix direction:** Drop the count ("these tests") so it cannot go stale again.
- **If we skip it:** The comment misstates what stands between a merge and Maven Central.
- **Effort:** S · **Touches:** `.github/workflows/release.yml` · **Depends on:** — · **Status:** new · **Verified:** confirmed

## DOC-7 · blocking · Calculator comments describe the panel from before `CalculatorStyle` and the bracket key

- **Where:** `library/src/main/java/ru/pravbeseda/currencyedittext/calculator/CalculatorPopup.kt:68`, `library/src/main/res-public/values/attrs.xml:31`
- **What this is:** The KDoc of `CalculatorPopup`, and the comment on the public `calculatorOperatorTextColor` attribute.
- **Problem:** The KDoc says `CalculatorPopup` is "the only class here that touches Android"; `CalculatorStyle` imports `Context` and `TypedArray`, and AGENTS.md and the Kover excludes name "the two". The attribute comment says the role paints "Operators, sign, backspace, clear and equals"; `isOperator` is also true for `( )`, which `CalculatorPopup.kt:197` paints with it, and README.md:310 says "brackets".
- **Fix direction:** "one of the two classes here that touch Android"; add "brackets" to the attribute comment.
- **If we skip it:** A wrong picture of which code touches Android and which keys a role paints.
- **Effort:** S · **Touches:** `CalculatorPopup.kt`, `attrs.xml` · **Depends on:** — · **Status:** new · **Verified:** confirmed
