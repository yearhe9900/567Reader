package com.qreader.reader.model.rss

import com.qreader.reader.data.entities.RssArticle
import com.qreader.reader.data.entities.RssSource
import com.qreader.reader.help.coroutine.Coroutine
import com.qreader.reader.help.http.StrResponse
import com.qreader.reader.model.Debug
import com.qreader.reader.model.analyzeRule.AnalyzeRule
import com.qreader.reader.model.analyzeRule.AnalyzeRule.Companion.setCoroutineContext
import com.qreader.reader.model.analyzeRule.AnalyzeUrl
import com.qreader.reader.model.analyzeRule.RuleData
import com.qreader.reader.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.CoroutineContext

@Suppress("MemberVisibilityCanBePrivate")
object Rss {

    fun getArticles(
        scope: CoroutineScope,
        sortName: String,
        sortUrl: String,
        rssSource: RssSource,
        page: Int,
        key: String? = null,
        context: CoroutineContext = Dispatchers.IO
    ): Coroutine<Pair<MutableList<RssArticle>, String?>> {
        return Coroutine.async(scope, context) {
            getArticlesAwait(sortName, sortUrl, rssSource, page, key)
        }
    }

    suspend fun getArticlesAwait(
        sortName: String,
        sortUrl: String,
        rssSource: RssSource,
        page: Int,
        key: String? = null
    ): Pair<MutableList<RssArticle>, String?> {
        val ruleData = RuleData()
        val analyzeUrl = AnalyzeUrl(
            sortUrl,
            page = page,
            key = key,
            baseUrl = rssSource.sourceUrl,
            source = rssSource,
            ruleData = ruleData,
            coroutineContext = currentCoroutineContext(),
            hasLoginHeader = false
        )
        val checkJs = rssSource.loginCheckJs
        val res = kotlin.runCatching {
            analyzeUrl.getStrResponseAwait().let {
                if (!checkJs.isNullOrBlank()) { //检测源是否已登录
                    analyzeUrl.evalJS(checkJs, it) as StrResponse
                } else {
                    it
                }
            }
        }.getOrElse { throwable ->
            if (!checkJs.isNullOrBlank()) {
                val errResponse = analyzeUrl.getErrStrResponse(throwable)
                try {
                    (analyzeUrl.evalJS(checkJs, errResponse) as StrResponse).also {
                        if (it.code() == 500) {
                            throw throwable
                        }
                    }
                } catch (_: Throwable) {
                    throw throwable
                }
            } else {
                throw throwable
            }
        }
        checkRedirect(rssSource, res)
        Debug.log(rssSource.sourceUrl, "≡获取成功:${analyzeUrl.ruleUrl}")
        return RssParserByRule.parseXML(sortName, sortUrl, res.url, res.body, rssSource, ruleData)
    }

    fun getContent(
        scope: CoroutineScope,
        rssArticle: RssArticle,
        ruleContent: String,
        rssSource: RssSource,
        context: CoroutineContext = Dispatchers.IO
    ): Coroutine<String> {
        return Coroutine.async(scope, context) {
            getContentAwait(rssArticle, ruleContent, rssSource)
        }
    }

    suspend fun getContentAwait(
        rssArticle: RssArticle,
        ruleContent: String,
        rssSource: RssSource,
    ): String {
        val analyzeUrl = AnalyzeUrl(
            rssArticle.link,
            baseUrl = rssArticle.origin,
            source = rssSource,
            ruleData = rssArticle,
            coroutineContext = currentCoroutineContext(),
            hasLoginHeader = false
        )
        val checkJs = rssSource.loginCheckJs
        val res = kotlin.runCatching {
            analyzeUrl.getStrResponseAwait().let {
                if (!checkJs.isNullOrBlank()) { //检测源是否已登录
                    analyzeUrl.evalJS(checkJs, it) as StrResponse
                } else {
                    it
                }
            }
        }.getOrElse { throwable ->
            if (!checkJs.isNullOrBlank()) {
                val errResponse = analyzeUrl.getErrStrResponse(throwable)
                try {
                    (analyzeUrl.evalJS(checkJs, errResponse) as StrResponse).also {
                        if (it.code() == 500) {
                            throw throwable
                        }
                    }
                } catch (_: Throwable) {
                    throw throwable
                }
            } else {
                throw throwable
            }
        }
        checkRedirect(rssSource, res)
        Debug.log(rssSource.sourceUrl, "≡获取成功:${rssSource.sourceUrl}")
        Debug.log(rssSource.sourceUrl, res.body ?: "", state = 20)
        val analyzeRule = AnalyzeRule(rssArticle, rssSource)
        analyzeRule.setContent(res.body)
            .setBaseUrl(NetworkUtils.getAbsoluteURL(rssArticle.origin, rssArticle.link))
            .setCoroutineContext(currentCoroutineContext())
            .setRedirectUrl(res.url)
        return analyzeRule.getString(ruleContent)
    }

    /**
     * 检测重定向
     */
    private fun checkRedirect(rssSource: RssSource, response: StrResponse) {
        response.raw.priorResponse?.let {
            if (it.isRedirect) {
                Debug.log(rssSource.sourceUrl, "≡检测到重定向(${it.code})")
                Debug.log(rssSource.sourceUrl, "┌重定向后地址")
                Debug.log(rssSource.sourceUrl, "└${response.url}")
            }
        }
    }
}