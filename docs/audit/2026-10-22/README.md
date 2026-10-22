# Audit 2026-10-22 — CurrencyEditText

Audited `cbc6c6cf01b059c67f3ac94f6dae9fa8b239bc11` on `main` · scope: whole repository · `origin/main` has since moved to `06692ae` (Renovate dependency bumps #63 and #64), which this audit did not read

## State of the project

This is a small Android library, maintained by one person and published to Maven Central, that formats money as the user types it and offers an optional calculator panel. The build and its checks are in good shape: formatting rules, static analysis, coverage, lint and the public-API check all gate every change, and the typing path itself is well covered by tests. The risk sits in the edges that the tests do not reach. When the field is read or written from code, it goes through older helpers that lose precision, cut amounts to two decimals, and can crash the host app on a very long pasted number. The two views also behave differently depending on the order they are set up in, and on whether the user has already tapped into and out of the field. Several of the project's own instructions for contributors no longer match the code. One of them matters now: dependency updates are merged automatically, although the instructions say they never are. This is the first audit of the repository.

## What to do first

1. Read the field's number exactly instead of through a floating-point detour, and treat every zero as zero — this removes the crash on a long paste and the rounding of large amounts (SEC-1, COR-3) — S, security.md
2. Give programmatic writes one formatter that knows the field's decimal places and its "no grouping" setting, and make the public formatting helper use it (SEC-2, ARCH-5) — S, security.md
3. Make the field's own lifecycle consistent: keep the formatting watcher attached, re-render the value after every setting change, apply XML text last in the Material view, and clear its error on a valid result (ARCH-1, ARCH-2, ARCH-3, ARCH-4) — M, architecture.md
4. Close the public-contract gaps: publish the Material view's two documented attributes, add `public.xml` to the contributor recipe, and trim the stale detekt baseline (ARCH-6, DOC-2, ARCH-7) — S, architecture.md
5. Decide whether dependency updates may merge themselves, and make the configuration and the instructions agree (DOC-1) — S, docs.md

## Findings

| ID | Lens | Label | Finding | Effort | Depends on | Status | Verified |
|----|------|-------|---------|--------|------------|--------|----------|
| SEC-1 | security | blocking | Reading the field goes through `Double`: a 310-digit paste crashes the host app, large amounts come back rounded | S | COR-3 | new | confirmed |
| SEC-2 | security | blocking | `setValue` drops every fractional digit past the second | S | — | new | confirmed |
| ARCH-1 | architecture | blocking | Whether the formatting watcher is attached depends on focus history | S | — | new | confirmed |
| ARCH-2 | architecture | blocking | Value carry-over across config changes copied three times, skipped by four setters (`getValue()` → 0) | M | — | new | confirmed |
| ARCH-3 | architecture | blocking | `CurrencyMaterialEditText` formats `app:text` before applying its attributes | S | ARCH-2 | new | confirmed |
| ARCH-4 | architecture | blocking | A validator's `null` clears the error in one view and keeps it in the other | S | — | new | confirmed |
| ARCH-5 | architecture | blocking | `emptyChar` read three ways; `Routines.bigDecimalToString` writes NUL characters | S | SEC-2 | new | confirmed |
| ARCH-6 | architecture | blocking | `public.xml` leaves `app:text` and `app:selectAllOnFocus` private | S | — | new | plausible |
| ARCH-7 | architecture | blocking | detekt baseline keeps five entries for `!!` calls that no longer exist | S | — | new | confirmed |
| COR-1 | correctness | blocking | A currency symbol containing the decimal separator turns 12 into 0.12 | M | — | new | confirmed |
| COR-2 | correctness | blocking | With zero padding, deleting the separator multiplies the value unless there are 2 places | S | — | new | confirmed |
| COR-3 | correctness | blocking | `emptyStringForZero` ignores a scaled zero, which the calculator always produces | S | — | new | confirmed |
| COR-4 | correctness | blocking | An ill-formed locale tag silently falls back to the device locale | S | — | new | confirmed |
| DOC-1 | docs | blocking | AGENTS.md says nothing is automerged; the Renovate preset automerges, and did (#64) | S | — | new | confirmed |
| DOC-2 | docs | blocking | AGENTS.md's new-attribute recipe leaves out `public.xml` | S | — | new | confirmed |
| DOC-3 | docs | blocking | AGENTS.md: "any" config change must invalidate the watcher, `calculatorEnabled` "the one" exception | S | ARCH-2 | new | confirmed |
| DOC-4 | docs | blocking | AGENTS.md's swallowed-exception anti-example points at a documented catch | S | SEC-1, COR-4 | new | confirmed |
| DOC-5 | docs | blocking | README says the two separators are set together; either one alone works | S | — | new | confirmed |
| DOC-6 | docs | blocking | `release.yml` calls the instrumented gate "nine tests"; it is 63 | S | — | new | confirmed |
| DOC-7 | docs | blocking | Calculator comments predate `CalculatorStyle` and the bracket key | S | — | new | confirmed |
| ARCH-8 | architecture | suggestion | `String?.firstChar()` reimplements `firstOrNull()` | S | ARCH-5 | new | confirmed |
| DUP-1 | duplication | suggestion | Calculator states "operand character" / "operator key" in three places | S | — | new | confirmed |
| DUP-2 | duplication | suggestion | SDK levels and JVM target written into both modules | S | — | new | confirmed |
| LEAN-1 | lean | suggestion | `onValueChanged` wrapper object exists for a self-assignment | S | — | new | confirmed |
| LEAN-2 | lean | suggestion | Unused test dependencies and `includeAndroidResources` | S | — | new | confirmed |
| LEAN-3 | lean | suggestion | Redundant clause in `buildNumber` | S | COR-1 | new | confirmed |

## Fix order

- COR-3 goes with SEC-1 in one change — the exact parse keeps scale, so the zero check must stop depending on it.
- SEC-2 before ARCH-5 — ARCH-5 translates `emptyChar` inside the formatter SEC-2 creates; done the other way, the sentinel is handled twice.
- ARCH-2 before ARCH-3 and DOC-3 — once every setter re-renders the value, the Material init order and the AGENTS.md rule are written against the new reconfigure step.
- SEC-1 and COR-4 before DOC-4 — they decide which catches remain for the rule to cite.
- ARCH-5 before ARCH-8 — both edit `util/Routines.kt`.
- COR-1 before LEAN-3 — COR-1 rewrites the number steps `LEAN-3` trims.

## Closed since the last audit

No previous audit.

## Shortcuts

No shortcut markers.

## Not included

Nothing.

Clean lenses: none.

## Not checked

- Nothing ran on a device or emulator. SEC-1's crash and ARCH-5's NUL output were reproduced on the JVM, whose `java.text` code differs from Android's ICU-backed one. ARCH-1's calculator path rests on the framework's focus handling. ARCH-3 and ARCH-4 rest on reading `TextInputLayout` code.
- ARCH-5, minus sign: under `sv` or `fa` default locales, `Routines` writes U+2212 instead of `-`. This was seen on the JVM; Android's ICU symbols were not checked.
- ARCH-6: whether Lint's `PrivateResource` fires on `app:text` in a consumer layout needs a consumer app built against the packaged AAR.
- `origin/main` at `06692ae` (Renovate PRs #63 and #64: AGP 8.13.2, Kotlin 2.4.21, Gradle 8.14.6, Kover 0.9.11, maven-publish 0.37) was not audited. Version claims in AGENTS.md were not checked against it.
- Not read by any finder in full:
  - library and sample layouts, drawables and styles;
  - the bodies of most instrumented tests;
  - the formatter's cursor arithmetic, which was only spot-checked;
  - the detekt baselines other than `library/detekt-baseline-debug.xml`.

## Method

Six finders (security, architecture, correctness, duplication, lean, docs) and one verifier per lens with findings, each in a clean context, on Opus 5.5. The top tier's usage limit was reached at the start of the run. Ledger: no earlier audits, open issues read (#60, the Renovate dashboard, only). 47 findings raised, 42 confirmed, 1 plausible, 4 rejected (see rejected.md). After merging duplicates across lenses, 26 findings remain: 20 blocking and 6 suggestions.
