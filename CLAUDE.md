# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

**Umain Launcher** — a minimal but working Android *home launcher* (Kotlin + Jetpack
Compose), plus a step-by-step tutorial (`posts/`, EN + PT-BR) that the demo backs.
Package: `com.umain.launcher`. Single-module app (`:app`).

## Build & run

```bash
./gradlew assembleDebug        # build
./gradlew installDebug         # build + install on the connected device/emulator
./gradlew lint                 # Android Lint
```

- No unit/instrumented tests exist yet; there is no test task to run.
- Requires JDK 17+. `local.properties` (git-ignored) must point `sdk.dir` at the Android SDK.
- To use as the actual home screen: install, press Home, pick **Umain Launcher**. Always
  keep a second launcher installed — this app declares `category.HOME`, and if it's the
  only launcher you can get stuck.

## Toolchain constraints (these are load-bearing — verify before changing)

- **AGP 9 has built-in Kotlin.** Do **not** apply `org.jetbrains.kotlin.android` — AGP 9
  errors out if you do. The module `plugins {}` block is only
  `android.application` + `kotlin.compose`.
- **The Compose Compiler plugin is still required** whenever `buildFeatures { compose = true }`
  is set. Its version must equal AGP's built-in Kotlin version; the version catalog ties
  it to the `kotlin` version via `version.ref`. A version mismatch is the usual Compose
  build failure — align the `kotlin` value in `gradle/libs.versions.toml` to whatever
  AGP's built-in Kotlin reports.
- Do **not** re-add a `kotlin { compilerOptions { jvmTarget } }` block — `jvmTarget`
  defaults to `compileOptions.targetCompatibility` (17) under built-in Kotlin.
- All versions live in `gradle/libs.versions.toml` (version catalog); don't inline them.
- Icons are rendered with `core-ktx`'s `Drawable.toBitmap().asImageBitmap()` — there is
  no Accompanist / image-loading dependency, keep it that way.

## What makes it a launcher (architecture)

Three things, none of them large:

1. **The HOME intent-filter** in `AndroidManifest.xml` (`action.MAIN` + `category.HOME`)
   is the entire difference between "an app" and "a launcher". The activity is
   `singleTask` + `stateNotNeeded`. A `<queries>` block (MAIN/LAUNCHER) grants package
   visibility on Android 11+ without `QUERY_ALL_PACKAGES`.
2. **`res/values/themes.xml`** sets `windowShowWallpaper=true` and a transparent window
   so the system wallpaper shows through. The home surface is kept transparent in Compose;
   only the app drawer is opaque. `minSdk = 26` so an adaptive-icon-only launcher icon
   works (no legacy PNGs).
3. **`data/AppRepository.kt`** is the whole "backend": `queryIntentActivities` for
   MAIN/LAUNCHER apps (off the main thread — `loadIcon`/`loadLabel` do disk I/O), and
   `getLaunchIntentForPackage` + `FLAG_ACTIVITY_NEW_TASK` to launch.

### UI composition flow

`MainActivity` (single Activity, edge-to-edge) → `HomeViewModel` (holds the app list as a
`StateFlow`, refreshed in `init` and `onResume`) → `LauncherRoot` composes the two layers:
`HomeScreen` (transparent, clock + swipe-up gesture) always present, and `AppDrawer`
(searchable grid) sliding over it via `AnimatedVisibility`. System Back closes the drawer
instead of leaving the launcher (`BackHandler`). Data flows down; `launch()`/open-drawer
callbacks flow up.

## Docs stay in sync with the demo

`README.md` and both `posts/*.md` pin exact versions and describe the exact build setup.
When you change `libs.versions.toml`, the AGP/Compose plugin setup, or the icon-rendering
approach, update the version table in `README.md` and the setup section (§2) + code
snippets in **both** posts to match.
