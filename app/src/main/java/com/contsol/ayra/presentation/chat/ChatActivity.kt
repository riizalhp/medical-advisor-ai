package com.contsol.ayra.presentation.chat

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope // Import for lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.contsol.ayra.R
import com.contsol.ayra.data.ai.LlmInferenceManager // Import LlmInferenceManager
import com.contsol.ayra.data.source.local.database.model.ChatLog
import kotlinx.coroutines.launch // Import launch
import kotlin.collections.toList

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerViewChat: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: ImageButton
    private lateinit var chatAdapter: ChatAdapter
    private val messagesList = mutableListOf<ChatLog>()

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
        val message = ChatLog(messageContent = text, isUserMessage = true, timestamp = System.currentTimeMillis())
        addNewMessage(message)
    }

    private fun generateAiResponse(originalMessage: String) {
        if (!LlmInferenceManager.isInitialized()) {
            Log.w("ChatActivity", "LLM not initialized. Cannot generate AI response yet.")
            // Optionally add a placeholder "AI is thinking..." message or a specific error message
            val thinkingMessage = ChatLog(
                messageContent = "AYRA is still initializing, please try again shortly.",
                isUserMessage = false
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
                val typingMessage = ChatLog(
                    messageContent = "AYRA is typing...",
                    isUserMessage = false
                )
                addNewMessage(typingMessage)
                val temporaryTypingMessageIndex = messagesList.size -1

                Log.d("ChatActivity", "Requesting AI response for: $originalMessage")
                val aiResponseText = LlmInferenceManager.run(originalMessage)

                // Remove "AYRA is typing..." message
                if (temporaryTypingMessageIndex >= 0 && messagesList[temporaryTypingMessageIndex].messageContent == "AYRA is typing...") {
                    messagesList.removeAt(temporaryTypingMessageIndex)
                    // No need to call chatAdapter.notifyItemRemoved, submitList will handle it.
                }

                if (aiResponseText != null) {
                    val aiMessage = ChatLog(
                        messageContent = aiResponseText, // Use the actual response
                        isUserMessage = false
                    )
                    addNewMessage(aiMessage)
                } else {
                    Log.e("ChatActivity", "Received null response from LlmInferenceManager.")
                    val errorMessage = ChatLog(
                        messageContent = "Sorry, I couldn't get a response right now.",
                        isUserMessage = false
                    )
                    addNewMessage(errorMessage)
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error getting AI response: ${e.message}", e)
                val errorMessage = ChatLog(
                    messageContent = "An error occurred while getting a response.",
                    isUserMessage = false
                )
                addNewMessage(errorMessage)
            }
        }
    }

    private fun addNewMessage(message: ChatLog) {
        messagesList.add(message)
        chatAdapter.submitList(messagesList.toList()) // Submit a new list for DiffUtil
        recyclerViewChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
    }

    private fun loadSampleMessages() {
        // You might want to clear this or not load samples if you expect fresh chat always
        // messagesList.clear()
        messagesList.addAll(listOf(
            ChatLog(messageContent = "Hello AYRA!", isUserMessage = true, timestamp = System.currentTimeMillis() - 50000),
            ChatLog(messageContent = "Hello! I'm AYRA. How can I assist you today?", isUserMessage = false, timestamp = System.currentTimeMillis() - 40000)
        ))
        chatAdapter.submitList(messagesList.toList())
        if (chatAdapter.itemCount > 0) {
            recyclerViewChat.scrollToPosition(chatAdapter.itemCount - 1)
        }
    }
}
