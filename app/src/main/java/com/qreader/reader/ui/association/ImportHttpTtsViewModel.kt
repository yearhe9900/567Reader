package com.qreader.reader.ui.association

import android.app.Application
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import com.qreader.reader.R
import com.qreader.reader.base.BaseViewModel
import com.qreader.reader.constant.AppConst
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.HttpTTS
import com.qreader.reader.exception.NoStackTraceException
import com.qreader.reader.help.http.decompressed
import com.qreader.reader.help.http.newCallResponseBody
import com.qreader.reader.help.http.okHttpClient
import com.qreader.reader.help.http.text
import com.qreader.reader.utils.isAbsUrl
import com.qreader.reader.utils.isJsonArray
import com.qreader.reader.utils.isJsonObject
import com.qreader.reader.utils.isUri
import com.qreader.reader.utils.readText
import splitties.init.appCtx

class ImportHttpTtsViewModel(app: Application) : BaseViewModel(app) {

    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()

    val allSources = arrayListOf<HttpTTS>()
    val checkSources = arrayListOf<HttpTTS?>()
    val selectStatus = arrayListOf<Boolean>()

    val isSelectAll: Boolean
        get() {
            selectStatus.forEach {
                if (!it) {
                    return false
                }
            }
            return true
        }

    val selectCount: Int
        get() {
            var count = 0
            selectStatus.forEach {
                if (it) {
                    count++
                }
            }
            return count
        }

    fun importSelect(finally: () -> Unit) {
        execute {
            val selectSource = arrayListOf<HttpTTS>()
            selectStatus.forEachIndexed { index, b ->
                if (b) {
                    selectSource.add(allSources[index])
                }
            }
            appDb.httpTTSDao.insert(*selectSource.toTypedArray())
        }.onFinally {
            finally.invoke()
        }
    }

    fun importSource(text: String) {
        execute {
            importSourceAwait(text.trim())
        }.onError {
            errorLiveData.postValue("ImportError:${it.localizedMessage}")
        }.onSuccess {
            comparisonSource()
        }
    }

    private suspend fun importSourceAwait(text: String) {
        when {
            text.isJsonObject() -> {
                HttpTTS.fromJson(text).getOrThrow().let {
                    allSources.add(it)
                }
            }

            text.isJsonArray() -> HttpTTS.fromJsonArray(text).getOrThrow().let { items ->
                allSources.addAll(items)
            }

            text.isAbsUrl() -> {
                importSourceUrl(text)
            }

            text.isUri() -> {
                importSourceAwait(text.toUri().readText(appCtx))
            }

            else -> throw NoStackTraceException(context.getString(R.string.wrong_format))
        }
    }

    private suspend fun importSourceUrl(url: String) {
        okHttpClient.newCallResponseBody {
            if (url.endsWith("#requestWithoutUA")) {
                url(url.substringBeforeLast("#requestWithoutUA"))
                header(AppConst.UA_NAME, "null")
            } else {
                url(url)
            }
        }.decompressed().text().let {
            importSourceAwait(it)
        }
    }

    private fun comparisonSource() {
        execute {
            allSources.forEach {
                val source = appDb.httpTTSDao.get(it.id)
                checkSources.add(source)
                selectStatus.add(source == null || source.lastUpdateTime < it.lastUpdateTime)
            }
            successLiveData.postValue(allSources.size)
        }
    }

}