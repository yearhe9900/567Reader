package com.qreader.reader.ui.book.manga.config

import androidx.annotation.Keep
import com.qreader.reader.ui.widget.ReaderInfoBarView

@Keep
data class MangaFooterConfig(
    var hideChapterLabel: Boolean = false,
    var hideChapter: Boolean = false,
    var hidePageNumberLabel: Boolean = false,
    var hidePageNumber: Boolean = false,
    var hideProgressRatioLabel: Boolean = false,
    var hideProgressRatio: Boolean = false,
    var footerOrientation: Int = ReaderInfoBarView.ALIGN_LEFT,//默认靠左
    var hideFooter: Boolean = false,
    var hideChapterName:Boolean=false,
)