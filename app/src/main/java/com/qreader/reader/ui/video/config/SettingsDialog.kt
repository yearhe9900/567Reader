package com.qreader.reader.ui.video.config

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import com.qreader.reader.R
import com.qreader.reader.base.BaseDialogFragment
import com.qreader.reader.databinding.DialogVideoSettingsBinding
import com.qreader.reader.model.VideoPlay
import com.qreader.reader.ui.widget.number.NumberPickerDialog
import com.qreader.reader.utils.viewbindingdelegate.viewBinding

class SettingsDialog(private val context: Context, private val callBack: CallBack? = null) :
    BaseDialogFragment(R.layout.dialog_video_settings) {
    private val binding by viewBinding(DialogVideoSettingsBinding::bind)

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initData()
        initView()
    }

    @SuppressLint("SetTextI18n")
    private fun initData() {
        binding.run {
            tvPressSpeed.text = (VideoPlay.longPressSpeed / 10.0f).toPressSpeedStr()
            cbAutoPlay.isChecked = VideoPlay.autoPlay
            cbStartFull.isChecked = VideoPlay.startFull
            cbFullBottomProgress.isChecked = VideoPlay.fullBottomProgressBar
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        binding.run {
            cbAutoPlay.setOnCheckedChangeListener { _, isChecked ->
                VideoPlay.autoPlay = isChecked
                ctStartFull.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
            cbStartFull.setOnCheckedChangeListener { _, isChecked ->
                VideoPlay.startFull = isChecked
            }
            cbFullBottomProgress.setOnCheckedChangeListener { _, isChecked ->
                VideoPlay.fullBottomProgressBar = isChecked
            }
            tvPressSpeed.setOnClickListener { _ ->
                NumberPickerDialog(requireContext(), true)
                    .setTitle(getString(R.string.press_speed))
                    .setMaxValue(60)
                    .setMinValue(5)
                    .setValue(VideoPlay.longPressSpeed)
                    .setCustomButton((R.string.btn_default_s)) {
                        VideoPlay.longPressSpeed = 30
                        tvPressSpeed.text = 3.0f.toPressSpeedStr()
                    }
                    .show {
                        VideoPlay.longPressSpeed = it
                        tvPressSpeed.text = (it / 10.0f).toPressSpeedStr()
                    }
            }
        }
    }

    private fun Float.toPressSpeedStr(): String {
        return context.getString(R.string.press_speed_summary, this)
    }
    interface CallBack {
//        fun upUi()
    }

}