package com.contsol.ayra.presentation.chat

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope // Import for lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.contsol.ayra.R
import com.contsol.ayra.data.ai.LlmInferenceManager // Import LlmInferenceManager
import com.contsol.ayra.data.source.local.database.model.ChatLog
import kotlinx.coroutines.launch // Import launch
import java.io.File
import java.io.FileOutputStream
import kotlin.collections.toList

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerViewChat: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: ImageButton
    private lateinit var buttonAttach: ImageButton
    private lateinit var buttonCamera: ImageButton
    private lateinit var chatAdapter: ChatAdapter
    private val messagesList = mutableListOf<ChatLog>()

    // For displaying the selected image thumbnail (optional, but good UX)
    private lateinit var imageViewAttachmentPreview: ImageView
    private lateinit var buttonRemoveAttachment: ImageButton

    private var selectedImageUri: Uri? = null
    private var selectedImageFilePath: String? = null // To store the path of the copied image

    // ActivityResultLauncher for the Photo Picker
    private val pickMediaLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                Log.d("PhotoPicker", "Selected URI: $uri")
                selectedImageUri = uri
                // Copy the selected image to app's internal storage to get a stable path
                // LlmInferenceManager.runWithImage expects a file path.
                copyUriToInternalStorage(uri)?.let { filePath ->
                    selectedImageFilePath = filePath
                    imageViewAttachmentPreview.setImageURI(uri) // Show preview
                    imageViewAttachmentPreview.isVisible = true
                    buttonRemoveAttachment.isVisible = true

                    val previewContainer = findViewById<ConstraintLayout>(R.id.attachmentPreviewContainer)
                    previewContainer.isVisible = true
                    Log.d("ChatActivity", "Preview Container Visible: ${previewContainer.isVisible}, Height: ${previewContainer.height}")

                    Toast.makeText(this, "Image selected. Add a prompt and send.", Toast.LENGTH_LONG).show()
                } ?: run {
                    Toast.makeText(this, "Failed to prepare image.", Toast.LENGTH_SHORT).show()
                    clearAttachment()

                    val previewContainer = findViewById<ConstraintLayout>(R.id.attachmentPreviewContainer)
                    previewContainer.isVisible = false
                    Log.d("ChatActivity", "Preview Container Cleared. Visible: ${previewContainer.isVisible}")
                }
            } else {
                Log.d("PhotoPicker", "No media selected")
                // Optionally clear attachment if user cancels picker after selecting one before
                // if (selectedImageUri != null) { // Only clear if there was a previous selection
                //     clearAttachment()
                // }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        recyclerViewChat = findViewById(R.id.recyclerViewChat)
        editTextMessage = findViewById(R.id.editTextMessage)
        buttonSend = findViewById(R.id.buttonSend)
        buttonAttach = findViewById(R.id.buttonAttach)
        buttonCamera = findViewById(R.id.buttonCamera)

        // Initialize preview views (assuming you add these to your R.layout.activity_chat)
        imageViewAttachmentPreview = findViewById(R.id.imageViewAttachmentPreview)
        buttonRemoveAttachment = findViewById(R.id.buttonRemoveAttachment)


        setupRecyclerView()
        loadSampleMessages()

        buttonSend.setOnClickListener {
            val messageText = editTextMessage.text.toString().trim()
            if (messageText.isNotEmpty() || selectedImageFilePath != null) { // Allow sending if text OR image
                if (selectedImageFilePath != null && messageText.isNotEmpty()) {
                    // Send message with image
                    sendImageMessage(messageText, selectedImageFilePath!!) // Pass the path
                    editTextMessage.text.clear()
                    generateAiResponseWithImage(messageText, selectedImageFilePath!!)
                    clearAttachment()
                } else if (messageText.isNotEmpty()) {
                    // Send text-only message
                    sendMessage(messageText)
                    editTextMessage.text.clear()
                    generateAiResponse(messageText)
                } else if (selectedImageFilePath != null) {
                    // Send image-only message (or prompt user for text)
                    Toast.makeText(this, "Please add a text prompt for the image.", Toast.LENGTH_SHORT).show()
                    // Alternatively, you could send a default prompt or allow image-only messages
                    // if your AI and UI are designed for it.
                    // For now, let's assume text is needed with an image.
                }
            } else {
                Toast.makeText(this, "Please type a message or attach an image.", Toast.LENGTH_SHORT).show()
            }
        }

        buttonAttach.setOnClickListener {
            // Launch the photo picker and allow the user to choose only images.
            pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        buttonCamera.setOnClickListener {
            Toast.makeText(this, "Button camera clicked", Toast.LENGTH_SHORT).show()
            // TODO: Implement camera capture
        }

        buttonRemoveAttachment.setOnClickListener {
            clearAttachment()
        }

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

    // New function to handle sending a message that includes an image (path)
    private fun sendImageMessage(text: String, imagePath: String) {
        val message = ChatLog(
            messageContent = text,
            imageUrl = imagePath, // Store the image path
            isUserMessage = true,
            timestamp = System.currentTimeMillis()
        )
        addNewMessage(message)
        // You'll need to update your ChatAdapter to display this image
    }


    private fun generateAiResponse(originalMessage: String) {
        if (!LlmInferenceManager.isInitialized()) {
            Log.w("ChatActivity", "LLM not initialized. Cannot generate AI response yet.")
            addNewMessage(ChatLog(messageContent = "AYRA is still initializing...", isUserMessage = false))
            return
        }
        lifecycleScope.launch {
            showTypingIndicator()
            try {
                val aiResponseText = LlmInferenceManager.run(originalMessage) ?: "Sorry, I couldn't process that."
                removeTypingIndicator()
                addNewMessage(ChatLog(messageContent = aiResponseText, isUserMessage = false))
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error getting AI response: ${e.message}", e)
                removeTypingIndicator()
                addNewMessage(ChatLog(messageContent = "An error occurred.", isUserMessage = false))
            }
        }
    }

    // New function to generate AI response with an image
    private fun generateAiResponseWithImage(prompt: String, imagePath: String) {
        if (!LlmInferenceManager.isInitialized()) {
            Log.w("ChatActivity", "LLM not initialized for image response.")
            addNewMessage(ChatLog(messageContent = "AYRA is still initializing for images...", isUserMessage = false))
            return
        }
        lifecycleScope.launch {
            showTypingIndicator()
            try {
                Log.d("ChatActivity", "Requesting AI response for prompt: '$prompt' with image: $imagePath")
                val aiResponseText = LlmInferenceManager.runWithImage(prompt, imagePath) ?: "Sorry, I couldn't process that image and prompt."
                removeTypingIndicator()
                addNewMessage(ChatLog(messageContent = aiResponseText, isUserMessage = false))
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error getting AI response with image: ${e.message}", e)
                removeTypingIndicator()
                addNewMessage(ChatLog(messageContent = "An error occurred with the image.", isUserMessage = false))
            }
        }
    }

    private var typingIndicatorIndex: Int = -1
    private fun showTypingIndicator() {
        if (typingIndicatorIndex != -1 && messagesList.getOrNull(typingIndicatorIndex)?.messageContent == "AYRA is typing...") {
            return // Already showing
        }
        val typingMessage = ChatLog(messageContent = "AYRA is typing...", isUserMessage = false)
        messagesList.add(typingMessage)
        typingIndicatorIndex = messagesList.size - 1
        chatAdapter.submitList(messagesList.toList())
        recyclerViewChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
    }

    private fun removeTypingIndicator() {
        if (typingIndicatorIndex != -1 && messagesList.getOrNull(typingIndicatorIndex)?.messageContent == "AYRA is typing...") {
            messagesList.removeAt(typingIndicatorIndex)
            chatAdapter.submitList(messagesList.toList()) // Update the adapter with the new list
        }
        typingIndicatorIndex = -1
    }


    private fun addNewMessage(message: ChatLog) {
        messagesList.add(message)
        chatAdapter.submitList(messagesList.toList())
        recyclerViewChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
    }

    private fun loadSampleMessages() {
        messagesList.addAll(listOf(
            ChatLog(messageContent = "Hello AYRA!", isUserMessage = true, timestamp = System.currentTimeMillis() - 50000),
            ChatLog(messageContent = "Hello! I'm AYRA. How can I assist you today?", isUserMessage = false, timestamp = System.currentTimeMillis() - 40000)
        ))
        chatAdapter.submitList(messagesList.toList())
        if (chatAdapter.itemCount > 0) {
            recyclerViewChat.scrollToPosition(chatAdapter.itemCount - 1)
        }
    }

    private fun clearAttachment() {
        selectedImageUri = null
        selectedImageFilePath = null
        imageViewAttachmentPreview.setImageURI(null) // Clear preview
        imageViewAttachmentPreview.isVisible = false
        buttonRemoveAttachment.isVisible = false
        Log.d("ChatActivity", "Attachment cleared.")
    }

    private fun copyUriToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val timestamp = System.currentTimeMillis()
            val file = File(filesDir, "attachment_${timestamp}.jpg") // Create a unique file name
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("ChatActivity", "Error copying URI to internal storage", e)
            null
        }
    }
}
