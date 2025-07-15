package com.contsol.ayra.data.ai

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object LlmInferenceManager {
    private var llmInference: LlmInference? = null
    private var isInitializing = false
    private val initializationMutex = Mutex() // To prevent concurrent initialization attempts
    private var initializationListeners = mutableListOf<() -> Unit>() // To queue listeners if init is in progress

    // --- Configuration ---
    // Option 1: Download model (uncomment the URL and choose this option in initializeIfNeeded)
    // private const val MODEL_DOWNLOAD_URL = "YOUR_MODEL_DOWNLOAD_URL_HERE" // Replace if using download

    // Option 2: Model from assets (ensure this matches the name in your assets folder)
    private const val MODEL_ASSET_NAME = "gemma-3n-E2B-it-int4.task"
    private const val MODEL_FILE_NAME = "gemma-3n-E2B-it-int4.task" // Name for the file in internal storage

    /**
     * Initializes the LlmInference engine if it hasn't been already.
     * This method handles model acquisition (download or copy from assets) and setup.
     *
     * @param context Application context.
     * @param onInitialized Optional callback that is invoked on the main thread
     *                      once initialization is successful.
     */
    fun initializeIfNeeded(context: Context, onInitialized: () -> Unit = {}) {
        CoroutineScope(Dispatchers.Main).launch { // Launch on Main, but heavy work will be on IO
            initializationMutex.withLock {
                if (isInitialized()) {
                    Log.d("LlmInferenceManager", "Already initialized.")
                    onInitialized()
                    return@launch
                }
                if (isInitializing) {
                    Log.d("LlmInferenceManager", "Initialization already in progress. Adding listener.")
                    initializationListeners.add(onInitialized)
                    return@launch
                }
                isInitializing = true
            }

            Log.d("LlmInferenceManager", "Starting model initialization...")

            // Choose your model acquisition strategy:
            // val modelFile = acquireModelByDownloading(context.applicationContext) // Option 1: Download
            val modelFile = acquireModelFromAssets(context.applicationContext)    // Option 2: Copy from Assets

            if (modelFile != null && modelFile.exists()) {
                try {
                    llmInference = withContext(Dispatchers.IO) { // LlmInference.createFromOptions can be blocking
                        val taskOptions = LlmInference.LlmInferenceOptions.builder()
                            .setModelPath(modelFile.absolutePath)
                            .setMaxTopK(64) // Example: from previous usage
                            // .setTopK(1) // Common for some models, adjust as needed
                            // .setTemperature(0.7f) // Adjust creativity
                            // .setRandomSeed(101)   // For reproducibility
                            .build()
                        LlmInference.createFromOptions(context.applicationContext, taskOptions)
                    }
                    Log.i("LlmInferenceManager", "LlmInference initialized successfully.")
                    initializationMutex.withLock {
                        isInitializing = false
                        onInitialized()
                        initializationListeners.forEach { it() }
                        initializationListeners.clear()
                    }
                } catch (e: Exception) {
                    Log.e("LlmInferenceManager", "Failed to initialize LlmInference: ${e.message}", e)
                    initializationMutex.withLock { isInitializing = false }
                    // Optionally notify listeners about failure here
                }
            } else {
                Log.e("LlmInferenceManager", "Model file not available. Initialization failed.")
                initializationMutex.withLock { isInitializing = false }
                // Optionally notify listeners about failure here
            }
        }
    }

    /**
     * Option 1: Acquires the model by downloading it if it doesn't exist.
     */
    @Suppress("unused") // Keep if you might switch to downloading
    private suspend fun acquireModelByDownloading(context: Context): File? {
        val modelFile = File(context.filesDir, MODEL_FILE_NAME)
        val modelDownloadUrl = "YOUR_MODEL_DOWNLOAD_URL_HERE" // ** IMPORTANT: SET THIS IF DOWNLOADING **

        if (modelDownloadUrl.isNotEmpty()) {
            Log.e("LlmInferenceManager", "MODEL_DOWNLOAD_URL is not set. Cannot download model.")
            return null
        }

        if (modelFile.exists() && modelFile.length() > 0) { // Check length to ensure it's not an empty/failed download
            Log.d("LlmInferenceManager", "Model already exists at ${modelFile.absolutePath}")
            return modelFile
        }

        Log.d("LlmInferenceManager", "Downloading model to ${modelFile.absolutePath}...")
        return try {
            withContext(Dispatchers.IO) {
                val url = URL(modelDownloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000 // 15 seconds
                connection.readTimeout = 15000  // 15 seconds
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.use { input ->
                        FileOutputStream(modelFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d("LlmInferenceManager", "Model downloaded successfully.")
                    modelFile
                } else {
                    Log.e("LlmInferenceManager", "Model download failed. Server responded with ${connection.responseCode}")
                    modelFile.delete() // Clean up
                    null
                }
            }
        } catch (e: IOException) {
            Log.e("LlmInferenceManager", "Model download failed: ${e.message}", e)
            modelFile.delete() // Clean up
            null
        }
    }

    /**
     * Option 2: Acquires the model by copying it from the app's assets folder.
     */
    private suspend fun acquireModelFromAssets(context: Context): File? {
        val modelFile = File(context.filesDir, MODEL_FILE_NAME)

        // Avoid re-copying if it already exists and seems valid (e.g. check size or a version flag)
        // For simplicity here, we'll just check existence. For production, you might add versioning.
        if (modelFile.exists() && modelFile.length() > 0) {
            Log.d("LlmInferenceManager", "Model already exists in internal storage (from assets): ${modelFile.absolutePath}")
            return modelFile
        }

        Log.d("LlmInferenceManager", "Copying model from assets to ${modelFile.absolutePath}...")
        return try {
            withContext(Dispatchers.IO) {
                context.assets.open(MODEL_ASSET_NAME).use { inputStream ->
                    FileOutputStream(modelFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Log.d("LlmInferenceManager", "Model copied from assets successfully.")
                modelFile
            }
        } catch (e: IOException) {
            Log.e("LlmInferenceManager", "Failed to copy model from assets: ${e.message}", e)
            modelFile.delete() // Clean up partially copied file if error occurs
            null
        }
    }


    fun isInitialized(): Boolean = llmInference != null

    /**
     * Generates a response from the LLM based on the given prompt.
     * This is a suspend function and should be called from a coroutine.
     *
     * @param prompt The input string to the LLM.
     * @return The LLM's response as a String, or null if an error occurred or not initialized.
     */
    suspend fun run(prompt: String): String? = withContext(Dispatchers.IO) {
        if (!isInitialized()) {
            Log.e("LlmInferenceManager", "LLM not initialized. Call initializeIfNeeded() first.")
            return@withContext "Error: AYRA is not ready yet. Please wait for initialization."
        }
        try {
            Log.d("LlmInferenceManager", "Generating response for prompt: \"$prompt\"")
            val response = llmInference?.generateResponse(prompt)
            Log.d("LlmInferenceManager", "LLM Raw Response: \"$response\"")
            response
        } catch (e: Exception) {
            Log.e("LlmInferenceManager", "Error generating LLM response: ${e.message}", e)
            "Error: I encountered a problem trying to respond."
        }
    }

    /**
     * Closes the LlmInference engine and releases resources.
     * Should be called when the LLM is no longer needed (e.g., in Application.onTerminate).
     */
    fun close() {
        Log.d("LlmInferenceManager", "Closing LlmInference...")
        try {
            llmInference?.close()
        } catch (e: Exception) {
            Log.e("LlmInferenceManager", "Error closing LlmInference: ${e.message}", e)
        } finally {
            llmInference = null
            Log.d("LlmInferenceManager", "LlmInference resources released.")
        }
    }
}
