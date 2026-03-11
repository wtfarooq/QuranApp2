package com.example.quranapp2

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class JuzProgressAdapter(
    private val pages: List<Int>,
    private val readPages: Set<Int>
) : RecyclerView.Adapter<JuzProgressAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.juz_progress_item, parent, false) as TextView
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val page = pages[position]
        holder.text.text = page.toString()
        val isRead = page in readPages
        holder.text.setBackgroundColor(
            if (isRead) ContextCompat.getColor(holder.text.context, R.color.colorAccent)
            else android.graphics.Color.TRANSPARENT
        )
        holder.text.setTextColor(
            if (isRead) ContextCompat.getColor(holder.text.context, R.color.comparisonBannerText)
            else {
                val out = TypedValue()
                holder.text.context.theme.resolveAttribute(android.R.attr.textColorPrimary, out, true)
                out.data
            }
        )
    }

    override fun getItemCount(): Int = pages.size

    class Holder(val text: TextView) : RecyclerView.ViewHolder(text)
}
