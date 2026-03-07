package com.example.quranapp2

import android.content.res.Configuration
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PageAdapter(private val pages: Array<Int>) : RecyclerView.Adapter<PageAdapter.PageViewHolder>(){

    private val invertColorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
        -0.84f, 0f, 0f, 0f, 232f,
         0f, -0.78f, 0f, 0f, 216f,
         0f, 0f, -0.67f, 0f, 188f,
         0f, 0f, 0f, 1f, 0f
    )))

    class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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

        val nightMode = holder.itemView.context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        if (nightMode) {
            holder.pageImg.colorFilter = invertColorFilter
        } else {
            holder.pageImg.colorFilter = null
        }
    }

    override fun getItemCount(): Int = pages.size
}