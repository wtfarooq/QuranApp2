package com.example.quranapp2

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

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
        val list = arguments?.getParcelableArrayList<SurahJuzItem>("list")
        recyclerView.adapter = SurahJuzAdapter(list, this)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true)
    }

    override fun onItemClick(pageNumber: Int) {
        val intent = Intent(activity, PageActivity::class.java)
        intent.putExtra("pageNum", pageNumber)
        startActivity(intent)
    }
}