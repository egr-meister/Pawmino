# Pawmino

Pawmino is a native Android, fully offline **pet care routine tracker**. You create one or more pet profiles and organize daily care — feeding, walks, grooming, play, water refresh, cleaning, custom tasks, notes, and a shopping list — around a single circular **Daily Pet Care Loop**. You mark each activity complete manually and keep a clear local care history.

> **Positioning:** *Organize your pet's daily routine and keep a clear local care history.*

Pawmino is a practical routine organizer — **not** a veterinary application, diagnosis tool, medical tracker, social network, pet game, or marketplace.

---

## Table of contents

- [Main features](#main-features)
- [Manual pet care tracking](#manual-pet-care-tracking)
- [Care tracking disclaimer](#care-tracking-disclaimer)
- [Emergency disclaimer](#emergency-disclaimer)
- [What Pawmino does not do](#what-pawmino-does-not-do)
- [Architecture & offline storage](#architecture--offline-storage)
- [Data model & schedules](#data-model--schedules)
- [Feature details](#feature-details)
- [Privacy note](#privacy-note)
- [Visual concept](#visual-concept)
- [Technology stack](#technology-stack)
- [Opening & building the project](#opening--building-the-project)
- [Release signing](#release-signing)
- [GitHub Actions](#github-actions)
- [R8 staged enablement](#r8-staged-enablement)
- [Local install & verification](#local-install--verification)
- [Functional test checklist](#local-functional-test-checklist)
- [Data reset behavior](#data-reset-behavior)
- [Manual-entry limitations](#manual-entry-limitations)

---

## Main features

- Manual pet care tracking across multiple pet profiles.
- A large circular **Daily Pet Care Loop** drawn with Compose Canvas (no chart library).
- Care tasks with flexible schedules: daily, selected weekdays, one-time, every N days, and unscheduled.
- Deterministic local generation of daily task instances (no background work).
- Manual completion with Completed / Pending / Skipped states and optional completion notes.
- Feeding schedule with free-text portion/food labels (no nutrition math).
- Manual walk logging (duration, optional distance, note — **no GPS**).
- Grooming and play logging as neutral, user-entered tasks.
- Pet notes journal.
- In-app monthly **calendar**, **day detail**, and reverse-chronological **history** with filters.
- In-app reminders (no push notifications, no background services).
- Local **shopping list** with categories, priority, and check/clear.
- Optional, editable routine **templates**.

## Manual pet care tracking

Everything in Pawmino is entered by you. You create pets and tasks, choose schedules, and tap to mark tasks complete or skipped. Pawmino only records and organizes what you enter; it never observes, measures, or infers anything about your pet automatically.

## Care tracking disclaimer

> Pawmino is a manual pet care routine organizer. Tasks, schedules, notes, and completion records are entered by the user. The app does not provide veterinary advice, diagnosis, treatment, emergency guidance, or medical recommendations.

This disclaimer appears in onboarding, in Settings, in task/feeding guidance, and here in the README.

## Emergency disclaimer

> If your pet appears unwell or needs urgent help, contact a qualified veterinarian or local emergency veterinary service. Pawmino is not an emergency or medical application.

Pawmino has no emergency workflow, no symptom entry, and never classifies tasks as medical treatment.

## What Pawmino does not do

Pawmino does **not**: provide veterinary advice, diagnose health conditions, detect illness, recommend medication or dosages, replace a veterinarian, guarantee improved pet health, measure real physical activity, monitor a pet automatically, connect to a smart collar, track location, identify food allergies, provide nutritional prescriptions, provide emergency instructions, or provide professional grooming advice.

It is also **not** a veterinary app, symptom checker, diagnosis app, telemedicine service, GPS/camera monitor, smart-collar app, social network, adoption marketplace, food store, subscription, cloud profile, or gamified virtual pet.

---

## Architecture & offline storage

Pawmino is a **fully offline** app. It declares **no permissions at all**, has **no `INTERNET` permission**, no backend, no cloud sync, no account, no authentication, no Firebase, no external APIs, no ads, no analytics, and no payments.

The architecture is deliberately simple **MVVM**:

- **Model** — immutable Kotlin `data class`es annotated with `kotlinx.serialization`.
- **Repository** — a single `PawminoRepository` that owns all app data and exposes it as a `Flow`.
- **ViewModel** — one shared `PawminoViewModel` exposing a `StateFlow<PawminoUiState>` plus operations.
- **UI** — Jetpack Compose (Material 3) screens that render immutable UI state.
- **Utilities** — small pure functions (task generation, loop math, reminders, calendar, history) that are easy to unit test.

No dependency injection framework, no domain layer, no Room. Local persistence uses **DataStore Preferences**, storing one serialized JSON string per entity list:

| Key | Contents |
| --- | --- |
| `pets_json` | pet profiles |
| `care_tasks_json` | care tasks |
| `task_instances_json` | daily task instances |
| `walk_logs_json` | walk logs |
| `pet_notes_json` | pet notes |
| `shopping_items_json` | shopping items |
| `settings_json` | app settings (onboarding, active pet, reminders, formats) |

Deserialization is intentionally tolerant: unknown keys are ignored, unknown enum values coerce to safe defaults, and a single malformed object is skipped instead of discarding every valid one. Empty storage, missing keys, empty JSON, and corrupted JSON all fall back to safe defaults and never crash. Private notes and full stored JSON are never logged.

## Data model & schedules

Core models: `PetProfile`, `CareTask`, `DailyTaskInstance`, `WalkLog`, `PetNote`, `ShoppingItem`, `ReminderSettings`, `AppSettings`, and the aggregate `AppData`. All fields have defaults for backward-compatible deserialization. Dates are stored as `yyyy-MM-dd`, times as `HH:mm`, and timestamps as ISO-8601, all using the **local device clock** via `java.time` (with core-library desugaring).

**Schedule types and daily task generation.** Instances are generated deterministically and lazily when a day is viewed — the app does not need to be open at midnight and never uses WorkManager or AlarmManager:

- **Daily** — active every calendar day.
- **SelectedDays** — active only when the date's weekday is selected.
- **OneTime** — active only on its specific date.
- **EveryNumberOfDays** — active on dates that are an exact multiple of the interval (≥ 1) from a stored anchor date, using local-date arithmetic only.
- **Unscheduled** — never generated automatically; you add it to a chosen day manually.

Tasks with multiple times produce one instance per time; tasks without a time produce a single "Anytime" instance. Instance ids are deterministic (`taskId` + date + time, safely encoded), so instances are never duplicated for the same task/date/time, and completed instances are always preserved during regeneration.

## Feature details

**Daily Pet Care Loop.** The Home screen centers on a large segmented circle. Each segment is a care category sized by its task count; the filled arc shows completion, a neutral gray shows skipped, and a faint track shows what remains. The center shows the pet name, `X of Y complete`, and `Z% today`. The loop stays readable with zero, one, or many tasks, and a full textual summary is provided for screen readers. `category completion = completed / active in category`; `daily completion = completed / total active`. Skipped tasks count toward the total but not toward completed.

**Feeding schedule.** Feeding is an ordinary routine category. Optional `portionLabel` and `foodLabel` are free text (for example "1 scoop", "Dry food"). *Feeding labels are entered manually and are not nutritional recommendations.* Pawmino never calculates calories or body-weight portions and makes no dietary or allergy claims.

**Walk tracking.** Walks are entered manually: optional start time, duration, distance, distance unit, and a note. *Walk details are entered manually.* Pawmino uses **no GPS**, no location permission, no route maps, no step counting, and no background tracking. Duration and distance are not treated as measures of health quality.

**Grooming & play tracking.** Grooming (brushing, nail care, bathing, coat/ear cleaning as labels only) and play are neutral, user-entered tasks and logs. Pawmino gives no instructions for invasive procedures and never recommends frequency based on health conditions.

**Notes.** Neutral, free-text notes (title up to 80 chars, body up to 1000). Pawmino does not analyze note text or derive any recommendation from it.

**Calendar & history.** An in-app monthly calendar (no device calendar permission, no Google Calendar) shows per-day indicators using **both color and shape** — filled dot (all complete), half ring (partial), outline (tasks existed but none complete), plus a small mark when a note exists. Day Detail shows the day's loop summary, tasks, walks, and notes. History lists days in reverse-chronological order with filters (all, feeding, walks, grooming, play, notes, completed, skipped).

**In-app reminders.** Reminders are computed only while the app is open — when Home becomes active, the pet or date changes, or a task changes. They cover upcoming tasks (configurable lead time: 15/30/60/120 min), overdue pending tasks, "nothing completed yet today", and high-priority unchecked shopping items. *Pawmino reminders appear inside the app while you use it. The app does not send push notifications or run in the background.*

**Shopping list.** A local checklist (not an e-commerce view) with categories, Normal/High priority, per-pet or shared items, check/uncheck, edit, delete, filters, and "clear checked". No stores, prices, purchase links, product recommendations, or affiliate links.

**Routine templates.** Optional editable examples (Dog Basic, Cat Basic, Small Pet Basic). *Routine templates are editable examples and are not veterinary recommendations.*

## Privacy note

> Pawmino stores pet profiles, care tasks, schedules, completion records, walk logs, grooming logs, notes, shopping items, and settings locally on this device. The app has no account, no cloud sync, no internet access, no ads, no analytics, no payments, no location tracking, no camera access, no push notifications, and no veterinary service.

## Visual concept

Visual style: **"Soft Orbit Care Planner"** — warm, calm, organized, non-medical, non-childish. The main concept is the **Daily Pet Care Loop**. Instead of the common "mascot → title → stats card → button stack" layout, Pawmino uses a unique **radial** Home layout: a compact pet switcher, a narrow date header strip, the central care loop, a "Next care task" ribbon, a horizontally scrollable row of category chips, a vertical timeline grouped by Morning/Afternoon/Evening/Anytime, a compact shopping preview, bottom navigation (Today, Calendar, History, Shopping, Settings), and a floating **Add Task** action placed so it does not cover the loop.

**Care loop drawing.** The loop is drawn with Compose `Canvas` arc primitives only — a faint full-circle track for readability, per-category arcs sized by task count, and filled/skip/track sub-arcs — with no external chart or animation libraries.

**App icon.** A custom adaptive icon: a warm cream background, a segmented care loop in category colors, and a subtle paw-shaped center mark. No animal photo, no veterinary cross, no medical symbol, no text. Legacy density PNGs are provided for API 24–25; adaptive XML covers API 26+.

**Splash screen.** A stable static splash: soft cream background with the centered segmented care-loop logo and the app name. No mascot, no photo, no heavy animation.

## Technology stack

Kotlin, Jetpack Compose, Material 3, Navigation Compose, Android ViewModel, Kotlin Coroutines & Flow, DataStore Preferences, and Kotlinx Serialization. Build uses the Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`). No networking, image-loading, chart, database, or DI libraries are used.

---

## Opening & building the project

**Requirements**

- Android Studio (a recent stable release compatible with Android Gradle Plugin 8.6 and API 35).
- **JDK 17.**
- Android SDK **Platform 35** and **Build Tools 35.0.0**.

**Configuration highlights**

- `compileSdk = 35`, `targetSdk = 35`, `minSdk = 24`.
- Portrait-locked, edge-to-edge with visible system bars.
- Core-library desugaring enabled for `java.time`.
- **16 KB memory page-size compatibility:** because the app uses only Kotlin/Compose/DataStore and ships **no native third-party binaries**, the resulting AAB is compatible with Android 15+ 16 KB page sizes. Still verify the final bundle before release.

**Open in Android Studio**

1. `File → Open` and select the project root.
2. Let Gradle sync. Android Studio generates the Gradle wrapper JAR automatically if it is missing. (You can also run `gradle wrapper --gradle-version 8.9` with a locally installed Gradle.)

**Build a debug APK**

```bash
./gradlew assembleDebug
```

**Build a non-minified release first (recommended)**

Validate a working release build before enabling R8 (see [R8 staged enablement](#r8-staged-enablement)):

```bash
./gradlew assembleRelease
```

---

## Release signing

Release APKs and AABs **must** be signed with a real PKCS12 keystore. The build never falls back to the Android debug key for release artifacts — it fails clearly if signing credentials are missing.

**Generate a keystore**

```bash
keytool -genkeypair -v -storetype PKCS12 \
  -keystore pawmino-release-key.p12 \
  -alias pawmino_key -keyalg RSA -keysize 2048 -validity 10000
```

**Local signing setup**

Copy `keystore.properties.example` to `keystore.properties` (git-ignored) and fill in your values:

```properties
storeFile=pawmino-release-key.p12
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=pawmino_key
keyPassword=YOUR_KEY_PASSWORD
```

`app/build.gradle.kts` reads signing values from **environment variables** (used by CI) or, if those are absent, from `keystore.properties` (used locally). Use the same password for the keystore and key unless you have configured separate values reliably.

**Never commit** the `.p12` keystore, passwords, decoded keystore data, or `keystore.properties`. These are all listed in `.gitignore`.

**Required GitHub Secrets**

| Secret | Purpose |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64 of the PKCS12 keystore |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Key alias (e.g. `pawmino_key`) |
| `ANDROID_KEY_PASSWORD` | Key password |

Create the base64 value with:

```bash
base64 -w0 pawmino-release-key.p12 > keystore.b64   # Linux
# or on macOS:
base64 -i pawmino-release-key.p12 -o keystore.b64
```

## GitHub Actions

The workflow `.github/workflows/android-build.yml`:

1. Runs on push to `main` and supports `workflow_dispatch`.
2. Checks out the repository and sets up **JDK 17**.
3. Installs Android **SDK Platform 35** and **Build Tools 35.0.0**.
4. Configures Gradle with caching and generates the Gradle wrapper.
5. Decodes `ANDROID_KEYSTORE_BASE64` into a temporary PKCS12 file and exposes signing values only as environment variables.
6. Builds the **signed release APK** and **signed release AAB**.
7. Locates the APK/AAB, runs `apksigner verify --print-certs`, prints the certificate, and **fails** if verification fails or if the output contains `CN=Android Debug`.
8. Uploads the signed APK (for testing) and the signed AAB (for Google Play) as artifacts.

CI never prints passwords or the base64 value and does not run an emulator. CI proves stable compilation, signing, certificate verification, and artifact production — it is **not** proof that the app launches; do a local launch test as well.

## R8 staged enablement

The project ships with `proguard-rules.pro` that keeps `kotlinx.serialization` serializers and model classes. The recommended flow:

1. First build and test a release with shrinking **off**:

   ```kotlin
   isMinifyEnabled = false
   isShrinkResources = false
   ```

2. After the non-minified release is built, installed, launched, and tested, enable R8 and resource shrinking (the committed configuration):

   ```kotlin
   isMinifyEnabled = true
   isShrinkResources = true
   proguardFiles(
       getDefaultProguardFile("proguard-android-optimize.txt"),
       "proguard-rules.pro"
   )
   ```

3. Rebuild and reinstall the minified release, then re-test Kotlinx Serialization, DataStore, navigation, and task generation.

## Local install & verification

```bash
# Build + verify certificate
./gradlew assembleRelease
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk

# Install and inspect logs
adb install -r app/build/outputs/apk/release/app-release.apk
adb logcat
```

The certificate **must not** contain `CN=Android Debug`. Build the AAB for Google Play with `./gradlew bundleRelease` (output under `app/build/outputs/bundle/release/`). **Only the `.aab` is uploaded to Google Play**; the APK is for local installation and testing.

When inspecting `adb logcat`, watch for `ClassNotFoundException`, `NoSuchMethodError`, serialization/DataStore parsing crashes, navigation argument crashes, duplicate instance generation, invalid date/time crashes, missing pet/task crashes, Canvas drawing crashes, R8-related crashes, and signing misconfiguration.

## Local functional test checklist

- First launch with empty storage; onboarding; skip onboarding.
- Create a pet; create multiple pets; switch active pet; edit pet; delete an inactive pet; delete the active pet.
- Apply dog and cat routine templates.
- Create daily / selected-weekday / one-time / every-N-days / no-time / multiple-times tasks.
- Edit, disable, enable, delete a task.
- Mark completed / skipped / return to pending; add a completion note.
- Confirm loop updates, category percentages, and **no duplicate task instances**.
- Add feeding labels and confirm **no nutritional calculation** occurs.
- Add a manual walk with duration/distance and confirm **no GPS request** appears.
- Add grooming and play records; add, edit, delete notes.
- Open calendar; navigate months; open an empty day and a completed day.
- Open history; apply filters.
- Add a shopping item; check it; edit it; clear checked items.
- Trigger and dismiss an overdue in-app reminder; disable reminders.
- Reset selected-pet history; reset all data; relaunch.
- Launch in airplane mode and confirm full functionality.
- Confirm no `INTERNET` permission, no runtime permission dialogs, no camera control, and no notification behavior.
- Verify the release certificate, the AAB output, API 35 configuration, and 16 KB page-size compatibility.

## Data reset behavior

- **Delete history for selected pet** removes that pet's completion records, walk logs, and notes, keeping the pet and its task setup.
- **Delete selected pet** removes the pet and its tasks, completion history, notes, and shopping links, then selects another pet or shows the empty setup state.
- **Reset all local data** permanently removes every pet profile, task, completion record, note, walk log, and shopping item. All destructive actions require explicit confirmation.

## Manual-entry limitations

Pawmino only knows what you enter. It does not observe your pet, measure activity, verify that a task truly happened, or provide any medical, nutritional, or diagnostic interpretation. Completion, walks, feeding labels, grooming, play, and notes are all manual records for your own organization.
