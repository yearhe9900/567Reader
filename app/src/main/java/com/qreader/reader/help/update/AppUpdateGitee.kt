package com.qreader.reader.help.update

import androidx.annotation.Keep
import com.qreader.reader.constant.AppConst
import com.qreader.reader.exception.NoStackTraceException
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.help.coroutine.Coroutine
import com.qreader.reader.help.http.newCallResponse
import com.qreader.reader.help.http.okHttpClient
import com.qreader.reader.help.http.text
import com.qreader.reader.utils.GSON
import com.qreader.reader.utils.fromJsonArray
import com.qreader.reader.utils.fromJsonObject
import kotlinx.coroutines.CoroutineScope

@Keep
@Suppress("unused")
object AppUpdateGitee : AppUpdate.AppUpdateInterface {

    private val checkVariant: AppVariant
        get() = when (AppConfig.updateToVariant) {
            "official_version" -> AppVariant.OFFICIAL
            "beta_release_version" -> AppVariant.BETA_RELEASE
            "beta_releaseA_version" -> AppVariant.BETA_RELEASEA
            "beta_releaseS_version" -> AppVariant.BETA_RELEASES
            else -> AppConst.appInfo.appVariant
        }

    private suspend fun getLatestRelease(): List<AppReleaseInfo> {
        val lastReleaseUrl = if (checkVariant.isBeta()) {
            "https://gitee.com/api/v5/repos/lyc486/legado/releases/latest"
        } else {
            "https://gitee.com/api/v5/repos/lyc486/legado/releases?page=1&per_page=3&direction=desc"
        }
        val res = okHttpClient.newCallResponse {
            url(lastReleaseUrl)
        }
        if (!res.isSuccessful) {
            throw NoStackTraceException("获取新版本出错(${res.code})")
        }
        val body = res.body.text()
        if (body.isBlank()) {
            throw NoStackTraceException("获取新版本出错")
        }
        if (!checkVariant.isBeta()) {
            return GSON.fromJsonArray<GiteeRelease>(body)
                .getOrElse {
                    throw NoStackTraceException("获取新版本出错 " + it.localizedMessage)
                }
                .first { !it.prerelease }
                .gitReleaseToAppReleaseInfo()
                .sortedByDescending { it.createdAt }
        }
        return GSON.fromJsonObject<GiteeRelease>(body)
            .getOrElse {
                throw NoStackTraceException("获取新版本出错 " + it.localizedMessage)
            }
            .gitReleaseToAppReleaseInfo()
            .sortedByDescending { it.createdAt }
    }

    override fun check(
        scope: CoroutineScope,
    ): Coroutine<AppUpdate.UpdateInfo> {
        return Coroutine.async(scope) {
            getLatestRelease()
                .filter {
                    if (AppConst.appInfo.appVariant == AppVariant.BETA_RELEASE) { //不切版本
                        it.appVariant == AppConst.appInfo.appVariant
                    } else {
                        it.appVariant == checkVariant
                    }
                }
                .firstOrNull { it.versionName > AppConst.appInfo.versionName }
                ?.let {
                    return@async AppUpdate.UpdateInfo(
                        it.versionName,
                        it.note,
                        it.downloadUrl,
                        it.name
                    )
                }
            throw NoStackTraceException("已是最新版本")
        }.timeout(10000)
    }
}
