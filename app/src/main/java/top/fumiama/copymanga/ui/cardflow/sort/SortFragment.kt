package top.fumiama.copymanga.ui.cardflow.sort

import android.os.Bundle
import android.view.View
import com.github.zawadz88.materialpopupmenu.popupMenu
import com.google.gson.Gson
import kotlinx.android.synthetic.main.anchor_popular.view.*
import kotlinx.android.synthetic.main.line_sort.*
import top.fumiama.copymanga.json.FilterStructure
import top.fumiama.copymanga.template.http.AutoDownloadThread
import top.fumiama.copymanga.template.ui.StatusCardFlow
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.dmzj.copymanga.R
import java.lang.Thread.sleep

@ExperimentalStdlibApi
class SortFragment : StatusCardFlow(0, R.id.action_nav_sort_to_nav_book, R.layout.fragment_sort) {
    private var theme = -1
    private var region = -1
    private var filter: FilterStructure? = null

    override fun getApiUrl() =
        getString(R.string.sortApiUrl).format(
                CMApi.myHostApiUrl,
                page * 21,
                sortWay[sortValue],
                if(theme >= 0) (filter?.results?.theme?.get(theme)?.path_word ?: "") else "",
                if(region >= 0) (filter?.results?.top?.get(region)?.path_word ?: "") else "",
            )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lineUpdate = line_sort_time
        lineHot = line_sort_hot
    }

    override fun setListeners() {
        super.setListeners()
        AutoDownloadThread(getString(R.string.filterApiUrl).format(CMApi.myHostApiUrl)) {
            if(ad?.exit == true) return@AutoDownloadThread
            it?.let {
                filter = Gson().fromJson(it.inputStream().reader(), FilterStructure::class.java)
                if(ad?.exit == true) return@AutoDownloadThread
                activity?.runOnUiThread{
                    if(ad?.exit != true) setClasses()
                }
            }
        }.start()
    }

    private fun setClasses(){
        filter?.results?.top?.let { items ->
            if(ad?.exit == true) return@let
            line_sort_region.apt.text = "全部"
            line_sort_region.setOnClickListener {
                val popupMenu = popupMenu {
                    style = R.style.Widget_MPM_Menu_Dark_CustomBackground
                    section {
                        item {
                            label = "全部"
                            labelColor = it.apt.currentTextColor
                            callback = {
                                region = -1
                                it.apt.text = "全部"
                                Thread{
                                    sleep(400)
                                    activity?.runOnUiThread {
                                        reset()
                                        addPage()
                                    }
                                }.start()
                            }
                        }
                        for(i in items.indices) item {
                            label = items[i].name
                            labelColor = it.apt.currentTextColor
                            callback = { //optional
                                it.apt.text = label
                                region = i
                                Thread{
                                    sleep(400)
                                    activity?.runOnUiThread {
                                        reset()
                                        addPage()
                                    }
                                }.start()
                            }
                        }
                    }
                }
                this.context?.let { it1 -> popupMenu.show(it1, it) }
            }
        }
        filter?.results?.theme?.let { items ->
            if(ad?.exit == true) return@let
            line_sort_class.apt.text = "全部"
            line_sort_class.setOnClickListener {
                val popupMenu = popupMenu {
                    style = R.style.Widget_MPM_Menu_Dark_CustomBackground
                    section {
                        item {
                            label = "全部"
                            labelColor = it.apt.currentTextColor
                            callback = {
                                theme = -1
                                it.apt.text = "全部"
                                Thread{
                                    sleep(400)
                                    activity?.runOnUiThread {
                                        reset()
                                        addPage()
                                    }
                                }.start()
                            }
                        }
                        for(i in items.indices) item {
                            label = items[i].name
                            labelColor = it.apt.currentTextColor
                            callback = { //optional
                                it.apt.text = label
                                theme = i
                                Thread{
                                    sleep(400)
                                    activity?.runOnUiThread {
                                        reset()
                                        addPage()
                                    }
                                }.start()
                            }
                        }
                    }
                }
                this.context?.let { it1 -> popupMenu.show(it1, it) }
            }
        }
    }
}