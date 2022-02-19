package com.example.quranapp2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

class PageAdapter(private val pages: Array<Int>) : RecyclerView.Adapter<PageAdapter.PageViewHolder>(){

    inner class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pageImg: ImageView = itemView.findViewById(R.id.pageImg)
        val pageNumber: TextView = itemView.findViewById(R.id.pageNum)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.page_item, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val currentItem = pages[position]
        holder.pageImg.setImageResource(currentItem)
        holder.pageNumber.text = (position+1).toString()
    }

    override fun getItemCount(): Int = pages.size
}