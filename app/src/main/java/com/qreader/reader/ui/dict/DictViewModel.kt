package com.qreader.reader.ui.dict

import android.app.Application
import com.qreader.reader.base.BaseViewModel
import com.qreader.reader.constant.AppLog
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.DictRule
import com.qreader.reader.help.coroutine.Coroutine

class DictViewModel(application: Application) : BaseViewModel(application) {

    private var dictJob: Coroutine<String>? = null

    fun initData(onSuccess: (List<DictRule>) -> Unit) {
        execute {
            appDb.dictRuleDao.enabled
        }.onSuccess {
            onSuccess.invoke(it)
        }
    }

    fun dict(
        dictRule: DictRule,
        word: String,
        onFinally: (String) -> Unit
    ) {
        dictJob?.cancel()
        dictJob = execute {
            dictRule.search(word)
        }.onSuccess {
            onFinally.invoke(it)
        }.onError {
            onFinally.invoke(it.localizedMessage ?: "ERROR")
        }
    }

    fun onButtonClick(
        dictRule: DictRule,
        name: String,
        click: String
    ) {
        if (click.isBlank()) {
            return
        }
        execute {
            dictRule.buttonClick(name, click)
        }.onError {
            AppLog.put("$name click error\n${it.localizedMessage}", it)
        }
    }

}