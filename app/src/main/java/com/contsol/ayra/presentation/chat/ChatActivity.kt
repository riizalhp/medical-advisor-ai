package com.contsol.ayra.presentation.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.toList
import androidx.core.view.isGone
import com.contsol.ayra.data.source.local.database.dao.ChatLogDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatActivity : AppCompatActivity() {

    private lateinit var chatLogDao: ChatLogDao

    private lateinit var recyclerViewChat: RecyclerView
    private lateinit var textViewEmptyChat: TextView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: ImageButton
    private lateinit var buttonAttach: ImageButton
    private lateinit var buttonCamera: ImageButton
    private lateinit var buttonMic: ImageButton
    private lateinit var chatAdapter: ChatAdapter
    private val messagesList = mutableListOf<ChatLog>()
    private lateinit var imageViewAttachmentPreview: ImageView
    private lateinit var buttonRemoveAttachment: ImageButton
    private lateinit var previewContainer: ConstraintLayout

    // For CameraX
    private lateinit var cameraPreviewView: PreviewView
    private lateinit var buttonCaptureImage: ImageButton // Button to take picture in CameraX view
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null

    private var currentAttachmentPath: String? = null

    // For Speech Recognition
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningForSpeech = false
    private val recordAudioPermissionCode = 101
    private val cameraPermissionCode = 102

    // ActivityResultLauncher for the Photo Picker
    private val pickMediaLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                Log.d("PhotoPicker", "Selected URI: $uri")
                currentAttachmentPath = null // Clear previous
                copyUriToAppStorage(uri, "picked_image_${System.currentTimeMillis()}.jpg")?.let { filePath ->
                    currentAttachmentPath = filePath
                    imageViewAttachmentPreview.setImageURI(uri) // Show preview
                    previewContainer.isVisible = true
                    buttonRemoveAttachment.isVisible = true
                    updateSendButtonVisibility()
                    Toast.makeText(this, "Image selected. Add a prompt and send.", Toast.LENGTH_LONG).show()
                } ?: run {
                    Toast.makeText(this, "Failed to prepare image from picker.", Toast.LENGTH_SHORT).show()
                    clearAttachment()
                }
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatLogDao = ChatLogDao(this)

        recyclerViewChat = findViewById(R.id.recyclerViewChat)
        textViewEmptyChat = findViewById(R.id.textViewEmptyChat)
        editTextMessage = findViewById(R.id.editTextMessage)
        buttonSend = findViewById(R.id.buttonSend)
        buttonAttach = findViewById(R.id.buttonAttach)
        buttonCamera = findViewById(R.id.buttonCamera)
        buttonMic = findViewById(R.id.buttonMic)

        // Initialize preview views (assuming you add these to your R.layout.activity_chat)
        previewContainer = findViewById(R.id.attachmentPreviewContainer)
        imageViewAttachmentPreview = findViewById(R.id.imageViewAttachmentPreview)
        buttonRemoveAttachment = findViewById(R.id.buttonRemoveAttachment)

        // CameraX Views
        cameraPreviewView = findViewById(R.id.cameraPreviewView)
        buttonCaptureImage = findViewById(R.id.buttonCaptureImage)

        updateEmptyStateVisibility()
        setupRecyclerView()
        loadChatMessages()
        // loadSampleMessages()
        setupTextChangeListener()
        updateSendButtonVisibility()

        buttonSend.setOnClickListener {
            handleSendButtonClick()
        }

        buttonAttach.setOnClickListener {
            // Launch the photo picker and allow the user to choose only images.
            hideCameraPreview()
            pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        buttonCamera.setOnClickListener {
            handleCameraButtonClick()
        }

        buttonMic.setOnClickListener {
            hideCameraPreview()
            handleMicButtonClick()
        }

        buttonRemoveAttachment.setOnClickListener {
            clearAttachment()
        }

        buttonCaptureImage.setOnClickListener {
            takePhoto()
        }

        if (!LlmInferenceManager.isInitialized()) {
            Toast.makeText(this, "AI is initializing, please wait...", Toast.LENGTH_SHORT).show()
        }

        // Initialize CameraX Executor
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun handleSendButtonClick() {
        val messageText = editTextMessage.text.toString().trim()
        val imagePathForLlm: String? = currentAttachmentPath

        if (messageText.isNotEmpty() || imagePathForLlm != null) {
            if (imagePathForLlm != null && messageText.isNotEmpty()) {
                sendImageMessage(messageText, imagePathForLlm)
                generateAiResponseWithImage(messageText, imagePathForLlm)
                editTextMessage.text.clear()
                clearAttachment()
            } else if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                generateAiResponse(messageText)
                editTextMessage.text.clear()
            } else if (imagePathForLlm != null) {
                Toast.makeText(this, "Please add a text prompt for the image.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Please type a message or attach an image.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleCameraButtonClick() {
        if (allPermissionsGranted()) {
            showCameraPreview()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), cameraPermissionCode
            )
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun showCameraPreview() {
        // Hide chat input, show camera preview and capture button
        editTextMessage.visibility = View.GONE
        buttonSend.visibility = View.GONE
        buttonAttach.visibility = View.GONE
        buttonMic.visibility = View.GONE
        previewContainer.visibility = View.GONE // Hide any existing attachment

        cameraPreviewView.visibility = View.VISIBLE
        buttonCaptureImage.visibility = View.VISIBLE
        startCamera()
    }

    private fun hideCameraPreview() {
        // Show chat input, hide camera preview
        editTextMessage.visibility = View.VISIBLE
        updateSendButtonVisibility() // This will manage send/mic button
        buttonAttach.visibility = View.VISIBLE

        cameraPreviewView.visibility = View.GONE
        buttonCaptureImage.visibility = View.GONE
        // Stop the camera
        cameraProvider?.unbindAll()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(cameraPreviewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) // Or .CAPTURE_MODE_MAXIMIZE_QUALITY
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider?.unbindAll()

                // Bind use cases to camera
                cameraProvider?.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
                Log.d("CameraX", "Camera started successfully")

            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
                Toast.makeText(this, "Failed to start camera.", Toast.LENGTH_SHORT).show()
                hideCameraPreview()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped name and MediaStore entry.
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(System.currentTimeMillis())

        // Create output options object which contains file + metadata
        // For saving to app's cache directory instead of MediaStore:
        val outputDirectory = File(cacheDir, "captures")
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }
        val photoFile = File(outputDirectory, "AYRA_IMG_$name.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()


        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraX", "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(baseContext, "Photo capture failed: ${exc.localizedMessage}", Toast.LENGTH_SHORT).show()
                    hideCameraPreview() // Or allow retry
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile) // Fallback if savedUri is null (though with File, it should be)
                    Log.d("CameraX", "Photo capture succeeded: $savedUri")
                    Log.d("CameraX", "Photo saved to path: ${photoFile.absolutePath}")

                    currentAttachmentPath = photoFile.absolutePath // Store path for LLM

                    imageViewAttachmentPreview.setImageURI(savedUri) // Show preview in your existing view
                    previewContainer.isVisible = true
                    buttonRemoveAttachment.isVisible = true
                    updateSendButtonVisibility()
                    Toast.makeText(baseContext, "Image captured!", Toast.LENGTH_SHORT).show()
                    hideCameraPreview() // Go back to chat input
                }
            }
        )
    }

    private fun handleMicButtonClick() {
        if (!isListeningForSpeech) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startSpeechToText()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), recordAudioPermissionCode)
            }
        } else {
            stopSpeechToText()
        }
    }

    private fun initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition is not available on this device.", Toast.LENGTH_LONG).show()
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d("SpeechRecognizer", "Ready for speech")
                    // You could change mic icon here to indicate listening
                    buttonMic.setImageResource(R.drawable.stop_circle_24px) // Example: use a different icon
                    isListeningForSpeech = true
                    Toast.makeText(this@ChatActivity, "Listening...", Toast.LENGTH_SHORT).show()
                }

                override fun onBeginningOfSpeech() {
                    Log.d("SpeechRecognizer", "Beginning of speech")
                }

                override fun onRmsChanged(rmsdB: Float) {} // Voice level changed

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    Log.d("SpeechRecognizer", "End of speech")
                    isListeningForSpeech = false
                    buttonMic.setImageResource(R.drawable.ic_mic) // Reset mic icon
                }

                override fun onError(error: Int) {
                    Log.e("SpeechRecognizer", "Error: $error")
                    isListeningForSpeech = false
                    buttonMic.setImageResource(R.drawable.ic_mic) // Reset mic icon
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                        SpeechRecognizer.ERROR_SERVER -> "Error from server"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                        else -> "Unknown speech recognition error"
                    }
                    Toast.makeText(this@ChatActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val recognizedText = matches[0] // Get the most likely result
                        Log.d("SpeechRecognizer", "Recognized text: $recognizedText")
                        editTextMessage.setText(recognizedText) // Set text to EditText
                        editTextMessage.setSelection(recognizedText.length) // Move cursor to end

                        // Optionally, send the message directly after recognition
                        // handleSendMessage()
                        // Or wait for user to press send
                    }
                    isListeningForSpeech = false
                    buttonMic.setImageResource(R.drawable.ic_mic) // Reset mic icon
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        editTextMessage.setText(matches[0]) // Show partial results in EditText
                        editTextMessage.setSelection(matches[0].length)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun startSpeechToText() {
        if (speechRecognizer == null) {
            initializeSpeechRecognizer()
        }
        if (speechRecognizer != null && SpeechRecognizer.isRecognitionAvailable(this)) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()) // Or specify a language
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) // For live updates in EditText
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...") // Optional prompt
            }
            speechRecognizer?.startListening(intent)
        } else {
            Toast.makeText(this, "Speech recognition not initialized or not available.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopSpeechToText() {
        speechRecognizer?.stopListening()
        isListeningForSpeech = false
        buttonMic.setImageResource(R.drawable.ic_mic) // Reset mic icon
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            recordAudioPermissionCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startSpeechToText()
                } else {
                    Toast.makeText(this, "Audio recording permission denied.", Toast.LENGTH_SHORT).show()
                }
            }
            cameraPermissionCode -> {
                if (allPermissionsGranted()) {
                    showCameraPreview()
                } else {
                    Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show()
                    hideCameraPreview() // Ensure camera view is hidden if permission denied
                }
            }
        }
    }

    private fun setupTextChangeListener() {
        editTextMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed for this use case
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Called when text is changing
                updateSendButtonVisibility()
            }

            override fun afterTextChanged(s: Editable?) {
                // Not needed for this use case
            }
        })
    }

    private fun updateEmptyStateVisibility() {
        val isEmpty = messagesList.isEmpty()

        if (isEmpty) {
            recyclerViewChat.visibility = View.GONE
            textViewEmptyChat.visibility = View.VISIBLE
        } else {
            recyclerViewChat.visibility = View.VISIBLE
            textViewEmptyChat.visibility = View.GONE
        }
    }

    private fun updateSendButtonVisibility() {
        val messageText = editTextMessage.text.toString().trim()
        val hasAttachment = currentAttachmentPath != null
        // Ensure camera UI is not active when updating these
        if (cameraPreviewView.isGone) {
            if (messageText.isNotEmpty() || hasAttachment) {
                buttonSend.isVisible = true
                buttonMic.isVisible = false
            } else {
                buttonSend.isVisible = false
                buttonMic.isVisible = true
            }
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
        Log.d("ChatActivity", "Sending image message with path: ${message.imageUrl}")
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
                Log.d("ChatActivity", "Requesting AI response for: $originalMessage")
                val aiResponseText = LlmInferenceManager.runWithRag(originalMessage)
                removeTypingIndicator()
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

    private var isTypingIndicatorVisible: Boolean = false
    private fun showTypingIndicator() {
        if (isTypingIndicatorVisible) {
            return // Already showing
        }

        // Remove any existing typing indicator first (defensive)
        removeTypingIndicatorLogic()

        val typingMessage = ChatLog(
            messageContent = "AYRA sedang berpikir...",
            isUserMessage = false,
            timestamp = System.currentTimeMillis() // Give it a timestamp for sorting if needed temporarily
            // Add a temporary flag if you prefer: isTypingIndicator = true (you'd need to add this field to ChatLog, non-persistent)
        )
        messagesList.add(typingMessage)
        chatAdapter.submitList(messagesList.toList()) // Make sure your adapter handles new list submissions correctly
        recyclerViewChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
        isTypingIndicatorVisible = true
    }

    private fun removeTypingIndicatorLogic() {
        val currentList = messagesList.toMutableList() // Work on a copy to avoid concurrent modification issues
        var listChanged = false

        // Iterate backwards to safely remove items
        for (i in currentList.indices.reversed()) {
            if (currentList[i].messageContent == "AYRA sedang berpikir..." && !currentList[i].isUserMessage) {
                // Found the typing indicator
                currentList.removeAt(i)
                listChanged = true
                // break // Assuming only one typing indicator can exist
            }
        }

        if (listChanged) {
            messagesList.clear()
            messagesList.addAll(currentList)
            chatAdapter.submitList(messagesList.toList())
        }
    }


    private fun removeTypingIndicator() {
        if (!isTypingIndicatorVisible) {
            return
        }
        removeTypingIndicatorLogic()
        isTypingIndicatorVisible = false
    }

    private fun addNewMessage(message: ChatLog) {
        lifecycleScope.launch(Dispatchers.IO) {
            val insertedId = chatLogDao.insert(message)
            if (insertedId > -1) { // Successfully inserted
                withContext(Dispatchers.Main) {
                    messagesList.add(message)
                    chatAdapter.submitList(messagesList.toList())
                    updateEmptyStateVisibility()
                    recyclerViewChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
                }
            } else {
                // Handle insertion error (e.g., show a toast)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "Error sending message", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadChatMessages() {
        lifecycleScope.launch(Dispatchers.IO) {
            val loadedMessages = chatLogDao.getAll()
            withContext(Dispatchers.Main) {
                messagesList.clear()
                messagesList.addAll(loadedMessages)
                chatAdapter.submitList(messagesList.toList())
                updateEmptyStateVisibility()
                if (messagesList.isNotEmpty()) {
                    recyclerViewChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
                }
                isTypingIndicatorVisible = false
            }
        }
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
        currentAttachmentPath?.let { /* path ->
            // Optionally delete the file if it's from CameraX and stored in cache
            if (path.contains(cacheDir.absolutePath)) { // Basic check
                File(path).delete()
            } */
        }
        currentAttachmentPath = null

        imageViewAttachmentPreview.setImageURI(null)
        previewContainer.isVisible = false
        buttonRemoveAttachment.isVisible = false
        updateSendButtonVisibility()
        Log.d("ChatActivity", "Attachment cleared.")
    }

    private fun copyUriToAppStorage(uri: Uri, fileName: String): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val outputDir = File(cacheDir, "attachments") // Store in a subdirectory
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            val file = File(outputDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Log.d("Storage", "Copied URI $uri to ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e("Storage", "Error copying URI to app storage", e)
            null
        }
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        cameraExecutor.shutdown()
        super.onDestroy()
    }
}
