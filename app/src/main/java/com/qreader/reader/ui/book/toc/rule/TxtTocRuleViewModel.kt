package com.qreader.reader.ui.book.toc.rule

import android.app.Application
import com.qreader.reader.base.BaseViewModel
import com.qreader.reader.data.appDb
import com.qreader.reader.data.entities.TxtTocRule
import com.qreader.reader.help.DefaultData

class TxtTocRuleViewModel(app: Application) : BaseViewModel(app) {

    fun save(txtTocRule: TxtTocRule) {
        execute {
            appDb.txtTocRuleDao.insert(txtTocRule)
        }
    }

    fun del(vararg txtTocRule: TxtTocRule) {
        execute {
            appDb.txtTocRuleDao.delete(*txtTocRule)
        }
    }

    fun update(vararg txtTocRule: TxtTocRule) {
        execute {
            appDb.txtTocRuleDao.update(*txtTocRule)
        }
    }

    fun importDefault() {
        execute {
            DefaultData.importDefaultTocRules()
        }
    }

    fun toTop(vararg rules: TxtTocRule) {
        execute {
            val minOrder = appDb.txtTocRuleDao.minOrder - 1
            rules.forEachIndexed { index, source ->
                source.serialNumber = minOrder - index
            }
            appDb.txtTocRuleDao.update(*rules)
        }
    }

    fun toBottom(vararg sources: TxtTocRule) {
        execute {
            val maxOrder = appDb.txtTocRuleDao.maxOrder + 1
            sources.forEachIndexed { index, source ->
                source.serialNumber = maxOrder + index
            }
            appDb.txtTocRuleDao.update(*sources)
        }
    }

    fun upOrder() {
        execute {
            val sources = appDb.txtTocRuleDao.all
            for ((index: Int, source: TxtTocRule) in sources.withIndex()) {
                source.serialNumber = index + 1
            }
            appDb.txtTocRuleDao.update(*sources.toTypedArray())
        }
    }

    fun enableSelection(vararg txtTocRule: TxtTocRule) {
        execute {
            val array = txtTocRule.map { it.copy(enable = true) }.toTypedArray()
            appDb.txtTocRuleDao.insert(*array)
        }
    }

    fun disableSelection(vararg txtTocRule: TxtTocRule) {
        execute {
            val array = txtTocRule.map { it.copy(enable = false) }.toTypedArray()
            appDb.txtTocRuleDao.insert(*array)
        }
    }

}
