package com.qreader.reader.ui.book.info.edit

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsCompat
import com.qreader.reader.R
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
import com.qreader.reader.utils.setOnApplyWindowInsetsListenerCompat
import com.qreader.reader.utils.showDialogFragment
import com.qreader.reader.utils.toastOnUi
import com.qreader.reader.utils.viewbindingdelegate.viewBinding
import splitties.init.appCtx
import splitties.views.bottomPadding
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.bookData.observe(this) { upView(it) }
        if (viewModel.bookData.value == null) {
            intent.getStringExtra("bookUrl")?.let {
                viewModel.loadBook(it)
            }
        }
        initView()
        initEvent()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_info_edit, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> saveData()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        binding.root.setOnApplyWindowInsetsListenerCompat { view, windowInsets ->
            val typeMask = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            val insets = windowInsets.getInsets(typeMask)
            view.bottomPadding = insets.bottom
            windowInsets
        }
    }

    private fun initEvent() = binding.run {
        tvChangeCover.setOnClickListener {
            viewModel.bookData.value?.let {
                showDialogFragment(
                    ChangeCoverDialog(it.name, it.author)
                )
            }
        }
        tvSelectCover.setOnClickListener {
            selectCover.launch {
                mode = HandleFileContract.IMAGE
            }
        }
        tvRefreshCover.setOnClickListener {
            viewModel.book?.customCoverUrl = tieCoverUrl.text?.toString()
            upCover()
        }
    }

    private fun upView(book: Book) = binding.run {
        tieBookName.setText(book.name)
        tieBookAuthor.setText(book.author)
        spType.setSelection(
            when {
                book.isVideo -> 4
                book.isImage -> 2
                book.isAudio -> 1
                else -> 0
            }
        )
        tieCoverUrl.setText(book.getDisplayCover())
        tieBookIntro.setText(book.getDisplayIntro())
        upCover()
    }

    private fun upCover() {
        viewModel.book?.let {
            binding.ivCover.load(it, false)
        }
    }

    private fun saveData() = binding.run {
        val book = viewModel.book ?: return@run
        val oldBook = book.copy()
        book.name = tieBookName.text?.toString() ?: ""
        book.author = tieBookAuthor.text?.toString() ?: ""
        val local = if (book.isLocal) BookType.local else 0
        val bookType = when (spType.selectedItemPosition) {
            4 -> BookType.video or local
            2 -> BookType.image or local
            1 -> BookType.audio or local
            else -> BookType.text or local
        }
        book.removeType(BookType.video, BookType.local, BookType.image, BookType.audio, BookType.text)
        book.addType(bookType)
        val customCoverUrl = tieCoverUrl.text?.toString()
        book.customCoverUrl = if (customCoverUrl == book.coverUrl) null else customCoverUrl
        val customIntro = tieBookIntro.text?.toString()
        book.customIntro = if (customIntro == book.intro) null else customIntro
        BookHelp.updateCacheFolder(oldBook, book)
        viewModel.saveBook(book) {
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun coverChangeTo(coverUrl: String) {
        viewModel.book?.customCoverUrl = coverUrl
        binding.tieCoverUrl.setText(coverUrl)
        upCover()
    }

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