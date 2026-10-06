# Kernel Breach

An **offline Android app that teaches cybersecurity** from zero to job-ready
topics — short lessons, hands-on **simulated** labs (everything runs inside the
app, no real VMs), module checkpoints, and spaced-repetition refreshes. In the
spirit of TryHackMe's learning paths and Duolingo's short sessions, but with no
story, no game layer, no hearts, no timers, no streak pressure.

> "Kernel Breach" is a placeholder display name. Application id
> `com.kernelbreach.app`.

The look is **"Blueprint"**: graph-paper background, a yellow highlighter on the
key idea, plain words, one idea per screen, accessibility first.

---

## Status at a glance

| Layer | What it is | State |
| --- | --- | --- |
| `:core:model` | Pure-Kotlin domain + unlock/lab-placement rules | ✅ built, unit-tested |
| `:core:content` | JSON schema, validator, importer/assembler | ✅ built, unit-tested (loads all 76 files) |
| `:core:srs` | Spaced-repetition engine | ✅ built, unit-tested |
| `:core:database` | Room schema, importer, repositories, Hilt DI | ✅ written (Android; see build note) |
| `:core:design` | Blueprint theme + shared Compose components | ✅ written (Android) |
| `:app` | App shell, navigation, all feature screens | ✅ written (Android) |

**29 unit tests pass** across the three pure-Kotlin core modules — including a
test that loads **every bundled content file** (76 lesson files + curriculum +
5 plans) and checks all the import rules: 6 paths, 96 modules, 433 lessons, 316
labs, PEN correctly "coming soon", deterministic content hash.

### Build-environment note

This repository was developed in a container with **JDK + Gradle but no Android
SDK**, and with Google's Maven repository blocked. As a result:

- The **pure-Kotlin core** (`:core:model`, `:core:content`, `:core:srs`) builds
  and its tests run anywhere — that is what `./gradlew test` executes here, and
  it is green.
- The **Android modules** (`:app`, `:core:design`, `:core:database`) are written
  against the spec and the designs but were **not compiled in that container**
  (no Android SDK). Open the project in Android Studio or build on a machine with
  the SDK to compile and run the app.

`settings.gradle.kts` includes the Android modules only when an Android SDK is
detected (`ANDROID_HOME`/`local.properties`, or `-PwithAndroid=true`), so
`./gradlew test` works both in a headless JVM CI and on a full Android machine.

---

## Architecture

Single Gradle project, multi-module, MVVM + unidirectional data flow, Hilt for
DI, Kotlin Coroutines/Flow. Min SDK 26, Kotlin + Jetpack Compose (Material 3 as
a base, custom Blueprint theme).

```
:core:model      pure Kotlin domain (no Android) — KMP-ready
:core:content    JSON DTOs, parser, validator, assembler (pure Kotlin)
:core:srs        spaced-repetition engine (pure Kotlin, injectable clock)
:core:design     theme, tokens, shared composables
:core:database   Room entities/DAOs, importer, repositories, DI
:app             navigation host, DI wiring, and the feature UIs
```

**Feature modules.** The spec lists `:feature:onboarding`, `:feature:home`, … as
*suggested* modules. In v1 they live as packages under
`com.kernelbreach.app.feature.*` (one package per feature, each with a `Screen`
+ `ViewModel`), structured so each can be extracted into its own Gradle module
later with no code change. The hard, logic-heavy work is already in the shared
`:core:*` modules.

**KMP readiness.** The domain and content/SRS layers are free of Android types
(they are `kotlin("jvm")` modules), so a later iOS target via Kotlin
Multiplatform is possible, as the spec requires.

### Data flow

1. On first launch (and after each app update) `ContentAssembler` reads the
   bundled JSON from assets, validates it, and `RoomContentImporter` writes the
   content tables **in one transaction, keyed by stable ids** (`CODE`,
   `CODE#L<n>`, `CODE#LAB<n>`), leaving all user-state tables untouched. A
   content **hash** makes re-import idempotent.
2. Repositories expose the assembled `Curriculum` and user progress as domain
   models / Flows.
3. ViewModels compose repositories into screen state.

---

## Content

Source of truth lives at the repo root and is packaged into the app's assets by
a Gradle task (single copy, no duplication in version control):

```
content/plans/curriculum.json   list of paths + module metadata
content/plans/<KEY>.json         per-module outcomes, lesson/lab plans, capstone
content/lessons/<CODE>.json      authored lesson content (76 files)
design/                          13 screens (PNG) + design-canvas HTML sources
docs/                            human-readable PDF renderings (review only)
```

Paths: **ROOK** (Cybersecurity Rookie) → **CORE** (Security Core) → the
specializations **SOC**, **PEN**, **ENG**, **AI** (in any order). Module codes
look like `ROOK-03`, `CORE-07`, `SOC-12`.

Do **not** hand-edit lesson text except to fix obvious formatting; it is AI
drafted and will be human-reviewed later (see the `reviewed` flag below).

---

## Building and running

Prerequisites: JDK 17+, Android SDK (for the app), Android Studio recommended.

```bash
# Pure-Kotlin core tests (no Android SDK needed)
./gradlew test

# Full app (needs an Android SDK; -PwithAndroid forces the modules in if your
# environment doesn't expose ANDROID_HOME)
./gradlew :app:assembleDebug
```

### Release signing

`:app` reads signing config from an untracked `keystore.properties` at the repo
root (never commit a keystore):

```properties
storeFile=/absolute/path/to/release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Without that file the release build is left unsigned. R8 + resource shrinking
are enabled for release.

---

## Learning engine (how it works)

- **Lesson flow (~15 min):** Learn (sections with highlights, snippet, worked
  example, key terms) → Try (the 4 check questions, one at a time, with
  immediate explanations) → Complete (score + "You can now explain" chips +
  confidence rating Shaky/Okay/Solid).
- **Spaced repetition** (`:core:srs`, fully unit-tested, injectable clock):
  initial interval from confidence (Shaky 1d / Okay 3d / Solid 7d); on refresh,
  Missed → reset to 1d (+lapse), Slow → ×1.3 (min 3d), Easy → ×2.5 (min 7d, max
  120d). Daily plan = 1 lesson + up to 5 due reviews + up to 5 quick-checks,
  trimmed to ~20 minutes.
- **Checkpoint:** 10 questions in random order (stored seed), 80% to pass,
  review wrong answers, retake allowed; misses feed into review items.
- **Unlock rules** (data-driven, no hard-coded path keys): foundation path open;
  core unlocks after the foundation's final checkpoint or a placement pass;
  specializations unlock after core's final checkpoint but can be "opened
  anyway". Lessons are sequential; a lab unlocks after the lesson it follows;
  the checkpoint unlocks when every lesson is done.
- **Simulated labs:** guided-script mode in v1 — each step shows an instruction
  and a simulator-styled control; running/revealing shows the output; a final
  screen asks for the learner's finding and self-checks it. The `LabSimulator`
  data format reserves optional `expect` / `answer_options` fields for a later
  version with real command matching.

---

## Known gaps and risks (v1)

- **PEN content is missing.** The Penetration Testing path has curriculum
  metadata only (20 modules, no lesson files). The app shows its modules as
  **"Coming soon"** — content-driven, so adding `content/lessons/PEN-xx.json`
  later needs no code change.
- **Content is AI-drafted, not human fact-checked.** Every module carries a
  `reviewed` flag (default `false`); a hidden **dev screen** (You → Developer)
  lists unreviewed modules for the review pass.
- **Simulated labs are guided scripts**, not real command parsing.
- **Reading depths (Simple / Analogy)** are not in the data yet. The Learn
  screen shows the single body text as the "Technical" depth and the depth
  toggle is hidden until `simple`/`analogy` fields are added (the DTOs already
  parse them).
- **Fonts are not bundled.** The design calls for Bricolage Grotesque / Hanken
  Grotesk / IBM Plex Mono; the font binaries are not in the content bundle, so
  the type system falls back to system fonts. Drop the `.ttf` files into
  `core/design/src/main/res/font` and swap the three `FontFamily` values in
  `Type.kt` to match the design and stay fully offline.
- **Certification mentions are guidance only**, not official exam prep.
- **Hours shown** (~486 h across the 5 written paths) are model study-time
  estimates (lesson 15 min, lab 30/40/50 min by level, checkpoint 30 min,
  capstone 2–8 h).
- **Screenshot tests** are scaffolded for the design-system components
  (Roborazzi); extending them to full per-screen goldens against
  `design/screens/*.png` requires rendering each screen from fake state (the
  screens are structured as state + stateless content to make this
  straightforward) and an Android SDK to run.

---

## Definition of done (v1 target)

All 76 modules import without errors; a learner can complete the Rookie path end
to end offline (lessons, labs, checkpoints), receive daily refreshes, resume
where they left off, change accessibility settings, and use dark mode. Unit
tests for importer, unlock rules and SRS pass; screenshot tests exist for the
designed components.

The import/validation, unlock and SRS logic is **done and test-verified**; the
Android UI is implemented and ready to compile/run on a machine with the Android
SDK.
