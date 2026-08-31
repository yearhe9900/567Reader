package com.qreader.reader.help.source

import com.script.rhino.runScriptWithContext
import com.qreader.reader.constant.BookSourceType
import com.qreader.reader.constant.BookType
import com.qreader.reader.data.entities.BookSource
import com.qreader.reader.data.entities.BookSourcePart
import com.qreader.reader.data.entities.rule.ExploreKind
import com.qreader.reader.ui.main.explore.ExploreAdapter.Companion.exploreInfoMapList
import com.qreader.reader.utils.ACache
import com.qreader.reader.utils.GSON
import com.qreader.reader.utils.InfoMap
import com.qreader.reader.utils.MD5Utils
import com.qreader.reader.utils.fromJsonArray
import com.qreader.reader.utils.isJsonArray
import com.qreader.reader.utils.printOnDebug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * 采用md5作为key可以在分类修改后自动重新计算,不需要手动刷新
 */

private val mutexMap by lazy { hashMapOf<String, Mutex>() }
private val exploreKindsMap by lazy { ConcurrentHashMap<String, List<ExploreKind>>() }
private val aCache by lazy { ACache.get("explore") }

private fun BookSource.getExploreKindsKey(): String {
    return MD5Utils.md5Encode(bookSourceUrl + exploreUrl)
}

private fun BookSourcePart.getExploreKindsKey(): String {
    return getBookSource()!!.getExploreKindsKey()
}

suspend fun BookSourcePart.exploreKinds(): List<ExploreKind> {
    return getBookSource()!!.exploreKinds()
}

suspend fun BookSource.exploreKinds(): List<ExploreKind> {
    val exploreKindsKey = getExploreKindsKey()
    exploreKindsMap[exploreKindsKey]?.let { return it }
    val exploreUrl = exploreUrl
    if (exploreUrl.isNullOrBlank()) {
        return emptyList()
    }
    val mutex = mutexMap[bookSourceUrl] ?: Mutex().apply { mutexMap[bookSourceUrl] = this }
    mutex.withLock {
        exploreKindsMap[exploreKindsKey]?.let { return it }
        val kinds = arrayListOf<ExploreKind>()
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                val ruleStr = when {
                    exploreUrl.startsWith("@js:", true) -> {
                        aCache.getAsString(exploreKindsKey)?.takeIf { it.isNotBlank() } ?: run {
                            val exploreInfoMap = exploreInfoMapList[bookSourceUrl] ?: InfoMap(bookSourceUrl).also {
                                exploreInfoMapList.put(bookSourceUrl, it)
                            }
                            runScriptWithContext {
                                evalJS(exploreUrl.substring(4)) {
                                    put("infoMap", exploreInfoMap)
                                }.toString().trim()
                            }.also {
                                aCache.put(exploreKindsKey, it)
                            }
                        }
                    }
                    exploreUrl.startsWith("<js>", true) -> {
                        aCache.getAsString(exploreKindsKey)?.takeIf { it.isNotBlank() } ?: run {
                            val exploreInfoMap = exploreInfoMapList[bookSourceUrl] ?: InfoMap(bookSourceUrl).also {
                                exploreInfoMapList.put(bookSourceUrl, it)
                            }
                            runScriptWithContext {
                                evalJS(exploreUrl.substring(4, exploreUrl.lastIndexOf("<"))) {
                                    put("infoMap", exploreInfoMap)
                                }.toString().trim()
                            }.also {
                                aCache.put(exploreKindsKey, it)
                            }
                        }
                    }
                    else -> exploreUrl
                }
                if (ruleStr.isJsonArray()) {
                    GSON.fromJsonArray<ExploreKind>(ruleStr).getOrThrow().let {
                        kinds.addAll(it)
                    }
                } else {
                    ruleStr.split("(&&|\n)+".toRegex()).forEach { kindStr ->
                        val kindCfg = kindStr.split("::")
                        kinds.add(ExploreKind(kindCfg.first(), kindCfg.getOrNull(1)))
                    }
                }
            }.onFailure {
                kinds.add(ExploreKind("ERROR:${it.localizedMessage}", it.stackTraceToString()))
                it.printOnDebug()
            }
        }
        exploreKindsMap[exploreKindsKey] = kinds
        return kinds
    }
}

suspend fun BookSourcePart.clearExploreKindsCache() {
    withContext(Dispatchers.IO) {
        val exploreKindsKey = getExploreKindsKey()
        aCache.remove(exploreKindsKey)
        exploreKindsMap.remove(exploreKindsKey)
    }
}

suspend fun BookSource.clearExploreKindsCache() {
    withContext(Dispatchers.IO) {
        val exploreKindsKey = getExploreKindsKey()
        aCache.remove(exploreKindsKey)
        exploreKindsMap.remove(exploreKindsKey)
    }
}

fun BookSource.exploreKindsJson(): String {
    val exploreKindsKey = getExploreKindsKey()
    return aCache.getAsString(exploreKindsKey)?.takeIf { it.isJsonArray() }
        ?: exploreUrl.takeIf { it.isJsonArray() }
        ?: ""
}

fun BookSource.getBookType(): Int {
    return when (bookSourceType) {
        BookSourceType.file -> BookType.text or BookType.webFile
        BookSourceType.image -> BookType.image
        BookSourceType.audio -> BookType.audio
        BookSourceType.video -> BookType.video
        else -> BookType.text
    }
}
