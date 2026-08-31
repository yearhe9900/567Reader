@file:Suppress("unused")

package com.qreader.reader.help.book

import com.qreader.reader.data.entities.BookChapter
import com.qreader.reader.help.RuleBigDataHelp.getDanmakuFile

fun BookChapter.getDanmaku(): Any? { //读取弹幕数据
    return variableMap["danmaku"] ?: getDanmakuFile(bookUrl, url)
}