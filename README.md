# Umain Launcher

A minimal, working Android home launcher built with **Kotlin + Jetpack Compose**,
inspired by [NoLagLauncher](https://github.com/M1nexoff/NoLagLauncher).

It shows what actually turns an app into a *launcher* and keeps everything else
as small as possible:

- A wallpaper-facing **home screen** with a live clock and a **favorites dock**.
- A **swipe-up app drawer** with a grid searchable by label *or* package name.
- Tap to launch; **long-press** for App info, copy package name, pin, hide or uninstall.
- **Multi-select mode** to hide or uninstall several apps at once.
- **Hidden apps** persist (DataStore) and can be revealed with a toggle.

- **Settings** (long-press the home screen, or the gear in the drawer): **layout**
  (icon grid or a **Minimal AF** text-only list — names, no icons), grid columns,
  icon size, icon **shape** (circle / squircle / rounded) and **color filter**
  (grayscale / desaturated / sepia), theme (system/light/dark, Material You **or**
  a preset accent), and wallpaper (system picker or pick-and-apply).
- **Movable home widgets** — the clock, favorites dock and a **status widget**
  (battery / storage / memory) are each draggable, snap to a grid, and resize via a
  corner handle. Positions persist; "Reset home layout" in Settings › Home.
- **System app widgets** — host third-party home-screen widgets via `AppWidgetHost`.
  Add from Settings › Home › *Add widget*; move (handle), resize and remove (×) like
  the built-in widgets. Bindings and placement persist.

Dev/pentest oriented:

- **Activity Launcher / package inspector** — inspect a package (version, SDK levels,
  **signing SHA-256**, installer source, `debuggable`/`cleartext`/`allowBackup`/`system`
  flags, **permissions with grant state + protection level**, **exported components**),
  **export its APK**, and fire any of its activities directly.
- **Dev shortcuts** — quick chips into Developer options, Device info and Settings.

> The full step-by-step tutorial lives in [`posts/`](posts/) — in
> [English](posts/how-to-build-an-android-launcher.en.md) and
> [Português-BR](posts/como-criar-um-launcher-android.pt-BR.md).

## Screenshots

Captured on a Samsung Galaxy A34 (Android 16, One UI 8) running the debug build as
the default home app.

### Home & drawer

| Home screen | App drawer | Package search |
|:---:|:---:|:---:|
| ![Home screen with clock, status widget, a hosted Google Search widget and the favorites dock](docs/screenshots/01-home.jpg) | ![App drawer showing the searchable icon grid and dev shortcut chips](docs/screenshots/02-app-drawer.png) | ![Drawer filtered by the package prefix com.google](docs/screenshots/03-package-search.png) |
| Live clock, the battery/storage/memory **status widget**, a hosted **system app widget**, and the favorites dock — each movable and resizable. | Searchable grid with **Dev options / Device info / Settings** shortcut chips. | The search box matches the **package name**, not just the label. |

### App actions

| Long-press actions | Multi-select | Minimal layout |
|:---:|:---:|:---:|
| ![Bottom sheet with App info, Copy package name, Activities, Pin to dock, Hide from drawer, Uninstall and Select multiple](docs/screenshots/04-app-actions.png) | ![Drawer in selection mode with four apps selected and hide/uninstall actions in the top bar](docs/screenshots/05-multi-select.png) | ![Text-only right-aligned app list](docs/screenshots/06-minimal-layout.png) |
| App info, copy package name, activity launcher, pin, hide, uninstall. | Hide or uninstall several apps in one pass. | **Minimal AF** — names only, no icons. |

### Settings

| Layout & icons | Theme, home & wallpaper | Widget picker |
|:---:|:---:|:---:|
| ![Settings showing layout mode, grid columns, icon size, icon shape and icon filter](docs/screenshots/07-settings-layout.png) | ![Settings showing theme mode, dynamic color, status widget toggle, Add widget and wallpaper buttons](docs/screenshots/08-settings-home.png) | ![Dialog listing installed app widgets to add to the home screen](docs/screenshots/09-widget-picker.png) |
| Grid or Minimal, columns, icon size, **shape** and **color filter**. | Theme mode, Material You, home-layout controls and wallpaper. | *Add widget* lists every installed `AppWidgetProvider`. |

### Dev / pentest tools

| Package inspector | Permissions | Activity launcher |
|:---:|:---:|:---:|
| ![Package details: version, SDK levels, UID, installer, install dates, signing SHA-256, APK path and manifest flags](docs/screenshots/10-package-inspector.png) | ![Permission list with protection level and grant state badges, including dangerous ones](docs/screenshots/11-permissions.png) | ![Activity list with exported and internal badges](docs/screenshots/12-activity-launcher.png) |
| Version, SDK levels, UID, installer, dates, **signing SHA-256**, APK path, manifest flags. | Every requested permission with its **protection level** and **grant state**. | Fire any activity directly; `exported` vs `internal` is called out. |

## Requirements

Verified against the current stable toolchain (July 2026):

| Tool | Version |
|------|---------|
| Android Studio | Quail 1 (2026.1.1) or newer |
| Android Gradle Plugin (AGP) | 9.2.1 |
| Gradle | 9.6.1 |
| Kotlin | 2.4.10 (bundled via AGP's built-in Kotlin) |
| Compose BOM | 2026.06.01 |
| JDK | 17+ (the bundled Android Studio JBR works) |
| compileSdk / targetSdk | 37 · minSdk 26 (Android 8.0+) |
| SDK Build Tools | 36.0.0 |

## Run it

1. Open the project in Android Studio and let it sync.
2. Run the `app` configuration on an emulator or device.
3. Press the **Home** button and pick **Umain Launcher** → *Always*.

From the command line (SDK configured in `local.properties`):

```bash
./gradlew installDebug
```

To go back to your normal launcher: **Settings → Apps → Default apps → Home app**.

## Project layout

```
app/src/main/
├── AndroidManifest.xml           # HOME intent-filter + <queries> — this is what makes it a launcher
├── java/com/umain/launcher/
│   ├── MainActivity.kt           # single Activity, hosts Compose
│   ├── data/
│   │   ├── AppInfo.kt            # one launchable app
│   │   └── AppRepository.kt      # queries + launches apps via PackageManager
│   └── ui/
│       ├── HomeViewModel.kt      # holds the app list (StateFlow)
│       ├── LauncherRoot.kt       # home + drawer composition
│       ├── HomeScreen.kt         # clock + swipe-up gesture
│       ├── AppDrawer.kt          # searchable app grid
│       └── theme/                # Material 3 theme
└── res/                          # icons, strings, launcher theme (transparent → wallpaper shows)
```

## License

Sample/demo code for the accompanying blog post. Use freely.
