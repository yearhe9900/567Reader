package com.qreader.reader.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.qreader.reader.R
import com.qreader.reader.constant.AppConst.appInfo
import com.qreader.reader.constant.AppLog
import com.qreader.reader.help.CrashHandler
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.help.coroutine.Coroutine
import com.qreader.reader.ui.widget.dialog.TextDialog
import com.qreader.reader.utils.FileDoc
import com.qreader.reader.utils.compress.ZipUtils
import com.qreader.reader.utils.createFileIfNotExist
import com.qreader.reader.utils.createFolderIfNotExist
import com.qreader.reader.utils.delete
import com.qreader.reader.utils.externalCache
import com.qreader.reader.utils.find
import com.qreader.reader.utils.list
import com.qreader.reader.utils.openInputStream
import com.qreader.reader.utils.openOutputStream
import com.qreader.reader.utils.openUrl
import com.qreader.reader.utils.sendMail
import com.qreader.reader.utils.sendToClip
import com.qreader.reader.utils.showDialogFragment
import com.qreader.reader.utils.toastOnUi
import kotlinx.coroutines.delay
import splitties.init.appCtx
import java.io.File

class AboutFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.about)
        findPreference<Preference>("update_log")?.summary =
            "${getString(R.string.version)} ${appInfo.versionName}"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.overScrollMode = View.OVER_SCROLL_NEVER
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            "contributors" -> openUrl(R.string.contributors_url)
            "update_log" -> showMdFile(getString(R.string.update_log), "updateLog.md")
            "mail" -> requireContext().sendMail(getString(R.string.email))
            "license" -> showMdFile(getString(R.string.license), "LICENSE.md")
            "disclaimer" -> showMdFile(getString(R.string.disclaimer), "disclaimer.md")
            "privacyPolicy" -> showMdFile(getString(R.string.privacy_policy), "privacyPolicy.md")
            "gzGzh" -> requireContext().sendToClip(getString(R.string.legado_gzh))
        }
        return super.onPreferenceTreeClick(preference)
    }

    @Suppress("SameParameterValue")
    private fun openUrl(@StringRes addressID: Int) {
        requireContext().openUrl(getString(addressID))
    }

    /**
     * 显示md文件
     */
    private fun showMdFile(title: String, fileName: String) {
        val mdText = String(requireContext().assets.open(fileName).readBytes())
        showDialogFragment(TextDialog(title, mdText, TextDialog.Mode.MD))
    }

    /**
     * 加入qq群
     */
    private fun joinQQGroup(key: String): Boolean {
        val intent = Intent()
        intent.data =
            Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26k%3D$key")
        // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面
        // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        kotlin.runCatching {
            startActivity(intent)
            return true
        }.onFailure {
            toastOnUi("添加失败,请手动添加")
        }
        return false
    }

}