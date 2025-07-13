package com.contsol.ayra.ui.chat // Use your actual package name

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.contsol.ayra.R
import com.contsol.ayra.data.source.local.database.entity.ChatLogEntity

class ChatAdapter : ListAdapter<ChatLogEntity, RecyclerView.ViewHolder>(ChatDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AYRA = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).is_user_message) {
            true -> VIEW_TYPE_USER
            false -> VIEW_TYPE_AYRA
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_user, parent, false)
                UserMessageViewHolder(view)
            }
            VIEW_TYPE_AYRA -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_ayra, parent, false)
                AiMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is UserMessageViewHolder -> holder.bind(message)
            is AiMessageViewHolder -> holder.bind(message)
        }
        // Apply animation
        holder.itemView.animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.item_slide_in)
    }

    inner class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.textViewMessage)
        fun bind(chatMessage: ChatLogEntity) {
            messageText.text = chatMessage.message_content
        }
    }

    inner class AiMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.textViewMessage)
        fun bind(chatMessage: ChatLogEntity) {
            messageText.text = chatMessage.message_content
        }
    }

    class ChatDiffCallback : DiffUtil.ItemCallback<ChatLogEntity>() {
        override fun areItemsTheSame(oldItem: ChatLogEntity, newItem: ChatLogEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatLogEntity, newItem: ChatLogEntity): Boolean {
            return oldItem == newItem
        }
    }
}
