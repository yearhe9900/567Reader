package com.qreader.reader.help.update

import com.qreader.reader.help.coroutine.Coroutine
import kotlinx.coroutines.CoroutineScope

object AppUpdate {

    val gitHubUpdate: AppUpdateInterface? by lazy {
        AppUpdateGitHub
    }
    val giteeUpdate: AppUpdateInterface by lazy {
        AppUpdateGitee
    }


    data class UpdateInfo(
        val tagName: String,
        val updateLog: String,
        val downloadUrl: String,
        val fileName: String
    )

    interface AppUpdateInterface {

        fun check(scope: CoroutineScope): Coroutine<UpdateInfo>

    }

}
