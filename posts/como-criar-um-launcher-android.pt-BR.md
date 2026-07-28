---
title: "Como criar um launcher Android do zero (com Kotlin e Jetpack Compose)"
description: "Um guia prático para construir um launcher Android de verdade — a intent HOME, a leitura dos apps instalados e uma UI em Compose — com uma demo completa chamada Umain Launcher."
tags: [android, kotlin, jetpack-compose, launcher]
date: 2026-07-27
---

# Como criar um launcher Android do zero

A maioria dos tutoriais de Android constrói *um app*. Este aqui constrói **o app que
abre todos os outros apps** — um launcher (a tela inicial), aquele que desenha sua
home e a gaveta de aplicativos.

Parece mágica profunda do sistema, mas não é. Um launcher é só um app normal com
**uma linha especial no manifest**. Todo o resto — o relógio, a grade de ícones, a
busca — é UI comum por cima de uma única API do sistema à qual você já tem acesso:
o `PackageManager`.

Vamos construir um launcher pequeno, porém realmente funcional, chamado
**Umain Launcher**, inspirado no minimalista
[NoLagLauncher](https://github.com/M1nexoff/NoLagLauncher). No fim você terá:

- uma **tela inicial** que mostra o papel de parede, com um relógio ao vivo;
- uma **gaveta de apps** que abre deslizando pra cima, com uma grade pesquisável de
  todos os apps instalados;
- toque-para-abrir funcionando de ponta a ponta.

Stack (toolchain estável atual, julho/2026): **Kotlin 2.4.10 + Jetpack Compose
(BOM 2026.06.01) + Material 3**, construído com **AGP 9.2.1 / Gradle 9.6.1**,
`minSdk 26`, `compileSdk 37`.

> 💡 O projeto completo acompanha este post — veja o módulo `app/`. Cada bloco de
> código abaixo corresponde a um arquivo real dele.

---

## 1. O que de fato transforma um app em launcher

O Android decide quais apps podem ser a tela inicial procurando por uma `<activity>`
que declare este intent filter:

```xml
<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.HOME" />
    <category android:name="android.intent.category.DEFAULT" />
</intent-filter>
```

`action.MAIN` + `category.HOME` é o truque inteiro. Quando o usuário aperta o botão
Home, o sistema dispara uma intent implícita exatamente para isso e oferece todos os
apps que combinam. É só isso — esse filtro é a diferença entre "um app" e "um
launcher".

Todo o resto deste artigo é apenas construir uma tela bonita para colocar atrás dele.

---

## 2. Configuração do projeto

Crie um projeto novo **Empty Activity (Compose)**, ou acompanhe pela demo. As
dependências são apenas o conjunto padrão do Compose — nenhuma biblioteca de
terceiros. Usando um version catalog do Gradle (`gradle/libs.versions.toml`):

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

Os ícones dos apps vêm do sistema como `android.graphics.drawable.Drawable`, e o
Compose fala `ImageBitmap` / `Painter`. Em vez de puxar uma ponte de terceiros, vamos
rasterizar o drawable nós mesmos em uma linha com o `toBitmap()` do `core-ktx`
(veja §6.3) — uma dependência a menos para manter atualizada.

No `app/build.gradle.kts`, defina `minSdk = 26`. Por que 26? Para podermos entregar
**apenas um ícone adaptativo** (sem PNGs legados) e porque o comportamento específico
de launcher fica bem mais limpo a partir do Android 8. Use `compileSdk = 37` (o SDK
estável atual, suportado pelo AGP 9.1.x).

> **Nota AGP 9:** desde o Android Gradle Plugin 9.0, o suporte a Kotlin é embutido —
> você não aplica mais o plugin `org.jetbrains.kotlin.android`. Mas você **ainda**
> aplica o Compose Compiler plugin (`org.jetbrains.kotlin.plugin.compose`) sempre que
> `buildFeatures { compose = true }` estiver ligado. Ou seja, o bloco `plugins { }` do
> módulo fica `alias(libs.plugins.android.application)` +
> `alias(libs.plugins.kotlin.compose)`, e o `jvmTarget` do Kotlin herda de
> `compileOptions.targetCompatibility`. Mantenha a versão do Compose Compiler plugin
> igual à versão do Kotlin embutido do AGP.

---

## 3. O manifest: HOME, papel de parede e visibilidade de pacotes

Três coisas entram no manifest.

### 3.1 O intent filter HOME

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

Dois atributos importam para launchers:

- **`launchMode="singleTask"`** — o sistema mantém *uma* instância do seu launcher
  viva e redireciona o botão Home de volta para ela, em vez de empilhar novas cópias.
- **`stateNotNeeded="true"`** — um launcher precisa conseguir iniciar mesmo que o
  sistema não tenha conseguido restaurar seu estado salvo (por exemplo, logo após o
  boot).

Mantemos também o `category.LAUNCHER`, para que o app apareça como um ícone normal
durante o desenvolvimento.

### 3.2 Deixe o papel de parede aparecer

Um launcher não deve pintar por cima do papel de parede. Damos a ele uma janela
transparente que mostra o wallpaper do sistema (`res/values/themes.xml`):

```xml
<style name="Theme.UmainLauncher" parent="android:Theme.Material.NoActionBar">
    <item name="android:windowShowWallpaper">true</item>
    <item name="android:windowBackground">@android:color/transparent</item>
    <item name="android:statusBarColor">@android:color/transparent</item>
    <item name="android:navigationBarColor">@android:color/transparent</item>
</style>
```

`windowShowWallpaper` é a linha-chave — a tela inicial vai renderizar o papel de
parede do usuário atrás do nosso relógio, porque mantemos essa superfície
transparente no Compose.

### 3.3 Visibilidade de pacotes (Android 11+)

Desde o Android 11 (API 30), apps não podem ver livremente a lista de outros apps
instalados. Um launcher obviamente precisa — então declaramos *quais* apps nos
interessam com um bloco `<queries>`, em vez da permissão pesada
`QUERY_ALL_PACKAGES`:

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent>
</queries>
```

Isso diz: "deixe-me ver todos os apps que têm um ponto de entrada lançável".
Exatamente o que uma gaveta precisa, nada além disso.

---

## 4. Lendo os apps instalados

Agora a parte interessante — e é mais curta do que você imagina. O `PackageManager`
já conhece todos os apps instalados. Pedimos a ele os que têm um ponto de entrada
MAIN/LAUNCHER, mapeamos cada um para uma pequena data class e ordenamos
alfabeticamente.

`AppInfo.kt`:

```kotlin
data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
)
```

`AppRepository.kt` — este é basicamente todo o "backend" de um launcher:

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
                if (packageName == context.packageName) return@mapNotNull null // esconde a nós mesmos

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

Dois pontos que valem destacar:

- **`loadIcon` / `loadLabel` fazem I/O de disco**, então rodamos a consulta inteira
  em `Dispatchers.IO`. Nunca faça isso na main thread — é a causa nº 1 de um launcher
  travando.
- **Lançar** é só `getLaunchIntentForPackage()` + `FLAG_ACTIVITY_NEW_TASK`. A flag é
  obrigatória porque estamos iniciando a activity de fora de uma task de activity
  normal.

---

## 5. Guardando o estado

Um `ViewModel` minúsculo mantém a lista de apps entre rotações para não bater no
`PackageManager` a cada mudança de configuração. Expomos como um `StateFlow`.

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

Chamamos `refresh()` novamente no `MainActivity.onResume()`, para que a gaveta
reflita apps que foram instalados ou removidos enquanto estávamos fora.

---

## 6. A UI, em três composables

### 6.1 A Activity

Nada de especial — uma Activity, edge-to-edge, hospedando o Compose:

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

### 6.2 Tela inicial: um relógio e um gesto de deslizar pra cima

A home fica transparente (para o wallpaper aparecer), desenha um relógio grande e
detecta um arraste pra cima para abrir a gaveta:

```kotlin
@Composable
fun HomeScreen(onOpenDrawer: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -8f) onOpenDrawer() // dedo subindo
                }
            },
    ) {
        Clock(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 96.dp))
        // ...uma dica "Deslize para cima" na parte de baixo
    }
}
```

O relógio reemite a hora a cada segundo com `produceState`, que é a forma idiomática
no Compose de transformar um valor que "tica" em estado, sem um loop manual:

```kotlin
@Composable
private fun Clock(modifier: Modifier = Modifier) {
    val now by produceState(initialValue = Date()) {
        while (true) { value = Date(); delay(1_000L) }
    }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Text(text = timeFormat.format(now), fontSize = 72.sp, color = Color.White)
    // ...a linha da data abaixo
}
```

### 6.3 A gaveta de apps: uma grade pesquisável

A gaveta é uma superfície opaca em tela cheia com um campo de busca e um
`LazyVerticalGrid`. A filtragem é só uma derivação da query dentro de um `remember`:

```kotlin
@Composable
fun AppDrawer(apps: List<AppInfo>, onAppClick: (AppInfo) -> Unit, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).statusBarsPadding()) {
        OutlinedTextField(value = query, onValueChange = { query = it }, /* UI da busca */)

        LazyVerticalGrid(columns = GridCells.Fixed(4)) {
            items(filtered, key = { it.packageName }) { app ->
                AppGridItem(app = app, onClick = { onAppClick(app) })
            }
        }
    }
}
```

Cada célula desenha o ícone real do app. O ícone chega como um `Drawable` do Android,
então o rasterizamos em um `ImageBitmap` do Compose com o `toBitmap()` do `core-ktx`
— cacheado por app com `remember`, para acontecer só uma vez:

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

> Use `Image`, não `Icon`, para ícones de apps. O `Icon` aplica um tint e achataria
> todo ícone colorido em uma cor só.

### 6.4 Juntando home + gaveta

O `LauncherRoot` compõe os dois, desliza a gaveta pra cima com `AnimatedVisibility` e
faz o gesto de **Voltar** do sistema fechar a gaveta em vez de sair do launcher:

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

## 7. Rode e defina como padrão

1. Rode a configuração `app` num emulador ou dispositivo.
2. Aperte **Home**. O sistema pergunta qual launcher usar — escolha
   **Umain Launcher**.
3. Para fixar, escolha *Sempre*, ou configure em
   **Configurações → Apps → Apps padrão → App de tela inicial**.

Para voltar ao seu launcher normal, use essa mesma tela de configurações. (Você não
consegue "desinstalar para sair" se ele for o único launcher — mantenha sempre um
segundo.)

---

## 8. Por onde seguir

Você agora tem o esqueleto que todo launcher compartilha. Os de verdade adicionam,
mais ou menos em ordem de esforço:

- **Favoritos / uma dock** — persista alguns nomes de pacote com DataStore e renderize
  uma fileira na parte de baixo da home.
- **Desinstalar & info do app** — pressione e segure um item da grade para mostrar um
  menu de contexto (`Intent.ACTION_DELETE`, ou a tela de detalhes do app).
- **Atualização de ícones ao vivo** — registre um `BroadcastReceiver` para
  `ACTION_PACKAGE_ADDED` / `REMOVED` e chame `refresh()`, em vez de verificar no
  `onResume`.
- **Widgets** — o grande: o `AppWidgetHost` permite embutir widgets na tela inicial.
- **Badges de notificação & gestos** — toque duplo para bloquear, deslizar pra baixo
  para notificações (`StatusBarManager`), etc.

Mas o núcleo é exatamente o que você construiu aqui: um intent filter, uma consulta
ao `PackageManager` e uma superfície Compose sobre o papel de parede. Todo o resto é
polimento.

---

### A demo

O código-fonte completo — **Umain Launcher** — está no módulo `app/` ao lado deste
post. Abra no Android Studio, rode e aperte Home. Bom hacking! 🚀
