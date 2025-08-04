package com.contsol.ayra.presentation.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.contsol.ayra.data.source.local.database.model.Tips
import com.contsol.ayra.databinding.ItemCarouselBinding

class HomeAdapter {
}

class TipsCarouselAdapter(
    private var tips: List<Tips>
): RecyclerView.Adapter<TipsCarouselAdapter.TipViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TipViewHolder {
        val binding = ItemCarouselBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TipViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TipViewHolder, position: Int) {
        holder.bind(tips[position])
    }

    override fun getItemCount(): Int = tips.size

    fun updateTips(newTips: List<Tips>) {
        val diffCallback = TipsDiffCallback(tips, newTips)
        val diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(diffCallback)
        this.tips = newTips
        diffResult.dispatchUpdatesTo(this)
    }

    inner class TipViewHolder(
        private val binding: ItemCarouselBinding
    ): RecyclerView.ViewHolder(binding.root) {
        fun bind(tips: Tips) {
            binding.tvCarouselItemTitle.text = tips.title
            binding.tvCarouselItemContent.text = tips.content
        }
    }
}

class TipsDiffCallback(
    private val oldList: List<Tips>,
    private val newList: List<Tips>
) : androidx.recyclerview.widget.DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].title == newList[newItemPosition].title
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}