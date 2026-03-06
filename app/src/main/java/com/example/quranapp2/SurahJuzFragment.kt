package com.example.quranapp2

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quranapp2.db.DatabaseHelper
import com.example.quranapp2.db.QuranData

class SurahJuzFragment : Fragment(), SurahJuzAdapter.OnItemClickListener {
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
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
        val position = arguments?.getInt("position")
        val dbHelper = context?.let { DatabaseHelper(it) }
        val list = when (position) {
            0 -> dbHelper?.let { toSurahJuzItem(it.getJuzList()) }
            1 -> dbHelper?.let { toSurahJuzItem(it.getSurahList()) }
            else -> ArrayList()
        }
        recyclerView.adapter = SurahJuzAdapter(list ?: ArrayList(), this)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true)
    }

    override fun onItemClick(pageNumber: Int) {
        val intent = Intent(activity, PageActivity::class.java)
        intent.putExtra("pageNum", pageNumber)
        startActivity(intent)
    }

    private fun toSurahJuzItem(list: ArrayList<QuranData>): ArrayList<SurahJuzItem> {
        val surahJuzList = ArrayList<SurahJuzItem>()
        for (item in list) {
            surahJuzList.add(SurahJuzItem(item.id, item.name, item.description, item.pageNumber))
        }
        return surahJuzList
    }
}