package com.qreader.reader.ui.book.read.config

import android.app.Application
import android.speech.tts.TextToSpeech
import com.qreader.reader.base.BaseViewModel
import com.qreader.reader.help.DefaultData

class SpeakEngineViewModel(application: Application) : BaseViewModel(application) {

    val sysEngines: List<TextToSpeech.EngineInfo> by lazy {
        val tts = TextToSpeech(context, null)
        val engines = tts.engines
        tts.shutdown()
        engines
    }

    fun importDefault() {
        execute {
            DefaultData.importDefaultHttpTTS()
        }
    }

}