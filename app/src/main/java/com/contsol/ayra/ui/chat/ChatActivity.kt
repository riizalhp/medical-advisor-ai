package com.contsol.ayra.ui.chat

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope // Import for lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.contsol.ayra.R
import com.contsol.ayra.data.ai.LlmInferenceManager // Import LlmInferenceManager
import com.contsol.ayra.data.source.local.database.entity.ChatLogEntity
import kotlinx.coroutines.launch // Import launch
import kotlin.collections.toList

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerViewChat: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: Button
    private lateinit var chatAdapter: ChatAdapter
    private val messagesList = mutableListOf<ChatLogEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        recyclerViewChat = findViewById(R.id.recyclerViewChat)
        editTextMessage = findViewById(R.id.editTextMessage)
        buttonSend = findViewById(R.id.buttonSend)

        setupRecyclerView()
        loadSampleMessages() // Load initial sample messages

        buttonSend.setOnClickListener {
            val messageText = editTextMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                editTextMessage.text.clear()
                generateAiResponse(messageText) // Call the new function
            }
        }

        // Optional: Show a message if LLM is not yet ready when activity starts
        if (!LlmInferenceManager.isInitialized()) {
            Toast.makeText(this, "AI is initializing, please wait...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerViewChat.layoutManager = layoutManager
        recyclerViewChat.adapter = chatAdapter
        recyclerViewChat.itemAnimator = null
    }

    private fun sendMessage(text: String) {
        val message = ChatLogEntity(message_content = text, is_user_message = true)
        addNewMessage(message)
    }

    private fun generateAiResponse(originalMessage: String) {
        if (!LlmInferenceManager.isInitialized()) {
            Log.w("ChatActivity", "LLM not initialized. Cannot generate AI response yet.")
            // Optionally add a placeholder "AI is thinking..." message or a specific error message
            val thinkingMessage = ChatLogEntity(
                message_content = "AYRA is still initializing, please try again shortly.",
                is_user_message = false
            )
            addNewMessage(thinkingMessage)
            // You might also want to re-check initialization status or trigger it if needed,
            // though Application level init should handle it.
            return
        }

        // Use lifecycleScope to launch a coroutine
        lifecycleScope.launch {
            try {
                // Show a "AYRA is typing..." message (optional)
                val typingMessage = ChatLogEntity(
                    message_content = "AYRA is typing...",
                    is_user_message = false
                )
                addNewMessage(typingMessage)
                val temporaryTypingMessageIndex = messagesList.size -1

                Log.d("ChatActivity", "Requesting AI response for: $originalMessage")
                val aiResponseText = LlmInferenceManager.run(originalMessage)

                // Remove "AYRA is typing..." message
                if (temporaryTypingMessageIndex >= 0 && messagesList[temporaryTypingMessageIndex].message_content == "AYRA is typing...") {
                    messagesList.removeAt(temporaryTypingMessageIndex)
                    // No need to call chatAdapter.notifyItemRemoved, submitList will handle it.
                }

                if (aiResponseText != null) {
                    val aiMessage = ChatLogEntity(
                        message_content = aiResponseText, // Use the actual response
                        is_user_message = false
                    )
                    addNewMessage(aiMessage)
                } else {
                    Log.e("ChatActivity", "Received null response from LlmInferenceManager.")
                    val errorMessage = ChatLogEntity(
                        message_content = "Sorry, I couldn't get a response right now.",
                        is_user_message = false
                    )
                    addNewMessage(errorMessage)
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error getting AI response: ${e.message}", e)
                val errorMessage = ChatLogEntity(
                    message_content = "An error occurred while getting a response.",
                    is_user_message = false
                )
                addNewMessage(errorMessage)
            }
        }
    }

    private fun addNewMessage(message: ChatLogEntity) {
        messagesList.add(message)
        chatAdapter.submitList(messagesList.toList()) // Submit a new list for DiffUtil
        recyclerViewChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
    }

    private fun loadSampleMessages() {
        // You might want to clear this or not load samples if you expect fresh chat always
        // messagesList.clear()
        messagesList.addAll(listOf(
            ChatLogEntity(message_content = "Hello AYRA!", is_user_message = true, timestamp = System.currentTimeMillis() - 50000),
            ChatLogEntity(message_content = "Hello! I'm AYRA. How can I assist you today?", is_user_message = false, timestamp = System.currentTimeMillis() - 40000)
        ))
        chatAdapter.submitList(messagesList.toList())
        if (chatAdapter.itemCount > 0) {
            recyclerViewChat.scrollToPosition(chatAdapter.itemCount - 1)
        }
    }
}
