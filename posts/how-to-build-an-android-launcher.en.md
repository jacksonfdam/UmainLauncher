---
title: "How to build an Android launcher from scratch (with Kotlin & Jetpack Compose)"
description: "A hands-on guide to building a real, working Android home launcher — the HOME intent, reading installed apps, and a Compose UI — with a full demo called Umain Launcher."
tags: [android, kotlin, jetpack-compose, launcher]
date: 2026-07-27
---

# How to build an Android launcher from scratch

Most Android tutorials build *an app*. This one builds **the app that launches every
other app** — a home launcher, the thing that draws your home screen and app drawer.

It sounds like deep system magic, but it isn't. A launcher is just a normal app with
**one special line in its manifest**. Everything else — the clock, the grid of icons,
the search — is ordinary UI on top of one system API you already have access to:
`PackageManager`.

We'll build a small but genuinely functional launcher called **Umain Launcher**,
inspired by the minimalist [NoLagLauncher](https://github.com/M1nexoff/NoLagLauncher).
By the end you'll have:

- a wallpaper-facing **home screen** with a live clock,
- a **swipe-up app drawer** with a searchable grid of every installed app,
- tap-to-launch working end to end.

Stack (current stable toolchain, July 2026): **Kotlin 2.4.10 + Jetpack Compose
(BOM 2026.06.01) + Material 3**, built with **AGP 9.2.1 / Gradle 9.6.1**,
`minSdk 26`, `compileSdk 37`.

> 💡 The complete project accompanies this post — see the `app/` module. Every code
> block below maps to a real file in it.

---

## 1. What actually makes an app a launcher

Android decides which apps can act as the home screen by looking for an `<activity>`
that declares this intent filter:

```xml
<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.HOME" />
    <category android:name="android.intent.category.DEFAULT" />
</intent-filter>
```

`action.MAIN` + `category.HOME` is the whole trick. When the user presses the Home
button, the system fires an implicit intent for exactly this, and offers every app
that matches. That's it — that one filter is the difference between "an app" and
"a launcher."

Everything else in this article is just building a nice screen to put behind it.

---

## 2. Project setup

Create a new **Empty Activity (Compose)** project, or follow along with the demo.
The dependencies are just the standard Compose set — no third-party libraries at all.
Using a Gradle version catalog (`gradle/libs.versions.toml`):

```toml
[versions]
agp = "9.2.1"
kotlin = "2.4.10"
composeBom = "2026.06.01"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version = "1.19.0" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version = "1.13.0" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version = "2.11.0" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

App icons come from the system as `android.graphics.drawable.Drawable`, and Compose
speaks `ImageBitmap` / `Painter`. Instead of pulling in a third-party bridge, we'll
rasterize the drawable ourselves in one line with `core-ktx`'s `toBitmap()` (see
§6.3) — one less dependency to keep up to date.

In `app/build.gradle.kts`, set `minSdk = 26`. Why 26? So we can ship an
**adaptive icon only** (no legacy PNGs), and because launcher-specific behavior is
much cleaner from Android 8 onward. Use `compileSdk = 37` (the current stable SDK,
supported by AGP 9.1.x).

> **AGP 9 note:** since Android Gradle Plugin 9.0, Kotlin support is built in — you no
> longer apply the `org.jetbrains.kotlin.android` plugin. You **do** still apply the
> Compose Compiler plugin (`org.jetbrains.kotlin.plugin.compose`) whenever
> `buildFeatures { compose = true }` is set. So the module's `plugins { }` block is
> `alias(libs.plugins.android.application)` + `alias(libs.plugins.kotlin.compose)`, and
> Kotlin's `jvmTarget` defaults to `compileOptions.targetCompatibility`. Keep the
> Compose Compiler plugin's version equal to AGP's built-in Kotlin version.

---

## 3. The manifest: HOME, wallpaper, and package visibility

Three things go into the manifest.

### 3.1 The HOME intent filter

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTask"
    android:stateNotNeeded="true"
    android:configChanges="keyboard|keyboardHidden|navigation|orientation|screenSize|screenLayout|smallestScreenSize|uiMode">

    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

Two attributes matter for launchers:

- **`launchMode="singleTask"`** — the system keeps *one* instance of your launcher
  alive and routes the Home button back to it, instead of stacking new copies.
- **`stateNotNeeded="true"`** — a launcher must be able to start even if the system
  couldn't restore its saved state (e.g. right after boot).

We keep `category.LAUNCHER` too, so the app also appears as a normal icon while
developing.

### 3.2 Let the wallpaper show through

A launcher shouldn't paint over the wallpaper. We give it a transparent window that
shows the system wallpaper (`res/values/themes.xml`):

```xml
<style name="Theme.UmainLauncher" parent="android:Theme.Material.NoActionBar">
    <item name="android:windowShowWallpaper">true</item>
    <item name="android:windowBackground">@android:color/transparent</item>
    <item name="android:statusBarColor">@android:color/transparent</item>
    <item name="android:navigationBarColor">@android:color/transparent</item>
</style>
```

`windowShowWallpaper` is the key line — the home screen will render the user's
wallpaper behind our clock, because we keep that surface transparent in Compose.

### 3.3 Package visibility (Android 11+)

Since Android 11 (API 30), apps can't freely see the list of other installed apps.
A launcher obviously needs to — so we declare *which* apps we care about with a
`<queries>` block, instead of the heavy-handed `QUERY_ALL_PACKAGES` permission:

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent>
</queries>
```

This says: "let me see every app that has a launchable entry point." Exactly what a
drawer needs, nothing more.

---

## 4. Reading the installed apps

Now the interesting part — and it's shorter than you'd think. The `PackageManager`
already knows every installed app. We ask it for the ones with a MAIN/LAUNCHER entry
point, map each to a small data class, and sort alphabetically.

`AppInfo.kt`:

```kotlin
data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
)
```

`AppRepository.kt` — this is essentially the entire "backend" of a launcher:

```kotlin
class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    suspend fun loadApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val mainLauncherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        packageManager.queryIntentActivities(mainLauncherIntent, 0)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName ?: return@mapNotNull null
                if (packageName == context.packageName) return@mapNotNull null // hide ourselves

                AppInfo(
                    label = resolveInfo.loadLabel(packageManager).toString(),
                    packageName = packageName,
                    icon = resolveInfo.loadIcon(packageManager),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun launchApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
    }
}
```

Two things worth calling out:

- **`loadIcon` / `loadLabel` do disk I/O**, so we run the whole query on
  `Dispatchers.IO`. Never do this on the main thread — it's the #1 cause of a janky
  launcher.
- **Launching** is just `getLaunchIntentForPackage()` + `FLAG_ACTIVITY_NEW_TASK`.
  The flag is required because we're starting the activity from outside a normal
  activity task.

---

## 5. Holding the state

A tiny `ViewModel` keeps the app list across rotations so we don't re-hit the
`PackageManager` on every configuration change. We expose it as a `StateFlow`.

`HomeViewModel.kt`:

```kotlin
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch { _apps.value = repository.loadApps() }
    }

    fun launch(packageName: String) = repository.launchApp(packageName)
}
```

We call `refresh()` again from `MainActivity.onResume()`, so the drawer reflects apps
that were installed or removed while we were away.

---

## 6. The UI, in three composables

### 6.1 The Activity

Nothing special — one Activity, edge-to-edge, hosting Compose:

```kotlin
class MainActivity : ComponentActivity() {
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UmainLauncherTheme { LauncherRoot(viewModel = viewModel) }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
```

### 6.2 Home screen: a clock and a swipe-up gesture

The home screen stays transparent (so the wallpaper shows), draws a big clock, and
detects an upward drag to open the drawer:

```kotlin
@Composable
fun HomeScreen(onOpenDrawer: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -8f) onOpenDrawer() // finger moving up
                }
            },
    ) {
        Clock(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 96.dp))
        // ...a "Swipe up" hint at the bottom
    }
}
```

The clock re-emits the time every second with `produceState`, which is the idiomatic
Compose way to turn a ticking value into state without a manual loop:

```kotlin
@Composable
private fun Clock(modifier: Modifier = Modifier) {
    val now by produceState(initialValue = Date()) {
        while (true) { value = Date(); delay(1_000L) }
    }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Text(text = timeFormat.format(now), fontSize = 72.sp, color = Color.White)
    // ...date line below
}
```

### 6.3 The app drawer: a searchable grid

The drawer is an opaque full-screen surface with a search field and a
`LazyVerticalGrid`. Filtering is just a `remember`ed derivation of the query:

```kotlin
@Composable
fun AppDrawer(apps: List<AppInfo>, onAppClick: (AppInfo) -> Unit, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).statusBarsPadding()) {
        OutlinedTextField(value = query, onValueChange = { query = it }, /* search UI */)

        LazyVerticalGrid(columns = GridCells.Fixed(4)) {
            items(filtered, key = { it.packageName }) { app ->
                AppGridItem(app = app, onClick = { onAppClick(app) })
            }
        }
    }
}
```

Each cell draws the real app icon. The icon arrives as an Android `Drawable`, so we
rasterize it into a Compose `ImageBitmap` with `core-ktx`'s `toBitmap()` — cached per
app with `remember` so it only happens once:

```kotlin
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap

val iconBitmap = remember(app.packageName) {
    app.icon.toBitmap(width = 144, height = 144).asImageBitmap()
}

Image(
    bitmap = iconBitmap,
    contentDescription = app.label,
    modifier = Modifier.size(52.dp),
)
```

> Use `Image`, not `Icon`, for app icons. `Icon` applies a tint and would flatten
> every colorful app icon into a single color.

### 6.4 Wiring home + drawer together

`LauncherRoot` composes the two, slides the drawer up with `AnimatedVisibility`, and
makes the system **Back** gesture close the drawer instead of leaving the launcher:

```kotlin
@Composable
fun LauncherRoot(viewModel: HomeViewModel) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    var drawerOpen by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        HomeScreen(onOpenDrawer = { drawerOpen = true })

        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            AppDrawer(apps = apps, onAppClick = { app ->
                viewModel.launch(app.packageName)
                drawerOpen = false
            })
        }
    }

    BackHandler(enabled = drawerOpen) { drawerOpen = false }
}
```

---

## 7. Run it and set it as default

1. Run the `app` configuration on an emulator or device.
2. Press **Home**. The system asks which launcher to use — pick **Umain Launcher**.
3. To make it stick, choose *Always*, or set it in
   **Settings → Apps → Default apps → Home app**.

To switch back to your normal launcher, use that same settings screen. (You can't
"uninstall your way out" if it's the only launcher — always keep a second one.)

---

## 8. Where to go from here

You now have the skeleton every launcher shares. Real ones add, roughly in order of
effort:

- **Favorites / a dock** — persist a few package names with DataStore and render a
  bottom row on the home screen.
- **App uninstall & info** — long-press a grid item to show a context menu
  (`Intent.ACTION_DELETE`, or the app-details settings screen).
- **Live icon updates** — register a `BroadcastReceiver` for
  `ACTION_PACKAGE_ADDED` / `REMOVED` and call `refresh()` instead of polling in
  `onResume`.
- **Widgets** — the big one: `AppWidgetHost` lets you embed home-screen widgets.
- **Notification badges & gestures** — double-tap to lock, swipe-down for
  notifications (`StatusBarManager`), etc.

But the core is exactly what you built here: one intent filter, one `PackageManager`
query, and a Compose surface over the wallpaper. Everything else is polish.

---

### The demo

The full source — **Umain Launcher** — is in the `app/` module next to this post.
Open it in Android Studio, run it, and press Home. Happy hacking. 🚀
