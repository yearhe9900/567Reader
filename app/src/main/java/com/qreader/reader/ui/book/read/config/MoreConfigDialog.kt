package com.qreader.reader.ui.book.read.config

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.preference.Preference
import com.qreader.reader.R
import com.qreader.reader.base.BasePrefDialogFragment
import com.qreader.reader.constant.EventBus
import com.qreader.reader.constant.PreferKey
import com.qreader.reader.help.config.ReadBookConfig
import com.qreader.reader.lib.prefs.fragment.PreferenceFragment
import com.qreader.reader.lib.theme.bottomBackground
import com.qreader.reader.lib.theme.primaryColor
import com.qreader.reader.model.ReadBook
import com.qreader.reader.ui.book.read.ReadBookActivity
import com.qreader.reader.ui.book.read.page.provider.ChapterProvider
import com.qreader.reader.utils.canvasrecorder.CanvasRecorderFactory
import com.qreader.reader.utils.dpToPx
import com.qreader.reader.utils.getPrefBoolean
import com.qreader.reader.utils.postEvent
import com.qreader.reader.utils.removePref
import com.qreader.reader.utils.setEdgeEffectColor

class MoreConfigDialog : BasePrefDialogFragment() {
    private val readPreferTag = "readPreferenceFragment"

    override fun onStart() {
        super.onStart()
        dialog?.window?.run {
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setBackgroundDrawableResource(R.color.background)
            decorView.setPadding(0, 0, 0, 0)
            val attr = attributes
            attr.dimAmount = 0.0f
            attr.gravity = Gravity.BOTTOM
            attributes = attr
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, 360.dpToPx())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as ReadBookActivity).bottomDialog++
        val view = LinearLayout(context)
        view.setBackgroundColor(requireContext().bottomBackground)
        view.id = R.id.tag1
        container?.addView(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var preferenceFragment = childFragmentManager.findFragmentByTag(readPreferTag)
        if (preferenceFragment == null) preferenceFragment = ReadPreferenceFragment()
        childFragmentManager.beginTransaction()
            .replace(view.id, preferenceFragment, readPreferTag)
            .commit()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (activity as ReadBookActivity).bottomDialog--
    }

    class ReadPreferenceFragment : PreferenceFragment(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        @SuppressLint("RestrictedApi")
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_config_read)
            if (!CanvasRecorderFactory.isSupport) {
                removePref(PreferKey.optimizeRender)
                preferenceScreen.removePreferenceRecursively(PreferKey.optimizeRender)
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            listView.setEdgeEffectColor(primaryColor)
        }

        override fun onResume() {
            super.onResume()
            preferenceManager
                .sharedPreferences
                ?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            preferenceManager
                .sharedPreferences
                ?.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences?,
            key: String?
        ) {
            when (key) {
                PreferKey.textSelectAble -> postEvent(key, getPrefBoolean(key))

                PreferKey.showBrightnessView -> {
                    postEvent(PreferKey.showBrightnessView, "")
                }

                PreferKey.doublePageHorizontal -> {
                    ChapterProvider.upLayout()
                    ReadBook.loadContent(false)
                }

                PreferKey.progressBarBehavior -> {
                    postEvent(EventBus.UP_SEEK_BAR, true)
                }

                PreferKey.optimizeRender -> {
                    ChapterProvider.upStyle()
                    ReadBook.callBack?.upPageAnim(true)
                    ReadBook.loadContent(false)
                }
            }
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            when (preference.key) {
                "customPageKey" -> PageKeyDialog(requireContext()).show()
                "clickRegionalConfig" -> {
                    (activity as? ReadBookActivity)?.showClickRegionalConfig()
                }
            }
            return super.onPreferenceTreeClick(preference)
        }
    }
}