package com.qreader.reader.ui.book.read.config

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.qreader.reader.R
import com.qreader.reader.constant.EventBus
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.help.config.ReadBookConfig
import com.qreader.reader.lib.dialogs.selector
import com.qreader.reader.model.ReadAloud
import com.qreader.reader.model.ReadBook
import com.qreader.reader.service.BaseReadAloudService
import com.qreader.reader.ui.book.read.ReadBookActivity
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.compose.glass.GlassSliderRow
import com.qreader.reader.ui.compose.glass.GlassToggleRow
import com.qreader.reader.ui.compose.glass.LiquidGlassDialog
import com.qreader.reader.utils.ColorUtils
import com.qreader.reader.utils.observeEvent
import com.qreader.reader.utils.toastOnUi

/**
 * 朗读主控制玻璃面板（替代 ReadAloudDialog / dialog_read_aloud）。
 *
 * 上/下章 · 播控 · 定时 · 语速跟随系统 · 目录/主菜单/后台/设置。
 */
@Composable
fun ReadAloudGlassSheet(
    backdrop: Backdrop,
    onDismiss: () -> Unit,
) {
    val activity = LocalContext.current as? ReadBookActivity ?: return
    val isLight = ColorUtils.isColorLight(ReadBookConfig.bgMeanColor)
    val contentColor = GlassConfig.contentColor(isLight)
    val accent = GlassConfig.toggleAccentColor(isLight)

    var playing by remember { mutableStateOf(!BaseReadAloudService.pause) }
    var ttsFollowSys by remember { mutableStateOf(AppConfig.ttsFlowSys) }
    var ttsRate by remember { mutableIntStateOf(AppConfig.ttsSpeechRate) }
    var timerMinute by remember {
        mutableIntStateOf(
            if (BaseReadAloudService.timeMinute > 0) BaseReadAloudService.timeMinute
            else AppConfig.ttsTimer
        )
    }

    DisposableEffect(activity) {
        activity.observeEvent<Int>(EventBus.ALOUD_STATE) {
            playing = !BaseReadAloudService.pause
        }
        activity.observeEvent<Int>(EventBus.READ_ALOUD_DS) {
            timerMinute = it
        }
        onDispose { }
    }

    LiquidGlassDialog(
        backdrop = backdrop,
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
            .navigationBarsPadding(),
        cardRadius = GlassConfig.sheetCornerRadius,
        alignment = Alignment.BottomCenter,
        showScrim = false,
    ) { colors ->
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
        ) {
            // 上一章 / 下一章
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ActionChip(stringResource(R.string.previous_chapter), colors.contentColor) {
                    ReadBook.moveToPrevChapter(upContent = true, toLast = false)
                }
                ActionChip(stringResource(R.string.next_chapter), colors.contentColor) {
                    ReadBook.moveToNextChapter(true)
                }
            }

            Spacer(Modifier.height(8.dp))

            // 播控
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBtn(R.drawable.ic_skip_previous, colors.contentColor) {
                    ReadAloud.prevParagraph(activity)
                }
                IconBtn(
                    if (playing) R.drawable.ic_pause_24dp else R.drawable.ic_play_24dp,
                    accent,
                ) {
                    activity.onClickReadAloud()
                }
                IconBtn(R.drawable.ic_skip_next, colors.contentColor) {
                    ReadAloud.nextParagraph(activity)
                }
                IconBtn(R.drawable.ic_stop_black_24dp, colors.contentColor) {
                    ReadAloud.stop(activity)
                    onDismiss()
                }
            }

            Spacer(Modifier.height(8.dp))

            // 定时
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBtn(R.drawable.ic_timer_black_24dp, colors.contentColor) {
                    AppConfig.ttsTimer = timerMinute
                    activity.toastOnUi("保存设定时间成功！")
                }
                Spacer(Modifier.width(8.dp))
                BasicText(
                    text = stringResource(R.string.timer_m, timerMinute),
                    style = TextStyle(colors.contentColor, 14.sp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            val times = intArrayOf(0, 5, 10, 15, 30, 60, 90, 180)
                            activity.selector("设定时间", times.map { "$it 分钟" }) { _, i ->
                                ReadAloud.setTimer(activity, times[i])
                                timerMinute = times[i]
                            }
                        }
                        .padding(vertical = 8.dp),
                )
            }
            GlassSliderRow("定时", timerMinute, 180, colors.contentColor, backdrop) {
                timerMinute = it
            }

            // 语速
            GlassToggleRow("语速跟随系统", ttsFollowSys, colors.contentColor, accent) {
                ttsFollowSys = it
                AppConfig.ttsFlowSys = it
                upTtsSpeechRate(activity)
            }
            if (!ttsFollowSys) {
                GlassSliderRow("语速", ttsRate, 100, colors.contentColor, backdrop, display = { ((it + 5) / 10f).toString() }) {
                    ttsRate = it
                    AppConfig.ttsSpeechRate = it
                    upTtsSpeechRate(activity)
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                SheetAction(R.drawable.ic_toc, stringResource(R.string.chapter_list), colors.contentColor) {
                    onDismiss()
                    activity.openChapterList()
                }
                SheetAction(R.drawable.ic_more, stringResource(R.string.main_menu), colors.contentColor) {
                    onDismiss()
                    activity.showMenuBar()
                }
                SheetAction(R.drawable.ic_settings, stringResource(R.string.setting), colors.contentColor) {
                    onDismiss()
                    activity.showReadAloudConfig()
                }
            }
        }
    }
}

private fun upTtsSpeechRate(context: android.content.Context) {
    ReadAloud.upTtsSpeechRate(context)
    if (!BaseReadAloudService.pause) {
        ReadAloud.pause(context)
        ReadAloud.resume(context)
    }
}

@Composable
private fun ActionChip(label: String, contentColor: Color, onClick: () -> Unit) {
    BasicText(
        text = label,
        style = TextStyle(contentColor, 14.sp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun IconBtn(icon: Int, tint: Color, onClick: () -> Unit) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = tint,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(8.dp),
    )
}

@Composable
private fun SheetAction(icon: Int, label: String, tint: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(4.dp))
        BasicText(text = label, style = TextStyle(tint, 12.sp, textAlign = TextAlign.Center))
    }
}
