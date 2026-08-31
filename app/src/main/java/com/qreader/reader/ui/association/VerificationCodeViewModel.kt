package com.qreader.reader.ui.association

import android.app.Application
import android.os.Bundle
import com.qreader.reader.base.BaseViewModel
import com.qreader.reader.constant.SourceType
import com.qreader.reader.help.source.SourceHelp

class VerificationCodeViewModel(app: Application): BaseViewModel(app) {

    var sourceOrigin = ""
    var sourceName = ""
    private var sourceType = SourceType.book

    fun initData(arguments: Bundle) {
        sourceName = arguments.getString("sourceName") ?: ""
        sourceOrigin = arguments.getString("sourceOrigin") ?: ""
        sourceType = arguments.getInt("sourceType", SourceType.book)
    }

    fun disableSource(block: () -> Unit) {
        execute {
            SourceHelp.enableSource(sourceOrigin, sourceType, false)
        }.onSuccess {
            block.invoke()
        }
    }

    fun deleteSource(block: () -> Unit) {
        execute {
            SourceHelp.deleteSource(sourceOrigin, sourceType)
        }.onSuccess {
            block.invoke()
        }
    }

}
