# 玻璃弹框视觉令牌化（GlassDialogTokens）

## 问题
- 项目存在常量类 `constant/AppConst.kt`，但里面是 App 级常量，不适合放 UI 玻璃令牌。
- 玻璃弹框的视觉参数在 3 处重复硬编码同一套深色固定值（白字 / `#0091FF` / `#121212·0.4` / `#121212·0.56` / `Black·0.5`）与同一套 `blur/lens/colorControls` 数值：`GlassAlertDialog`、`ExplorePage` 删除框、`ThemeModeDialogOverlay`。

## 方案
新建 `app/src/main/java/com/qreader/reader/ui/compose/glass/GlassDialogTokens.kt`：

```kotlin
object GlassDialogTokens {
    // 深色固定配色
    val contentColor   = Color.White
    val accentColor    = Color(0xFF0091FF)
    val containerColor = Color(0xFF121212).copy(0.4f)
    val dimColor       = Color(0xFF121212).copy(0.56f)
    val scrimColor     = Color.Black.copy(0.5f)

    // scrim / card / widget 三档玻璃效果参数（Dp + Float）
    const val scrimBrightness = 0f; const val scrimSaturation = 1.4f
    val scrimBlur = 12.dp; val scrimLensX = 28.dp; val scrimLensY = 56.dp
    const val cardBrightness  = 0f; const val cardSaturation  = 1.5f
    val cardBlur  = 8.dp;   val cardLensX  = 24.dp; val cardLensY  = 48.dp
    val widgetBlur = 8.dp;  val widgetLensX = 16.dp; val widgetLensY = 24.dp
}

data class GlassDialogColors(...)
fun glassDialogColors(isEInkMode: Boolean): GlassDialogColors  // 解析 E-Ink 黑白回退
```

## 复用的 3 处生产弹框
- `GlassAlertDialog.kt`（书籍信息页删除书籍）
- `ExplorePage.kt` 删除书源框（补了 E-Ink 跳过 glass）
- `MySettingsScreen.kt` `ThemeModeDialogOverlay`

调用处统一改为：
```kotlin
val colors = glassDialogColors(AppConfig.isEInkMode)
val contentColor = colors.contentColor /* ... */
// effects 内：blur(GlassDialogTokens.cardBlur.toPx()) 等
```

## 未改动（性质不同，仅记录）
- `LiquidDialog.kt`：仅 GlassDemoActivity 用，按系统明暗分支，深色分支值恰等于令牌。
- `LiquidBottomTabs.kt`：底部导航栏，按明暗主题切换。
- `GlassDialog.kt` / `GlassCard.kt`：已无调用点，疑似死代码。

## 提交
`88e0e08` refactor(dialog): 抽出玻璃弹框视觉令牌 GlassDialogTokens 供三处复用

## 验证
```bash
./gradlew :app:assembleAppDebug
```
