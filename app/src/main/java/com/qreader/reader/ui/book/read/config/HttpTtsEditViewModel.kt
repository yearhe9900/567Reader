package com.qreader.reader.ui.book.read.config

import android.app.Application
import android.os.Bundle
import com.qreader.reader.base.BaseViewModel
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.HttpTTS
import com.qreader.reader.exception.NoStackTraceException
import com.qreader.reader.help.ConcurrentRateLimiter.Companion.concurrentRecordMap
import com.qreader.reader.model.ReadAloud
import com.qreader.reader.utils.getClipText
import com.qreader.reader.utils.isJsonArray
import com.qreader.reader.utils.isJsonObject
import com.qreader.reader.utils.toastOnUi

class HttpTtsEditViewModel(app: Application) : BaseViewModel(app) {

    var id: Long? = null
    var httpTTS: HttpTTS? = null

    fun initData(arguments: Bundle?, success: (httpTTS: HttpTTS) -> Unit) {
        execute {
            if (id == null) {
                val argumentId = arguments?.getLong("id")
                if (argumentId != null && argumentId != 0L) {
                    id = argumentId
                    val source = appDb.httpTTSDao.get(argumentId)
                    httpTTS = source
                    return@execute source
                }
            }
            return@execute null
        }.onSuccess {
            it?.let {
                success.invoke(it)
            }
        }
    }

    fun save(httpTTS: HttpTTS, success: (() -> Unit)? = null) {
        id = httpTTS.id
        this.httpTTS = httpTTS
        execute {
            appDb.httpTTSDao.insert(httpTTS)
            concurrentRecordMap.remove(httpTTS.getKey()) //删除并发限制缓存
            if (ReadAloud.ttsEngine == httpTTS.id.toString()) ReadAloud.upReadAloudClass()
        }.onSuccess {
            success?.invoke()
        }
    }

    fun importFromClip(onSuccess: (httpTTS: HttpTTS) -> Unit) {
        val text = context.getClipText()
        if (text.isNullOrBlank()) {
            context.toastOnUi("剪贴板为空")
        } else {
            importSource(text, onSuccess)
        }
    }

    fun importSource(text: String, onSuccess: (httpTTS: HttpTTS) -> Unit) {
        val text1 = text.trim()
        execute {
            when {
                text1.isJsonObject() -> {
                    HttpTTS.fromJson(text1).getOrThrow()
                }
                text1.isJsonArray() -> {
                    HttpTTS.fromJsonArray(text1).getOrThrow().first()
                }
                else -> {
                    throw NoStackTraceException("格式不对")
                }
            }
        }.onSuccess {
            onSuccess.invoke(it)
        }.onError {
            context.toastOnUi(it.localizedMessage)
        }
    }

}