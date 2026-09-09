package com.qreader.reader.ui.welcome

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qreader.reader.R
import com.qreader.reader.lib.theme.accentColor
import com.qreader.reader.lib.theme.backgroundColor
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.compose.glass.GlassConfig.pseudoGlass
import com.qreader.reader.utils.BitmapUtils

/**
 * 欢迎页（Compose 玻璃态版）。
 *
 * 使用 [pseudoGlass] 而非 drawBackdrop：启动页停留极短，
 * 嵌套 backdrop 捕获曾导致主线程卡死无法进入主界面。
 */
@Composable
fun WelcomeScreen(
    showText: Boolean = true,
    showIcon: Boolean = true,
    backgroundPath: String? = null,
) {
    val context = LocalContext.current
    val accent = Color(context.accentColor)
    val bgColor = Color(context.backgroundColor)
    val isLight = GlassConfig.isLightTheme(context)
    val contentColor = GlassConfig.contentColor(isLight)

    GlassConfig.SyncStatusBarToGlassTheme(isLightTheme = isLight)

    val customBitmap = remember(backgroundPath) { decodeWelcomeBitmap(backgroundPath) }

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val enterAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "welcomeAlpha",
    )
    val enterScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.97f,
        animationSpec = tween(durationMillis = 320),
        label = "welcomeScale",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // 背景：自定义封面或氛围底
        if (customBitmap != null) {
            Image(
                bitmap = customBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (isLight) 0.12f else 0.28f)),
            )
        } else {
            AmbientBackdrop(
                bgColor = bgColor,
                accent = accent,
                isLight = isLight,
            )
        }

        // 中央品牌卡（伪玻璃，不走 backdrop 捕获）
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .alpha(enterAlpha)
                    .scale(enterScale)
                    .widthIn(max = 320.dp)
                    .fillMaxWidth()
                    .pseudoGlass(isLight, RoundedCornerShape(36.dp))
                    .padding(horizontal = 28.dp, vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (showIcon) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .background(
                                    if (isLight) {
                                        Color.White.copy(alpha = 0.45f)
                                    } else {
                                        Color.White.copy(alpha = 0.14f)
                                    },
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(R.drawable.icon_read_book),
                                contentDescription = stringResource(R.string.welcome),
                                colorFilter = ColorFilter.tint(accent),
                                modifier = Modifier.size(48.dp),
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                    }

                    if (showText) {
                        Text(
                            text = stringResource(R.string.app_name),
                            color = contentColor,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "享受美好时光",
                            color = contentColor.copy(alpha = 0.72f),
                            fontSize = 15.sp,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // 底部标语胶囊
        if (showText) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(bottom = 36.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .alpha(enterAlpha)
                        .pseudoGlass(isLight, RoundedCornerShape(50))
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = "品读万千故事",
                        color = contentColor.copy(alpha = 0.88f),
                        fontSize = 13.sp,
                        letterSpacing = 3.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun AmbientBackdrop(
    bgColor: Color,
    accent: Color,
    isLight: Boolean,
) {
    val mid = if (isLight) GlassConfig.dialogBackdropLightMid else GlassConfig.dialogBackdropDarkMid
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        mid.copy(alpha = 0.45f),
                        Color.Transparent,
                        accent.copy(alpha = 0.10f),
                    ),
                ),
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.16f),
                        Color.Transparent,
                    ),
                    radius = 640f,
                ),
            ),
    )
}

private fun decodeWelcomeBitmap(path: String?): android.graphics.Bitmap? {
    if (path.isNullOrBlank() || path.endsWith(".9.png")) return null
    return runCatching {
        BitmapUtils.decodeBitmap(path)
    }.getOrNull()
}
