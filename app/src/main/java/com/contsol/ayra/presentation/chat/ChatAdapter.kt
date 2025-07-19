package com.contsol.ayra.presentation.chat // Use your actual package name

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.contsol.ayra.R
import com.contsol.ayra.data.source.local.database.model.ChatLog
import com.contsol.ayra.utils.convertTimestampToDate

class ChatAdapter : ListAdapter<ChatLog, RecyclerView.ViewHolder>(ChatDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_USER_WITH_IMAGE = 2
        private const val VIEW_TYPE_AYRA = 3
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return when (message.isUserMessage) {
            true -> {
                if (message.imageUrl != null) VIEW_TYPE_USER_WITH_IMAGE else VIEW_TYPE_USER
            }
            false -> {
                // if (message.imageUrl != null) VIEW_TYPE_AI_WITH_IMAGE else VIEW_TYPE_AI // If AI can also send images
                VIEW_TYPE_AYRA
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_user, parent, false)
                UserMessageViewHolder(view)
            }
            VIEW_TYPE_USER_WITH_IMAGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_user_with_image, parent, false)
                UserMessageImageViewHolder(view)
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
            is UserMessageImageViewHolder -> holder.bind(message)
            is AiMessageViewHolder -> holder.bind(message)
        }
        // Apply animation
        holder.itemView.animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.item_slide_in)
    }

    inner class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.textViewMessage)
        private val messageTimestamp: TextView = itemView.findViewById(R.id.textViewTimestamp)
        fun bind(chatMessage: ChatLog) {
            messageText.text = chatMessage.messageContent
            messageTimestamp.text = convertTimestampToDate(chatMessage.timestamp)
        }
    }

    inner class UserMessageImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.textViewMessage)
        private val messageTimestamp: TextView = itemView.findViewById(R.id.textViewTimestamp)
        private val messageImage: ImageView = itemView.findViewById(R.id.imageViewSent) // Ensure this ID matches your item_chat_user_with_image.xml

        fun bind(chatMessage: ChatLog) {
            // Bind text message content
            if (chatMessage.messageContent.isNotEmpty()) {
                messageText.text = chatMessage.messageContent
                messageText.visibility = View.VISIBLE
            } else {
                // Hide the TextView if there's no text message,
                // useful if the message is only an image.
                messageText.visibility = View.GONE
            }

            // Bind timestamp
            messageTimestamp.text = convertTimestampToDate(chatMessage.timestamp)

            // Bind image using Coil
            if (chatMessage.imageUrl != null) {
                messageImage.visibility = View.VISIBLE // Make ImageView visible
                messageImage.load(chatMessage.imageUrl) {
                    // placeholder(R.drawable.ic_placeholder_image) // Optional: a drawable to show while loading
                    // error(R.drawable.ic_error_image) // Optional: a drawable to show if loading fails
                    // crossfade(true) // Optional: for a fade-in effect
                    // transformations(RoundedCornersTransformation(16f)) // Optional: if you want rounded corners on the image itself.
                    // Ensure you have the coil-transformations artifact if you use this.
                }
            } else {
                messageImage.visibility = View.GONE
            }
        }
    }


    inner class AiMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.textViewMessage)
        private val messageTimestamp: TextView = itemView.findViewById(R.id.textViewTimestamp)
        fun bind(chatMessage: ChatLog) {
            messageText.text = chatMessage.messageContent
            messageTimestamp.text = convertTimestampToDate(chatMessage.timestamp)
        }
    }

    class ChatDiffCallback : DiffUtil.ItemCallback<ChatLog>() {
        override fun areItemsTheSame(oldItem: ChatLog, newItem: ChatLog): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatLog, newItem: ChatLog): Boolean {
            return oldItem == newItem
        }
    }
}
