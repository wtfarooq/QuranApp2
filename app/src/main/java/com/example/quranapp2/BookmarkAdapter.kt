package com.example.quranapp2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.core.graphics.toColorInt
import com.google.android.material.color.MaterialColors

class BookmarkAdapter(
    private val bookmarkList: ArrayList<SurahJuzItem>,
    private val listener: OnBookmarkClickListener
) : RecyclerView.Adapter<BookmarkAdapter.BookmarkViewHolder>() {

    private var deleteMode: Int = -1

    class BookmarkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: CardView = itemView.findViewById(R.id.card)
        val actionIcon: ImageView = itemView.findViewById(R.id.actionIcon)
        val titleView: TextView = itemView.findViewById(R.id.title)
        val subtitleView: TextView = itemView.findViewById(R.id.subtitle)
        val pageView: TextView = itemView.findViewById(R.id.page)
    }

    interface OnBookmarkClickListener {
        fun onItemClick(pageNumber: Int)
        fun onDeleteClick(pageNumber: Int, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.bookmark_item, parent, false)
        return BookmarkViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        val currentItem = bookmarkList[position]
        holder.titleView.text = currentItem.name
        holder.subtitleView.text = currentItem.subtext
        holder.pageView.text = currentItem.page.toString()

        val context = holder.itemView.context
        val defaultCardColor = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorSurface)

        if (position == deleteMode) {
            holder.actionIcon.setImageResource(R.drawable.delete)
            holder.actionIcon.setColorFilter("#D32F2F".toColorInt())
            holder.actionIcon.contentDescription = "Delete bookmark"
            holder.card.setCardBackgroundColor("#4DD32F2F".toColorInt())
        } else {
            holder.actionIcon.setImageResource(R.drawable.play)
            holder.actionIcon.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent))
            holder.actionIcon.contentDescription = "Open bookmark"
            holder.card.setCardBackgroundColor(defaultCardColor)
        }

        holder.itemView.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            if (deleteMode != -1) {
                val old = deleteMode
                deleteMode = -1
                notifyItemChanged(old)
            } else {
                listener.onItemClick(bookmarkList[pos].page)
            }
        }

        holder.itemView.setOnLongClickListener {
            val pos = holder.adapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnLongClickListener false
            val old = deleteMode
            deleteMode = pos
            if (old != -1) notifyItemChanged(old)
            notifyItemChanged(pos)
            true
        }

        holder.actionIcon.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            if (pos == deleteMode) {
                deleteMode = -1
                listener.onDeleteClick(bookmarkList[pos].page, pos)
                bookmarkList.removeAt(pos)
                notifyItemRemoved(pos)
            } else {
                listener.onItemClick(bookmarkList[pos].page)
            }
        }
    }

    fun clearDeleteMode() {
        if (deleteMode != -1) {
            val old = deleteMode
            deleteMode = -1
            notifyItemChanged(old)
        }
    }

    override fun getItemCount(): Int = bookmarkList.size
}
