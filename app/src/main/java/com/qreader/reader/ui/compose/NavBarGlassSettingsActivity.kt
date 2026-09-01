package com.qreader.reader.ui.compose

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.qreader.reader.R
import com.qreader.reader.lib.theme.accentColor
import com.qreader.reader.ui.compose.liquid.LiquidBottomTab
import com.qreader.reader.ui.compose.liquid.LiquidBottomTabs
import com.qreader.reader.ui.compose.liquid.NavBarGlassConfig
import com.qreader.reader.utils.ColorUtils

/**
 * 底部导航栏玻璃态设置页
 *
 * 实时预览底部导航栏（LiquidBottomTabs），通过滑块调节玻璃参数：
 *  - 模糊半径（Blur radius）
 *  - 折射高度（Refraction height）
 *  - 折射强度（Refraction amount）
 *  - 色差（Chromatic aberration）
 * 与 AndroidLiquidGlass 官方 GlassPlayground 对齐（去掉对 Capsule 无效的 Corner radius）。
 * 点击「保存」后写入 [NavBarGlassConfig] 并返回主界面，主界面底部导航栏即时生效。
 */
class NavBarGlassSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NavBarGlassSettingsScreen()
        }
    }
}

@Composable
fun NavBarGlassSettingsScreen() {
    val context = LocalContext.current
    val initialConfig = remember { NavBarGlassConfig.load(context) }

    var blurRadius by remember { mutableFloatStateOf(initialConfig.blurRadiusDp) }
    var refractionHeight by remember { mutableFloatStateOf(initialConfig.refractionHeightDp) }
    var refractionAmount by remember { mutableFloatStateOf(initialConfig.refractionAmountDp) }
    var chromaticAberration by remember { mutableStateOf(initialConfig.chromaticAberration) }
    var previewTab by remember { mutableIntStateOf(0) }

    val isLightTheme = !isSystemInDarkTheme()
    val contentColor = if (isLightTheme) Color.Black else Color.White
    val accentColor = Color(context.accentColor)

    // 当前滑块状态实时映射为玻璃样式，用于预览
    val previewConfig = NavBarGlassConfig(
        blurRadiusDp = blurRadius,
        refractionHeightDp = refractionHeight,
        refractionAmountDp = refractionAmount,
        chromaticAberration = chromaticAberration,
    )

    val backdrop = rememberLayerBackdrop()

    Box(modifier = Modifier.fillMaxSize()) {
        // 壁纸背景：作为玻璃预览的 backdrop 捕获源（与官方 demo 一致）
        Image(
            painter = painterResource(id = R.drawable.wallpaper_light),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏：返回 + 标题
            TopBar(
                title = "底部导航栏",
                contentColor = contentColor,
                onBack = { (context as? ComponentActivity)?.finish() }
            )

            // 预览区：底部居中展示当前玻璃态导航栏
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LiquidBottomTabs(
                    selectedTabIndex = { previewTab },
                    onTabSelected = { previewTab = it },
                    backdrop = backdrop,
                    tabsCount = 3,
                    accentColor = accentColor,
                    containerColor = previewConfig.containerColor(isLightTheme),
                    isLightTheme = isLightTheme,
                    glassStyle = previewConfig.toLiquidGlassStyle(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    NavBarPreviewTab(0, previewTab, "书架", R.drawable.ic_bottom_books_e, R.drawable.ic_bottom_books_s, accentColor, contentColor) { previewTab = it }
                    NavBarPreviewTab(1, previewTab, "发现", R.drawable.ic_bottom_explore_e, R.drawable.ic_bottom_explore_s, accentColor, contentColor) { previewTab = it }
                    NavBarPreviewTab(2, previewTab, "设置", R.drawable.ic_bottom_person_e, R.drawable.ic_bottom_person_s, accentColor, contentColor) { previewTab = it }
                }
            }

            // 设置面板：半透明圆角卡片承载滑块/开关/按钮
            SettingsPanel(
                blurRadius = blurRadius,
                onBlurRadiusChange = { blurRadius = it },
                refractionHeight = refractionHeight,
                onRefractionHeightChange = { refractionHeight = it },
                refractionAmount = refractionAmount,
                onRefractionAmountChange = { refractionAmount = it },
                chromaticAberration = chromaticAberration,
                onChromaticAberrationChange = { chromaticAberration = it },
                contentColor = contentColor,
                accentColor = accentColor,
                onReset = {
                    blurRadius = 8f
                    refractionHeight = 24f
                    refractionAmount = 24f
                    chromaticAberration = true
                },
                onSave = {
                    NavBarGlassConfig.save(
                        context,
                        NavBarGlassConfig(
                            blurRadiusDp = blurRadius,
                            refractionHeightDp = refractionHeight,
                            refractionAmountDp = refractionAmount,
                            chromaticAberration = chromaticAberration,
                        )
                    )
                    Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                    (context as? ComponentActivity)?.finish()
                }
            )
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    contentColor: Color,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            text = "‹ 返回",
            style = TextStyle(color = contentColor, fontSize = 16.sp),
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onBack() }
                .padding(8.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        BasicText(
            text = title,
            style = TextStyle(color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun RowScope.NavBarPreviewTab(
    position: Int,
    selected: Int,
    label: String,
    iconEmpty: Int,
    iconSelected: Int,
    accentColor: Color,
    contentColor: Color,
    onClick: (Int) -> Unit,
) {
    val selectedNow = position == selected
    val tint = if (selectedNow) accentColor else contentColor
    LiquidBottomTab(onClick = { onClick(position) }) {
        Image(
            painter = painterResource(id = if (selectedNow) iconSelected else iconEmpty),
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(tint)
        )
        BasicText(
            text = label,
            style = TextStyle(color = tint, fontSize = 12.sp)
        )
    }
}

@Composable
private fun SettingsPanel(
    blurRadius: Float,
    onBlurRadiusChange: (Float) -> Unit,
    refractionHeight: Float,
    onRefractionHeightChange: (Float) -> Unit,
    refractionAmount: Float,
    onRefractionAmountChange: (Float) -> Unit,
    chromaticAberration: Boolean,
    onChromaticAberrationChange: (Boolean) -> Unit,
    contentColor: Color,
    accentColor: Color,
    onReset: () -> Unit,
    onSave: () -> Unit,
) {
    val isLightTheme = !isSystemInDarkTheme()
    val panelColor = if (isLightTheme) Color(0xF2FFFFFF) else Color(0xF21C1C1E)
    val sliderColors = SliderDefaults.colors(
        thumbColor = accentColor,
        activeTrackColor = accentColor,
        inactiveTrackColor = contentColor.copy(0.2f),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp)
            .background(panelColor, RoundedCornerShape(28.dp))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        SliderRow(
            title = "模糊半径",
            valueText = "${blurRadius.toInt()} dp",
            contentColor = contentColor,
        ) {
            Slider(
                value = blurRadius,
                onValueChange = onBlurRadiusChange,
                valueRange = 0f..32f,
                colors = sliderColors,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SliderRow(
            title = "折射高度",
            valueText = "${refractionHeight.toInt()} dp",
            contentColor = contentColor,
        ) {
            Slider(
                value = refractionHeight,
                onValueChange = onRefractionHeightChange,
                valueRange = 0f..64f,
                colors = sliderColors,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SliderRow(
            title = "折射强度",
            valueText = "${refractionAmount.toInt()} dp",
            contentColor = contentColor,
        ) {
            Slider(
                value = refractionAmount,
                onValueChange = onRefractionAmountChange,
                valueRange = 0f..64f,
                colors = sliderColors,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 色差开关
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = "色差效果",
                    style = TextStyle(color = contentColor, fontSize = 16.sp)
                )
                BasicText(
                    text = "镜头色散，开启后选中滑块边缘出现彩虹色晕",
                    style = TextStyle(color = contentColor.copy(0.55f), fontSize = 13.sp)
                )
            }
            Switch(
                checked = chromaticAberration,
                onCheckedChange = onChromaticAberrationChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF0088FF),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = contentColor.copy(0.25f)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PanelButton(
                text = "恢复默认",
                contentColor = contentColor,
                accentColor = accentColor,
                isPrimary = false,
                modifier = Modifier.weight(1f),
                onClick = onReset
            )
            PanelButton(
                text = "保存",
                contentColor = contentColor,
                accentColor = accentColor,
                isPrimary = true,
                modifier = Modifier.weight(1f),
                onClick = onSave
            )
        }
    }
}

@Composable
private fun SliderRow(
    title: String,
    valueText: String,
    contentColor: Color,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = title,
                style = TextStyle(color = contentColor, fontSize = 16.sp),
                modifier = Modifier.weight(1f)
            )
            BasicText(
                text = valueText,
                style = TextStyle(color = contentColor.copy(0.55f), fontSize = 14.sp)
            )
        }
        content()
    }
}

@Composable
private fun PanelButton(
    text: String,
    contentColor: Color,
    accentColor: Color,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bgColor = if (isPrimary) accentColor else contentColor.copy(0.1f)
    val textColor = if (isPrimary) {
        if (ColorUtils.isColorLight(accentColor.toArgb())) Color.Black else Color.White
    } else {
        contentColor
    }
    Box(
        modifier = modifier
            .height(48.dp)
            .background(bgColor, RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = TextStyle(color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        )
    }
}
