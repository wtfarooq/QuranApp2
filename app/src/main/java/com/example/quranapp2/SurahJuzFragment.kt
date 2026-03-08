package com.example.quranapp2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quranapp2.db.DatabaseHelper
import com.example.quranapp2.db.QuranData
import androidx.core.content.edit

class SurahJuzFragment : Fragment(), SurahJuzAdapter.OnItemClickListener, BookmarkAdapter.OnBookmarkClickListener {
    private var recyclerView: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_surah_juz, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView?.layoutManager = LinearLayoutManager(context)
        recyclerView?.setHasFixedSize(true)

        recyclerView?.let { rv ->
            val basePadding = rv.paddingBottom
            ViewCompat.setOnApplyWindowInsetsListener(rv) { v, insets ->
                val navBar = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.updatePadding(bottom = basePadding + navBar.bottom)
                insets
            }
        }

        val position = arguments?.getInt("position")
        val dbHelper = context?.let { DatabaseHelper(it) }
        when (position) {
            0 -> {
                val list = dbHelper?.let { toSurahJuzItem(it.getJuzList()) } ?: ArrayList()
                recyclerView?.adapter = SurahJuzAdapter(list, this)
            }
            1 -> {
                val list = dbHelper?.let { toSurahJuzItem(it.getSurahList()) } ?: ArrayList()
                recyclerView?.adapter = SurahJuzAdapter(list, this)
            }
            2 -> {
                val list = dbHelper?.let { getBookmarkItems(it) } ?: ArrayList()
                val bookmarkAdapter = BookmarkAdapter(list, this)
                recyclerView?.adapter = bookmarkAdapter
                addEmptySpaceTouchListener(bookmarkAdapter)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val position = arguments?.getInt("position")
        if (position == 2) {
            val dbHelper = context?.let { DatabaseHelper(it) }
            val list = dbHelper?.let { getBookmarkItems(it) } ?: ArrayList()
            val bookmarkAdapter = BookmarkAdapter(list, this)
            recyclerView?.adapter = bookmarkAdapter
            addEmptySpaceTouchListener(bookmarkAdapter)
        }
    }

    override fun onItemClick(pageNumber: Int) {
        val intent = Intent(activity, PageActivity::class.java)
        intent.putExtra("pageNum", pageNumber)
        startActivity(intent)
    }

    override fun onDeleteClick(pageNumber: Int, position: Int) {
        val prefs = requireContext().getSharedPreferences("bookmarks", Context.MODE_PRIVATE)
        val bookmarks = prefs.getStringSet("pages", mutableSetOf())!!.toMutableSet()
        bookmarks.remove(pageNumber.toString())
        prefs.edit { putStringSet("pages", bookmarks) }
    }

    private fun toSurahJuzItem(list: ArrayList<QuranData>): ArrayList<SurahJuzItem> {
        val surahJuzList = ArrayList<SurahJuzItem>()
        for (item in list) {
            surahJuzList.add(SurahJuzItem(item.id, item.name, item.description, item.pageNumber))
        }
        return surahJuzList
    }

    private fun addEmptySpaceTouchListener(adapter: BookmarkAdapter) {
        recyclerView?.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (e.action == MotionEvent.ACTION_DOWN) {
                    val child = rv.findChildViewUnder(e.x, e.y)
                    if (child == null) {
                        adapter.clearDeleteMode()
                    }
                }
                return false
            }
        })
    }

    private fun getBookmarkItems(dbHelper: DatabaseHelper): ArrayList<SurahJuzItem> {
        val prefs = requireContext().getSharedPreferences("bookmarks", Context.MODE_PRIVATE)
        val bookmarks = prefs.getStringSet("pages", mutableSetOf()) ?: mutableSetOf()
        val list = ArrayList<SurahJuzItem>()
        val sortedPages = bookmarks.mapNotNull { it.toIntOrNull() }.sorted()
        for (page in sortedPages) {
            val surahName = dbHelper.getSurahForPage(page)
            val juzName = dbHelper.getJuzForPage(page)
            list.add(SurahJuzItem(page, surahName, juzName, page))
        }
        return list
    }
}