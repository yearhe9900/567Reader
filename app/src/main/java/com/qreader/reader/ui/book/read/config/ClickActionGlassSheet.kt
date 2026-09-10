package com.qreader.reader.ui.book.read.config

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.qreader.reader.R
import com.qreader.reader.constant.PreferKey
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.help.config.ReadBookConfig
import com.qreader.reader.lib.dialogs.selector
import com.qreader.reader.ui.compose.glass.GlassConfig
import com.qreader.reader.ui.compose.glass.LiquidGlassDialog
import com.qreader.reader.utils.ColorUtils
import com.qreader.reader.utils.putPrefInt

/**
 * 点击区域设置玻璃全屏浮层（替代 ClickActionConfigDialog）。
 *
 * 3×3 区域点选动作；关闭时 [AppConfig.detectClickArea]。
 */
@Composable
fun ClickActionGlassSheet(
    backdrop: Backdrop,
    onDismiss: () -> Unit,
) {
    val activity = LocalContext.current as? android.app.Activity ?: return
    val contentColor = GlassConfig.contentColor(
        ColorUtils.isColorLight(ReadBookConfig.bgMeanColor)
    )
    val actions = remember {
        linkedMapOf(
            -1 to activity.getString(R.string.non_action),
            0 to activity.getString(R.string.menu),
            1 to activity.getString(R.string.next_page),
            2 to activity.getString(R.string.prev_page),
            3 to activity.getString(R.string.next_chapter),
            4 to activity.getString(R.string.previous_chapter),
            5 to activity.getString(R.string.read_aloud_prev_paragraph),
            6 to activity.getString(R.string.read_aloud_next_paragraph),
            7 to activity.getString(R.string.bookmark_add),
            8 to activity.getString(R.string.edit_content),
            9 to activity.getString(R.string.replace_state_change),
            10 to activity.getString(R.string.chapter_list),
            11 to activity.getString(R.string.search_content),
            12 to activity.getString(R.string.sync_book_progress_t),
            13 to activity.getString(R.string.read_aloud_pause_resume),
        )
    }

    var tl by remember { mutableStateOf(actions[AppConfig.clickActionTL]) }
    var tc by remember { mutableStateOf(actions[AppConfig.clickActionTC]) }
    var tr by remember { mutableStateOf(actions[AppConfig.clickActionTR]) }
    var ml by remember { mutableStateOf(actions[AppConfig.clickActionML]) }
    var mc by remember { mutableStateOf(actions[AppConfig.clickActionMC]) }
    var mr by remember { mutableStateOf(actions[AppConfig.clickActionMR]) }
    var bl by remember { mutableStateOf(actions[AppConfig.clickActionBL]) }
    var bc by remember { mutableStateOf(actions[AppConfig.clickActionBC]) }
    var br by remember { mutableStateOf(actions[AppConfig.clickActionBR]) }

    fun pick(preferKey: String, onSet: (String?) -> Unit) {
        activity.selector(
            activity.getString(R.string.select_action),
            actions.values.toList(),
        ) { _, index ->
            val action = actions.keys.toList()[index]
            activity.putPrefInt(preferKey, action)
            onSet(actions[action])
        }
    }

    DisposableEffect(Unit) {
        onDispose { AppConfig.detectClickArea() }
    }

    LiquidGlassDialog(
        backdrop = backdrop,
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(vertical = 24.dp),
        cardRadius = GlassConfig.dialogCardRadius,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        alignment = Alignment.Center,
        showScrim = false,
    ) { colors ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(
                text = stringResource(R.string.click_regional_config),
                style = TextStyle(colors.contentColor, 18.sp),
            )
            Spacer(Modifier.height(16.dp))

            val cells = listOf(
                Triple(tl, PreferKey.clickActionTL) { v: String? -> tl = v },
                Triple(tc, PreferKey.clickActionTC) { v: String? -> tc = v },
                Triple(tr, PreferKey.clickActionTR) { v: String? -> tr = v },
                Triple(ml, PreferKey.clickActionML) { v: String? -> ml = v },
                Triple(mc, PreferKey.clickActionMC) { v: String? -> mc = v },
                Triple(mr, PreferKey.clickActionMR) { v: String? -> mr = v },
                Triple(bl, PreferKey.clickActionBL) { v: String? -> bl = v },
                Triple(bc, PreferKey.clickActionBC) { v: String? -> bc = v },
                Triple(br, PreferKey.clickActionBR) { v: String? -> br = v },
            )
            cells.chunked(3).forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { (label, key, set) ->
                        ZoneCell(
                            label = label ?: "",
                            contentColor = colors.contentColor,
                            containerColor = colors.containerColor,
                            onClick = { pick(key, set) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close_with__shadow),
                    contentDescription = null,
                    tint = colors.contentColor,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.containerColor.copy(alpha = 0.3f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss,
                        )
                        .padding(10.dp),
                )
            }
        }
    }
}

@Composable
private fun ZoneCell(
    label: String,
    contentColor: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor.copy(alpha = 0.35f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = TextStyle(contentColor, 13.sp, textAlign = TextAlign.Center),
            modifier = Modifier.padding(4.dp),
        )
    }
}
