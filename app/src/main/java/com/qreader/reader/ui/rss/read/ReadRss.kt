package com.qreader.reader.ui.rss.read

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.qreader.reader.constant.SourceType
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.RssArticle
import com.qreader.reader.data.entities.RssReadRecord
import com.qreader.reader.data.entities.RssSource
import com.qreader.reader.exception.ContentEmptyException
import com.qreader.reader.model.rss.Rss
import com.qreader.reader.ui.video.VideoPlayerActivity
import com.qreader.reader.ui.widget.dialog.PhotoDialog
import com.qreader.reader.utils.NetworkUtils
import com.qreader.reader.utils.showDialogFragment
import com.qreader.reader.utils.startActivity
import kotlinx.coroutines.Dispatchers.IO

object ReadRss {
    /**
     * 通过RSS历史记录点击阅读
     */
    fun readRss(activity: AppCompatActivity, record: RssReadRecord) {
        val type = record.type
        if (type == 0) {
            ReadRssActivity.start(
                activity,
                record.origin,
                record.title,
                link = record.record,
                sort = record.sort
            )
            return
        }
        if (type == 2) {
            activity.startActivity<VideoPlayerActivity> {
                putExtra("sourceKey", record.origin)
                putExtra("sourceType", SourceType.rss)
                putExtra("record", record.record)
            }
            return
        }
        readNoHtml(activity, record, type)
    }

    fun readRss(fragment: Fragment, rssArticle: RssArticle,rssSource: RssSource? = null) {
        val rssReadRecord = rssArticle.toRecord()
        appDb.rssReadRecordDao.insertRecord(rssReadRecord)
        val type = rssArticle.type
        if (type == 0) {
            //web网页
            ReadRssActivity.start(
                fragment.requireContext(),
                rssArticle.origin,
                rssArticle.title,
                link = rssArticle.link,
                sort = rssArticle.sort
            )
            return
        }
        if (type == 2) {
            //视频播放
            fragment.startActivity<VideoPlayerActivity> {
                putExtra("sourceKey", rssArticle.origin)
                putExtra("sourceType", SourceType.rss)
                putExtra("record", rssArticle.link)
            }
            return
        }
        readNoHtml(fragment, rssArticle, rssSource, type)
    }

    private fun readNoHtml(fragment: Fragment, rssArticle: RssArticle, rssSource: RssSource? = null, type: Int) {
        val rssSource = rssSource ?: appDb.rssSourceDao.getByKey(rssArticle.origin)
        rssSource?.let { s ->
            val ruleContent = s.ruleContent
            if (ruleContent.isNullOrBlank()) {
                when (type) {
                    1 -> fragment.showDialogFragment(PhotoDialog(rssArticle.link))
                }
            } else {
                Rss.getContent(fragment.viewLifecycleOwner.lifecycleScope, rssArticle, ruleContent, s)
                    .onSuccess(IO) { body ->
                        if (body.isBlank()) {
                            throw ContentEmptyException("正文为空")
                        }
                        val url = NetworkUtils.getAbsoluteURL(rssArticle.link, body)
                        when (type) {
                            1 -> fragment.showDialogFragment(PhotoDialog(url))
                        }
                    }.onError {
                    }
            }
        }
    }

    private fun readNoHtml(activity: AppCompatActivity, record: RssReadRecord, type: Int) {
        val rssSource = appDb.rssSourceDao.getByKey(record.origin)
        rssSource?.let { s ->
            val ruleContent = s.ruleContent
            if (ruleContent.isNullOrBlank()) {
                when (type) {
                    1 -> activity.showDialogFragment(PhotoDialog(record.record))
                }
            } else {
                Rss.getContent(activity.lifecycleScope, record.toRssArticle(), ruleContent, s)
                    .onSuccess(IO) { body ->
                        val url = NetworkUtils.getAbsoluteURL(record.record, body)
                        when (type) {
                            1 -> activity.showDialogFragment(PhotoDialog(url))
                        }
                    }.onError {
                    }
            }
        }
    }

}