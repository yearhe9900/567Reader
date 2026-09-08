package com.qreader.reader.ui.book.info.edit

import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.qreader.reader.base.VMBaseActivity
import com.qreader.reader.constant.BookType
import com.qreader.reader.data.entities.Book
import com.qreader.reader.databinding.ActivityBookInfoEditBinding
import com.qreader.reader.help.book.BookHelp
import com.qreader.reader.help.book.addType
import com.qreader.reader.help.book.isAudio
import com.qreader.reader.help.book.isImage
import com.qreader.reader.help.book.isLocal
import com.qreader.reader.help.book.isVideo
import com.qreader.reader.help.book.removeType
import com.qreader.reader.ui.book.changecover.ChangeCoverDialog
import com.qreader.reader.ui.file.HandleFileContract
import com.qreader.reader.utils.FileUtils
import com.qreader.reader.utils.MD5Utils
import com.qreader.reader.utils.externalFiles
import com.qreader.reader.utils.inputStream
import com.qreader.reader.utils.readUri
import com.qreader.reader.utils.showDialogFragment
import com.qreader.reader.utils.toastOnUi
import com.qreader.reader.utils.viewbindingdelegate.viewBinding
import splitties.init.appCtx
import java.io.FileOutputStream

class BookInfoEditActivity :
    VMBaseActivity<ActivityBookInfoEditBinding, BookInfoEditViewModel>(),
    ChangeCoverDialog.CallBack {

    private val selectCover = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            coverChangeTo(uri)
        }
    }

    override val binding by viewBinding(ActivityBookInfoEditBinding::inflate)
    override val viewModel by viewModels<BookInfoEditViewModel>()

    // ── 表单字段（Compose 渲染需要可观察状态；沿用 BookInfoActivity 的 mutableStateOf 模式）──
    private var uiName by mutableStateOf("")
    private var uiAuthor by mutableStateOf("")
    private var uiTypePos by mutableStateOf(0)
    private var uiCoverUrl by mutableStateOf("")
    private var uiIntro by mutableStateOf("")

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.bookData.observe(this) { upView(it) }
        if (viewModel.bookData.value == null) {
            intent.getStringExtra("bookUrl")?.let {
                viewModel.loadBook(it)
            }
        }
        // 设置 Compose 内容（layout 仅含 ComposeView）
        binding.composeView.setContent {
            BookInfoEditScreen(
                book = viewModel.book,
                name = uiName,
                onNameChange = { uiName = it },
                author = uiAuthor,
                onAuthorChange = { uiAuthor = it },
                typePos = uiTypePos,
                onTypePosChange = { uiTypePos = it },
                coverUrl = uiCoverUrl,
                onCoverUrlChange = { uiCoverUrl = it },
                intro = uiIntro,
                onIntroChange = { uiIntro = it },
                onBack = { finish() },
                onSave = { saveData() },
                onChangeCover = {
                    viewModel.book?.let {
                        showDialogFragment(ChangeCoverDialog(it.name, it.author))
                    }
                },
                onSelectCover = {
                    selectCover.launch {
                        mode = HandleFileContract.IMAGE
                    }
                },
                onRefreshCover = {
                    // 预览已实时跟随 uiCoverUrl；此处把当前输入回写到 book.customCoverUrl，
                    // 与原 tvRefreshCover「按 URL 刷新封面」的语义保持一致。
                    viewModel.book?.customCoverUrl = uiCoverUrl
                },
            )
        }
    }

    private fun upView(book: Book) {
        uiName = book.name
        uiAuthor = book.author
        uiTypePos = when {
            book.isVideo -> 4
            book.isImage -> 2
            book.isAudio -> 1
            else -> 0
        }
        uiCoverUrl = book.getDisplayCover()
        uiIntro = book.getDisplayIntro()
    }

    private fun saveData() {
        val book = viewModel.book ?: return
        val oldBook = book.copy()
        book.name = uiName
        book.author = uiAuthor
        val local = if (book.isLocal) BookType.local else 0
        val bookType = when (uiTypePos) {
            4 -> BookType.video or local
            2 -> BookType.image or local
            1 -> BookType.audio or local
            else -> BookType.text or local
        }
        book.removeType(BookType.video, BookType.local, BookType.image, BookType.audio, BookType.text)
        book.addType(bookType)
        val customCoverUrl = uiCoverUrl
        book.customCoverUrl = if (customCoverUrl == book.coverUrl) null else customCoverUrl
        val customIntro = uiIntro
        book.customIntro = if (customIntro == book.intro) null else customIntro
        BookHelp.updateCacheFolder(oldBook, book)
        viewModel.saveBook(book) {
            setResult(RESULT_OK)
            finish()
        }
    }

    /** ChangeCoverDialog 回调 + selectCover 复制完成后的统一入口。 */
    override fun coverChangeTo(coverUrl: String) {
        uiCoverUrl = coverUrl
        viewModel.book?.customCoverUrl = coverUrl
    }

    /** 选择本地图片后，把 Uri 复制到 app externalFiles/covers 下，再走 String 入口回填。 */
    private fun coverChangeTo(uri: Uri) {
        if (uri.scheme?.lowercase() in listOf("http", "https")) {
            coverChangeTo(uri.toString())
            return
        }
        readUri(uri) { fileDoc, inputStream ->
            runCatching {
                inputStream.use {
                    var file = this.externalFiles
                    val suffix = if (fileDoc.name.contains(".9.png", true)) {
                        ".9.png"
                    } else {
                        "." + fileDoc.name.substringAfterLast(".")
                    }
                    val fileName = uri.inputStream(this).getOrThrow().use {
                        MD5Utils.md5Encode(it) + suffix
                    }
                    file = FileUtils.createFileIfNotExist(file, "covers", fileName)
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    coverChangeTo(file.absolutePath)
                }
            }.onFailure {
                appCtx.toastOnUi(it.localizedMessage)
            }
        }
    }

}
