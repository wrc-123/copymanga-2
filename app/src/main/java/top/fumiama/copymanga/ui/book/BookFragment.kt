package top.fumiama.copymanga.ui.book

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.card_book.*
import kotlinx.android.synthetic.main.fragment_book.*
import kotlinx.android.synthetic.main.line_bookinfo_text.*
import kotlinx.android.synthetic.main.line_booktandb.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.json.VolumeStructure
import top.fumiama.copymanga.manga.Reader
import top.fumiama.copymanga.template.general.NoBackRefreshFragment
import top.fumiama.copymanga.tools.ui.Navigate
import top.fumiama.copymanga.ui.comicdl.ComicDlFragment
import top.fumiama.dmzj.copymanga.R
import java.io.File
import java.lang.Thread.sleep
import java.lang.ref.WeakReference

class BookFragment: NoBackRefreshFragment(R.layout.fragment_book) {
    var isOnPause = false
    private var mBookHandler: BookHandler? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ComicDlFragment.exit = false
        fbvp?.setPadding(0, 0, 0, navBarHeight)

        if(isFirstInflate) {
            var path = ""
            arguments?.apply {
                if (getBoolean("loadJson")) {
                    getString("name")?.let { name ->
                        activity?.getExternalFilesDir("")?.let {
                            Gson().fromJson(File(File(it, name), "info.json").readText(), Array<VolumeStructure>::class.java)
                        }?.apply {
                            if (isEmpty() || get(0).results.list.isEmpty()) {
                                findNavController().popBackStack()
                                return
                            }
                            else {
                                path = get(0).results.list[0].comic_path_word
                            }
                        }
                    }
                } else getString("path").let {
                    if (it != null) path = it
                    else {
                        findNavController().popBackStack()
                        return
                    }
                }
            }
            mBookHandler = BookHandler(WeakReference(this), path)
            Log.d("MyBF", "read path: $path")
            bookHandler = mBookHandler
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    sleep(600)
                    mBookHandler?.startLoad()
                }
            }
        } else {
            bookHandler = mBookHandler
        }
    }

    override fun onResume() {
        super.onResume()
        isOnPause = false
        bookHandler = mBookHandler
        activity?.apply {
            toolbar.title = mBookHandler?.book?.results?.comic?.name
        }
        setStartRead()
    }

    override fun onPause() {
        super.onPause()
        isOnPause = true
    }

    override fun onDestroy() {
        super.onDestroy()
        mBookHandler?.destroy()
        mBookHandler?.ads?.forEach {
            it.exit = true
        }
        bookHandler = null
    }

    fun setStartRead() {
        if(mBookHandler?.chapterNames?.isNotEmpty() == true) activity?.apply {
            mBookHandler?.book?.results?.comic?.let { comic ->
                getPreferences(MODE_PRIVATE).getInt(comic.name, -1).let { p ->
                    this@BookFragment.lbbstart.apply {
                        var i = 0
                        if(p >= 0) {
                            text = mBookHandler!!.chapterNames[p]
                            i = p
                        }
                        setOnClickListener {
                            mBookHandler?.urlArray?.let {
                                Reader.viewMangaAt(comic.name, i, it)
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun setAddToShelf() {
        if(mBookHandler?.chapterNames?.isNotEmpty() != true) return
        lifecycleScope.launch {
            val b = MainActivity.shelf?.query(mBookHandler?.path!!)
            mBookHandler?.collect = b?.results?.collect?:-2
            Log.d("MyBF", "get collect of ${mBookHandler?.path} = ${mBookHandler?.collect}")
            tic.text = b?.results?.browse?.chapter_name?.let { name ->
                getString(R.string.text_format_cloud_read_to).format(name)
            }
            mBookHandler?.collect?.let { collect ->
                if (collect > 0) {
                    this@BookFragment.lbbsub.setText(R.string.button_sub_subscribed)
                }
            }
            mBookHandler?.book?.results?.comic?.let { comic ->
                this@BookFragment.lbbsub.setOnClickListener {
                    lifecycleScope.launch clickLaunch@ {
                        if (this@BookFragment.lbbsub.text != getString(R.string.button_sub)) {
                            mBookHandler?.collect?.let { collect ->
                                if (collect < 0) return@clickLaunch
                                val re = MainActivity.shelf?.del(collect)
                                Toast.makeText(context, re, Toast.LENGTH_SHORT).show()
                                if (re == "请求成功") {
                                    this@BookFragment.lbbsub.setText(R.string.button_sub)
                                }
                            }
                            return@clickLaunch
                        }
                        val re = MainActivity.shelf?.add(comic.uuid)
                        Toast.makeText(context, re, Toast.LENGTH_SHORT).show()
                        if (re == "修改成功") {
                            this@BookFragment.lbbsub.setText(R.string.button_sub_subscribed)
                        }
                    }
                }
            }
        }
    }

    fun navigate2dl(){
        val bundle = Bundle()
        bundle.putString("path", arguments?.getString("path")?:"null")
        bundle.putString("name", mBookHandler!!.book?.results?.comic?.name)
        if(mBookHandler!!.vols != null) {
            bundle.putBoolean("loadJson", true)
        }
        bundle.putStringArray("group", mBookHandler!!.gpws)
        bundle.putStringArray("groupNames", mBookHandler!!.keys)
        bundle.putIntArray("count", mBookHandler!!.cnts)
        findNavController().let {
            Navigate.safeNavigateTo(it, R.id.action_nav_book_to_nav_group, bundle)
        }
    }

    companion object {
        var bookHandler: BookHandler? = null
    }
}