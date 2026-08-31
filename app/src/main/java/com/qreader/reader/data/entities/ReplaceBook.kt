package com.qreader.reader.data.entities

import com.qreader.reader.constant.BookType

data class ReplaceBook(
    val bookUrl: String = "",
    val origin: String = "",
    val originName: String = "",
    val type: Int = BookType.text,
    val name: String = "",
    val author: String = "",
    val kind: String? = null,
    val coverUrl: String? = null,
    val intro: String? = null,
    val wordCount: String? = null,
    val latestChapterTitle: String? = null,
    val tocUrl: String = "",
    val originOrder: Int = 0
    )