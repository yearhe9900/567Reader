package com.qreader.reader.help.source

import com.qreader.reader.constant.SourceType
import com.qreader.reader.data.entities.BaseSource
import com.qreader.reader.data.entities.BookSource
import com.qreader.reader.data.entities.RssSource
import com.qreader.reader.model.SharedJsScope
import org.mozilla.javascript.Scriptable
import kotlin.coroutines.CoroutineContext

fun BaseSource.getShareScope(coroutineContext: CoroutineContext? = null): Scriptable? {
    return SharedJsScope.getScope(jsLib, coroutineContext)
}

fun BaseSource.getSourceType(): Int {
    return when (this) {
        is BookSource -> SourceType.book
        is RssSource -> SourceType.rss
        else -> error("unknown source type: ${this::class.simpleName}.")
    }
}
