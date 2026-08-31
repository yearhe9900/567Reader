package com.qreader.reader.ui.about

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.qreader.reader.R
import com.qreader.reader.base.BaseDialogFragment
import com.qreader.reader.base.BaseViewModel
import com.qreader.reader.base.adapter.ItemViewHolder
import com.qreader.reader.base.adapter.RecyclerAdapter
import com.qreader.reader.databinding.DialogRecyclerViewBinding
import com.qreader.reader.databinding.Item1lineTextBinding
import com.qreader.reader.help.config.AppConfig
import com.qreader.reader.lib.theme.primaryColor
import com.qreader.reader.ui.widget.dialog.TextDialog
import com.qreader.reader.utils.FileDoc
import com.qreader.reader.utils.FileUtils
import com.qreader.reader.utils.delete
import com.qreader.reader.utils.find
import com.qreader.reader.utils.getFile
import com.qreader.reader.utils.list
import com.qreader.reader.utils.setLayout
import com.qreader.reader.utils.showDialogFragment
import com.qreader.reader.utils.toastOnUi
import com.qreader.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.isActive
import java.io.FileFilter

class CrashLogsDialog : BaseDialogFragment(R.layout.dialog_recycler_view),
    Toolbar.OnMenuItemClickListener {

    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    private val viewModel by viewModels<CrashViewModel>()
    private val adapter by lazy { LogAdapter() }

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.setTitle(R.string.crash_log)
        binding.toolBar.inflateMenu(R.menu.crash_log)
        binding.toolBar.setOnMenuItemClickListener(this)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        viewModel.logLiveData.observe(viewLifecycleOwner) {
            adapter.setItems(it)
        }
        viewModel.initData()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_clear -> viewModel.clearCrashLog()
        }
        return true
    }

    private fun showLogFile(fileDoc: FileDoc) {
        viewModel.readFile(fileDoc) {
            if (lifecycleScope.isActive) {
                showDialogFragment(TextDialog(fileDoc.name, it))
            }
        }

    }

    inner class LogAdapter : RecyclerAdapter<FileDoc, Item1lineTextBinding>(requireContext()) {

        override fun getViewBinding(parent: ViewGroup): Item1lineTextBinding {
            return Item1lineTextBinding.inflate(inflater, parent, false)
        }

        override fun registerListener(holder: ItemViewHolder, binding: Item1lineTextBinding) {
            binding.root.setOnClickListener {
                getItemByLayoutPosition(holder.layoutPosition)?.let { item ->
                    showLogFile(item)
                }
            }
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: Item1lineTextBinding,
            item: FileDoc,
            payloads: MutableList<Any>
        ) {
            binding.textView.text = item.name
        }

    }

    class CrashViewModel(application: Application) : BaseViewModel(application) {

        val logLiveData = MutableLiveData<List<FileDoc>>()

        fun initData() {
            execute {
                val list = arrayListOf<FileDoc>()
                context.externalCacheDir
                    ?.getFile("crash")
                    ?.listFiles(FileFilter { it.isFile })
                    ?.forEach {
                        list.add(FileDoc.fromFile(it))
                    }
                val backupPath = AppConfig.backupPath
                if (!backupPath.isNullOrEmpty()) {
                    val uri = Uri.parse(backupPath)
                    FileDoc.fromUri(uri, true)
                        .find("crash")
                        ?.list {
                            !it.isDir
                        }?.let {
                            list.addAll(it)
                        }
                }
                return@execute list.sortedByDescending { it.name }.distinctBy { it.name }
            }.onSuccess {
                logLiveData.postValue(it)
            }
        }

        fun readFile(fileDoc: FileDoc, success: (String) -> Unit) {
            execute {
                String(fileDoc.readBytes())
            }.onSuccess {
                success.invoke(it)
            }.onError {
                context.toastOnUi(it.localizedMessage)
            }
        }

        fun clearCrashLog() {
            execute {
                context.externalCacheDir
                    ?.getFile("crash")
                    ?.let {
                        FileUtils.delete(it, false)
                    }
                val backupPath = AppConfig.backupPath
                if (!backupPath.isNullOrEmpty()) {
                    val uri = Uri.parse(backupPath)
                    FileDoc.fromUri(uri, true)
                        .find("crash")
                        ?.delete()
                }
            }.onError {
                context.toastOnUi(it.localizedMessage)
            }.onFinally {
                initData()
            }
        }

    }

}