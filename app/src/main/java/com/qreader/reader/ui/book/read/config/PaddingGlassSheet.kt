package com.qreader.reader.ui.book.read.config

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.qreader.reader.R
import com.qreader.reader.constant.EventBus
import com.qreader.reader.help.config.ReadBookConfig
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.compose.glass.GlassSliderRow
import com.qreader.reader.ui.compose.glass.GlassToggleRow
import com.qreader.reader.ui.compose.glass.LiquidGlassDialog
import com.qreader.reader.utils.ColorUtils
import com.qreader.reader.utils.postEvent

/**
 * 阅读边距玻璃面板（替代 PaddingConfigDialog）。
 */
@Composable
fun PaddingGlassSheet(
    backdrop: Backdrop,
    onDismiss: () -> Unit,
) {
    val contentColor = GlassConfig.contentColor(
        ColorUtils.isColorLight(ReadBookConfig.bgMeanColor)
    )
    val accent = GlassConfig.toggleAccentColor(
        ColorUtils.isColorLight(ReadBookConfig.bgMeanColor)
    )

    var headerTop by remember { mutableIntStateOf(ReadBookConfig.headerPaddingTop) }
    var headerBottom by remember { mutableIntStateOf(ReadBookConfig.headerPaddingBottom) }
    var headerLeft by remember { mutableIntStateOf(ReadBookConfig.headerPaddingLeft) }
    var headerRight by remember { mutableIntStateOf(ReadBookConfig.headerPaddingRight) }
    var bodyTop by remember { mutableIntStateOf(ReadBookConfig.paddingTop) }
    var bodyBottom by remember { mutableIntStateOf(ReadBookConfig.paddingBottom) }
    var bodyLeft by remember { mutableIntStateOf(ReadBookConfig.paddingLeft) }
    var bodyRight by remember { mutableIntStateOf(ReadBookConfig.paddingRight) }
    var footerTop by remember { mutableIntStateOf(ReadBookConfig.footerPaddingTop) }
    var footerBottom by remember { mutableIntStateOf(ReadBookConfig.footerPaddingBottom) }
    var footerLeft by remember { mutableIntStateOf(ReadBookConfig.footerPaddingLeft) }
    var footerRight by remember { mutableIntStateOf(ReadBookConfig.footerPaddingRight) }
    var showHeaderLine by remember { mutableStateOf(ReadBookConfig.showHeaderLine) }
    var showFooterLine by remember { mutableStateOf(ReadBookConfig.showFooterLine) }

    LiquidGlassDialog(
        backdrop = backdrop,
        onDismiss = {
            ReadBookConfig.save()
            onDismiss()
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
            .navigationBarsPadding(),
        cardRadius = GlassConfig.sheetCornerRadius,
        alignment = Alignment.BottomCenter,
        showScrim = false,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
        ) {
            BasicText(
                text = stringResource(R.string.header),
                style = TextStyle(contentColor, 16.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            GlassToggleRow(
                stringResource(R.string.showLine),
                showHeaderLine,
                contentColor,
                accent,
            ) {
                showHeaderLine = it
                ReadBookConfig.showHeaderLine = it
                postEvent(EventBus.UP_CONFIG, arrayListOf(2))
            }
            GlassSliderRow(stringResource(R.string.padding_top), headerTop, 100, contentColor, backdrop) {
                headerTop = it
                ReadBookConfig.headerPaddingTop = it
                postEvent(EventBus.UP_CONFIG, arrayListOf(2))
            }
            GlassSliderRow(stringResource(R.string.padding_bottom), headerBottom, 100, contentColor, backdrop) {
                headerBottom = it
                ReadBookConfig.headerPaddingBottom = it
                postEvent(EventBus.UP_CONFIG, arrayListOf(2))
            }
            GlassSliderRow(stringResource(R.string.padding_left), headerLeft, 100, contentColor, backdrop) {
                headerLeft = it
                ReadBookConfig.headerPaddingLeft = it
                postEvent(EventBus.UP_CONFIG, arrayListOf(2))
            }
            GlassSliderRow(stringResource(R.string.padding_right), headerRight, 100, contentColor, backdrop) {
                headerRight = it
                ReadBookConfig.headerPaddingRight = it
                postEvent(EventBus.UP_CONFIG, arrayListOf(2))
            }

            BasicText(
                text = stringResource(R.string.content),
                style = TextStyle(contentColor, 16.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            GlassSliderRow(stringResource(R.string.padding_top), bodyTop, 100, contentColor, backdrop) {
                bodyTop = it
                ReadBookConfig.paddingTop = it
                postEvent(EventBus.UP_CONFIG, arrayListOf(10, 5))
            }
            GlassSliderRow(stringResource(R.string.padding_bottom), bodyBottom, 100, contentColor, backdrop) {
                bodyBottom = it
                ReadBookConfig.paddingBottom = it
                postEvent(EventBus.UP_CONFIG, arrayListOf(10, 5))
            }
            GlassSliderRow(stringResource(R.string.padding_left), bodyLeft, 100, contentColor, backdrop) {
                bodyLeft = it
                ReadBookConfig.paddingLeft = it
                postEvent(EventBus.UP_CONFIG, arrayListOf(10, 5))
            }
            GlassSliderRow(stringResource(R.string.padding_right), bodyRight, 100, contentColor, backdrop) {
                bodyRight = it
                ReadBookConfig.paddingRight = it
                postEvent(EventBus.UP_CONFIG, arrayListOf(10, 5))
            }

            BasicText(
                text = stringResource(R.string.footer),
                style = TextStyle(contentColor, 16.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            GlassToggleRow(
                stringResource(R.string.showLine),
                showFooterLine,
                contentColor,
                accent,
            ) {
                showFooterLine = it
                ReadBookConfig.showFooterLine = it
                postEvent(EventBus.UP_CONFIG, arrayListOf(2))
            }
            GlassSliderRow(stringResource(R.string.padding_top), footerTop, 100, contentColor, backdrop) {
                footerTop = it
                ReadBookConfig.footerPaddingTop = it
                postEvent(EventBus.UP_CONFIG, arrayListOf(2))
            }
            GlassSliderRow(stringResource(R.string.padding_bottom), footerBottom, 100, contentColor, backdrop) {
                footerBottom = it
                ReadBookConfig.footerPaddingBottom = it
                postEvent(EventBus.UP_CONFIG, arrayListOf(2))
            }
            GlassSliderRow(stringResource(R.string.padding_left), footerLeft, 100, contentColor, backdrop) {
                footerLeft = it
                ReadBookConfig.footerPaddingLeft = it
                postEvent(EventBus.UP_CONFIG, arrayListOf(2))
            }
            GlassSliderRow(stringResource(R.string.padding_right), footerRight, 100, contentColor, backdrop) {
                footerRight = it
                ReadBookConfig.footerPaddingRight = it
                postEvent(EventBus.UP_CONFIG, arrayListOf(2))
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { ReadBookConfig.save() }
    }
}
