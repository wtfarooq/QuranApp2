package com.example.quranapp2

import android.sax.EndTextElementListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.ArrayList

class SurahJuzAdapter(
    private val surahJuzList: ArrayList<SurahJuzItem>?,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<SurahJuzAdapter.SurahJuzViewHolder>() {

    inner class SurahJuzViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        val numberView: TextView = itemView.findViewById(R.id.number)
        val titleView: TextView = itemView.findViewById(R.id.title)
        val subtitleView: TextView = itemView.findViewById(R.id.subtitle)
        val pageView: TextView = itemView.findViewById(R.id.page)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val pageNumber: Int = v?.findViewById<TextView>(R.id.page)?.text.toString().toInt()
            listener.onItemClick(pageNumber)
        }
    }

    interface OnItemClickListener {
        fun onItemClick(pageNumber: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SurahJuzViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.surah_juz_item, parent, false)
        return SurahJuzViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: SurahJuzViewHolder, position: Int) {
        val currentItem = surahJuzList!![position]
        holder.numberView.text = currentItem.number.toString()
        holder.titleView.text = currentItem.name
        holder.subtitleView.text = currentItem.subtext
        holder.pageView.text = currentItem.page.toString()
    }

    override fun getItemCount(): Int = surahJuzList!!.size
}