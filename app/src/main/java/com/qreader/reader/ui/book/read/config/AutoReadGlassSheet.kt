package com.qreader.reader.ui.book.read.config

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.qreader.reader.R
import com.qreader.reader.help.config.ReadBookConfig
import com.qreader.reader.model.ReadAloud
import com.qreader.reader.model.ReadBook
import com.qreader.reader.service.BaseReadAloudService
import com.qreader.reader.ui.book.read.BaseReadBookActivity
import com.qreader.reader.ui.book.read.ReadBookActivity
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.compose.glass.LiquidGlassDialog
import com.qreader.reader.ui.compose.liquid.LiquidSlider
import com.qreader.reader.utils.ColorUtils
import java.util.Locale

/**
 * 自动翻页玻璃面板（替代 AutoReadDialog / dialog_auto_read）。
 *
 * 速度调节 + 目录 / 主菜单 / 停止 / 翻页动画设置。
 */
@Composable
fun AutoReadGlassSheet(
    backdrop: Backdrop,
    onDismiss: () -> Unit,
) {
    val activity = LocalContext.current as? ReadBookActivity ?: return
    val contentColor = GlassConfig.contentColor(
        ColorUtils.isColorLight(ReadBookConfig.bgMeanColor)
    )
    var speed by remember {
        mutableIntStateOf(
            ReadBookConfig.autoReadSpeed.let { if (it < 1) 1 else it }
        )
    }

    LiquidGlassDialog(
        backdrop = backdrop,
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = GlassConfig.sheetHorizontalPadding),
        cardRadius = GlassConfig.sheetCornerRadius,
        alignment = Alignment.BottomCenter,
        showScrim = false,
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = stringResource(R.string.auto_page_speed),
                    style = TextStyle(contentColor, 14.sp),
                )
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f).height(40.dp), contentAlignment = Alignment.Center) {
                    LiquidSlider(
                        value = { speed.toFloat() },
                        onValueChange = { v ->
                            speed = v.toInt().coerceAtLeast(1)
                        },
                        valueRange = 1f..30f,
                        visibilityThreshold = 1f,
                        backdrop = backdrop,
                        onValueChangeFinished = {
                            val s = speed.coerceAtLeast(1)
                            ReadBookConfig.autoReadSpeed = s
                            ReadAloud.upTtsSpeechRate(activity)
                            if (!BaseReadAloudService.pause) {
                                ReadAloud.pause(activity)
                                ReadAloud.resume(activity)
                            }
                        },
                    )
                }
                Spacer(Modifier.width(8.dp))
                BasicText(
                    text = String.format(Locale.ROOT, "%ds", speed),
                    style = TextStyle(contentColor, 14.sp),
                    modifier = Modifier.width(36.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                SheetAction(
                    icon = R.drawable.ic_toc,
                    label = stringResource(R.string.chapter_list),
                    tint = contentColor,
                ) {
                    onDismiss()
                    activity.openChapterList()
                }
                SheetAction(
                    icon = R.drawable.ic_more,
                    label = stringResource(R.string.main_menu),
                    tint = contentColor,
                ) {
                    onDismiss()
                    activity.showMenuBar()
                }
                SheetAction(
                    icon = R.drawable.ic_auto_page_stop,
                    label = stringResource(R.string.stop),
                    tint = contentColor,
                ) {
                    onDismiss()
                    activity.autoPageStop()
                }
                SheetAction(
                    icon = R.drawable.ic_settings,
                    label = stringResource(R.string.setting),
                    tint = contentColor,
                ) {
                    (activity as BaseReadBookActivity).showPageAnimConfig {
                        activity.upPageAnim()
                        ReadBook.loadContent(false)
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetAction(
    icon: Int,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(4.dp))
        BasicText(text = label, style = TextStyle(tint, 12.sp))
    }
}
