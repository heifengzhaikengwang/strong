package com.paperscanner.app.ui.adapter

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.paperscanner.app.databinding.ItemPreviewBinding

class PreviewAdapter(
    private val onItemClick: (Int) -> Unit
) : ListAdapter<Bitmap, PreviewAdapter.PreviewViewHolder>(BitmapDiffCallback()) {

    private var selectedPosition = 0
    private var totalCount = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
        val binding = ItemPreviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PreviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
        holder.bind(getItem(position), position, selectedPosition)
    }

    fun setSelectedPosition(position: Int) {
        val oldPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(oldPosition)
        notifyItemChanged(position)
    }

    fun getSelectedPosition(): Int = selectedPosition

    fun setTotalCount(count: Int) {
        totalCount = count
    }

    inner class PreviewViewHolder(
        private val binding: ItemPreviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(position)
                }
            }
        }

        fun bind(bitmap: Bitmap, position: Int, selectedPos: Int) {
            binding.ivPreview.setImageBitmap(bitmap)
            binding.root.isSelected = position == selectedPos
        }
    }

    private class BitmapDiffCallback : DiffUtil.ItemCallback<Bitmap>() {
        override fun areItemsTheSame(oldItem: Bitmap, newItem: Bitmap): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Bitmap, newItem: Bitmap): Boolean {
            return oldItem.sameAs(newItem)
        }
    }
}
