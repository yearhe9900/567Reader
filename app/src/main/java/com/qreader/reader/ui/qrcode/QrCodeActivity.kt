package com.qreader.reader.ui.qrcode

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.google.zxing.Result
import com.qreader.reader.R
import com.qreader.reader.base.BaseActivity
import com.qreader.reader.databinding.ActivityQrcodeCaptureBinding
import com.qreader.reader.ui.file.HandleFileContract
import com.qreader.reader.utils.QRCodeUtils
import com.qreader.reader.utils.readBytes
import com.qreader.reader.utils.viewbindingdelegate.viewBinding

class QrCodeActivity : BaseActivity<ActivityQrcodeCaptureBinding>(), ScanResultCallback {

    override val binding by viewBinding(ActivityQrcodeCaptureBinding::inflate)

    /**
     * 相机预览宿主的挂载点。Compose 化后不再由 XML 提供，改由本 Activity 创建
     * 并交给 [QrCodeScreen] 通过 AndroidView 承载
     * （`fragment-compose` 未引入，处理方式与 ConfigActivity 一致）。
     */
    private lateinit var fragmentHost: FrameLayout

    /** 顶栏标题。 */
    private var uiTitle by mutableStateOf("")

    private val selectQrImage = registerForActivityResult(HandleFileContract()) {
        it.uri?.readBytes(this)?.let { bytes ->
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            onScanResultCallback(QRCodeUtils.parseCodeResult(bitmap))
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        uiTitle = getString(R.string.scan_qr_code)

        fragmentHost = FrameLayout(this).apply {
            id = R.id.qrFragmentContainer
        }

        binding.composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.composeView.setContent {
            QrCodeScreen(
                title = uiTitle,
                fragmentHost = fragmentHost,
                onBack = { finish() },
                // 「从相册选择」原为菜单项（R.menu.qr_code_scan 的 action_choose_from_gallery），
                // Compose 顶栏没有菜单体系，改由顶栏右侧图标承担，行为完全一致。
                onPickFromGallery = {
                    selectQrImage.launch {
                        mode = HandleFileContract.IMAGE
                    }
                },
            )
        }

        val fTag = "qrCodeFragment"
        // 与 ConfigActivity 同理：Fragment 事务必须等 AndroidView 把 fragmentHost
        // 挂进视图树之后再提交，否则 FragmentManager 会因找不到容器 id 而抛
        // "No view found for id"。post 到下一个消息循环即可保证首帧已 attach。
        binding.composeView.post {
            val qrCodeFragment = supportFragmentManager.findFragmentByTag(fTag) as? QrCodeFragment
                ?: QrCodeFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.qrFragmentContainer, qrCodeFragment, fTag)
                .commit()
        }
    }

    override fun onScanResultCallback(result: Result?) {
        val intent = Intent()
        intent.putExtra("result", result?.text)
        setResult(RESULT_OK, intent)
        finish()
    }

}
