package com.qreader.reader.model

import com.qreader.reader.constant.AppConst
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.BookSource
import com.qreader.reader.data.entities.ReplaceRule
import com.qreader.reader.data.entities.RssSource
import com.qreader.reader.data.entities.RuleSub
import com.qreader.reader.exception.NoStackTraceException
import com.qreader.reader.help.book.ContentProcessor
import com.qreader.reader.help.http.decompressed
import com.qreader.reader.help.http.newCallResponseBody
import com.qreader.reader.help.http.okHttpClient
import com.qreader.reader.help.source.SourceHelp
import com.qreader.reader.utils.GSON
import com.qreader.reader.utils.fromJsonArray
import java.util.concurrent.ConcurrentHashMap

object RuleUpdate {
    val cacheBookSourceMap = ConcurrentHashMap<String, List<BookSource>>()
    val cacheRssSourceMap = ConcurrentHashMap<String, List<RssSource>>()
    val cacheReplaceRuleMap = ConcurrentHashMap<String, List<ReplaceRule>>()

    suspend fun cacheSource(ruleSub: RuleSub): Boolean {
        val url = ruleSub.url
        val type = ruleSub.type
        val silentUpdate = ruleSub.silentUpdate
        val update = ruleSub.update
        val updateInterval = ruleSub.updateInterval
        if (update + updateInterval * 3600 * 1000L > System.currentTimeMillis()) {
            return false
        } else {
            ruleSub.update = System.currentTimeMillis()
            appDb.ruleSubDao.update(ruleSub)
        }
        var upRules = false
        okHttpClient.newCallResponseBody {
            if (url.endsWith("#requestWithoutUA")) {
                url(url.substringBeforeLast("#requestWithoutUA"))
                header(AppConst.UA_NAME, "null")
            } else {
                url(url)
            }
        }.decompressed().byteStream().use {
            when (type) {
                0 -> GSON.fromJsonArray<BookSource>(it).getOrThrow().let { lists ->
                    val source = lists.firstOrNull() ?: return@let
                    if (source.bookSourceUrl.isEmpty()) {
                        throw NoStackTraceException("不是书源")
                    }
                    lists.forEach { list ->
                        val localSource = appDb.bookSourceDao.getBookSourcePart(list.bookSourceUrl)
                        if (localSource == null || localSource.lastUpdateTime < list.lastUpdateTime) {
                            if (silentUpdate) {
                                if (localSource != null) {
                                    list.bookSourceGroup = localSource.bookSourceGroup
                                }
                                SourceHelp.insertBookSource(list)
                                upRules = true
                            }
                            else {
                                cacheBookSourceMap[url] = lists
                                return true
                            }
                        }
                    }
                }
                1 -> GSON.fromJsonArray<RssSource>(it).getOrThrow().let { lists ->
                    val source = lists.firstOrNull() ?: return@let
                    if (source.sourceUrl.isEmpty()) {
                        throw NoStackTraceException("不是订阅源")
                    }
                    lists.forEach { list ->
                        val localSource = appDb.rssSourceDao.getByKey(list.sourceUrl)
                        if (localSource == null || localSource.lastUpdateTime < list.lastUpdateTime) {
                            if (silentUpdate) {
                                if (localSource != null) {
                                    list.sourceGroup = localSource.sourceGroup
                                }
                                SourceHelp.insertRssSource(list)
                            }
                            else {
                                cacheRssSourceMap[url] = lists
                                return true
                            }
                        }
                    }
                }
                2 -> GSON.fromJsonArray<ReplaceRule>(it).getOrThrow().let { lists ->
                    lists.forEach { list ->
                        val oldRule = appDb.replaceRuleDao.findById(list.id)
                        if (oldRule == null || list.pattern != oldRule.pattern || list.replacement != oldRule.replacement) {
                            if (silentUpdate) {
                                appDb.replaceRuleDao.insert(list)
                                upRules = true
                            }
                            else {
                                cacheReplaceRuleMap[url] = lists
                                return true
                            }
                        }
                    }
                }
            }
            if (upRules) {
                ContentProcessor.upReplaceRules()
            }
        }
        return false
    }
}