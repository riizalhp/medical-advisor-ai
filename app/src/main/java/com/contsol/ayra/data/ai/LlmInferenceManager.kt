package com.contsol.ayra.data.ai

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.contsol.ayra.data.source.rag.RagPipeline
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession.LlmInferenceSessionOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object LlmInferenceManager {
    private var llmInference: LlmInference? = null
    private var isInitializing = false
    private val initializationMutex = Mutex() // To prevent concurrent initialization attempts
    private var initializationListeners = mutableListOf<() -> Unit>() // To queue listeners if init is in progress
    private var ragPipeline: RagPipeline? = null

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
        CoroutineScope(Dispatchers.Main).launch {
            initializationMutex.withLock {
                if (isInitialized()) {
                    onInitialized()
                    return@launch
                }
                if (isInitializing) {
                    initializationListeners.add(onInitialized)
                    return@launch
                }
                isInitializing = true
            }

            Log.d("LlmInferenceManager", "Starting model initialization...")
            val modelFile = acquireModelFromAssets(context.applicationContext)

            if (modelFile != null && modelFile.exists()) {
                Log.d("LlmInferenceManager", "Model Path: ${modelFile.absolutePath}")
                try {
                    llmInference = withContext(Dispatchers.IO) {
                        val taskOptions = LlmInference.LlmInferenceOptions.builder()
                            .setModelPath(modelFile.absolutePath)
                            .setMaxNumImages(1)
                            // .setMaxTopK(64) // Moved to session options as per reference
                            // Vision modality might also be enabled here for some models globally,
                            // but your reference shows it at the session level.
                            // .setGraphOptions(GraphOptions.newBuilder().setEnableVisionModality(true).build()) // Example global setting
                            .build()
                        LlmInference.createFromOptions(context.applicationContext, taskOptions)
                    }
                    Log.i("LlmInferenceManager", "LlmInference initialized successfully.")
                    ragPipeline = RagPipeline(context.applicationContext as Application, modelFile.absolutePath)
                    ragPipeline?.memorizeChunks(context, "knowledge/knowledge-base.txt")
                    if (ragPipeline != null) {
                        Log.i("LlmInferenceManager", "RAG Pipeline initialized successfully.")
                    }
                    initializationMutex.withLock {
                        isInitializing = false
                        onInitialized()
                        initializationListeners.forEach { it() }
                        initializationListeners.clear()
                    }

                } catch (e: Exception) {
                    Log.e("LlmInferenceManager", "Failed to initialize LlmInference: ${e.message}", e)
                    initializationMutex.withLock { isInitializing = false }
                }
            } else {
                Log.e("LlmInferenceManager", "Model file not available. Initialization failed.")
                initializationMutex.withLock { isInitializing = false }
            }
        }
    }

    private suspend fun acquireModelFromAssets(context: Context): File? {
        val modelFile = File(context.filesDir, MODEL_FILE_NAME)
        // Activate lines below to delete existing model if it exists and re-copy from assets (use when existing model corrupted)
        if (modelFile.exists()) {
            Log.d("LlmInferenceManager", "Existing model found at ${modelFile.absolutePath}. Deleting it to force re-copy from assets.")
            val deleted = withContext(Dispatchers.IO) {
                try { modelFile.delete() } catch (e: Exception) {
                    Log.e("LlmInferenceManager", "Failed to delete existing model: ${e.message}", e); false
                }
            }
            if (!deleted && modelFile.exists()) Log.w("LlmInferenceManager", "Could not delete existing model.")
            else if (deleted) Log.d("LlmInferenceManager", "Successfully deleted existing model.")
        }
        Log.d("LlmInferenceManager", "Copying model from assets to ${modelFile.absolutePath}...")
        return try {
            withContext(Dispatchers.IO) {
                context.assets.open(MODEL_ASSET_NAME).use { input -> FileOutputStream(modelFile).use { output -> input.copyTo(output) } }
                Log.d("LlmInferenceManager", "Model copied from assets successfully.")
                modelFile
            }
        } catch (e: IOException) {
            Log.e("LlmInferenceManager", "Failed to copy model from assets: ${e.message}", e)
            if (modelFile.exists()) modelFile.delete()
            null
        }
    }

    fun isInitialized(): Boolean = llmInference != null

    suspend fun run(prompt: String): String = withContext(Dispatchers.IO) {
        if (!isInitialized()) {
            return@withContext "Error: AYRA is not ready yet. Please wait for initialization."
        }
        try {
            // For simple text, LlmInference.generateResponse might be simpler
            // or you can use a session here too for consistency.
            // val response = llmInference?.generateResponse(prompt)

            // Using session for text for consistency (optional)
            val sessionOptions = LlmInferenceSessionOptions.builder()
                // .setTopK(64) // Example, adjust as needed
                // .setTemperature(0.7f) // Example
                .build()

            llmInference?.let { inferenceInstance ->
                val session = LlmInferenceSession.createFromOptions(inferenceInstance, sessionOptions)
                session.use { // Use try-with-resources for the session
                    it.addQueryChunk(prompt)
                    val response = it.generateResponse()
                    Log.d("LlmInferenceManager", "LLM Raw Response: \"$response\"")
                    response
                }
            } ?: "Error: LlmInference instance is null."

        } catch (e: Exception) {
            Log.e("LlmInferenceManager", "Error generating LLM response: ${e.message}", e)
            "Error: I encountered a problem trying to respond."
        }
    }

    suspend fun runWithImage(prompt: String, imagePath: String): String? = withContext(Dispatchers.IO) {
        if (!isInitialized()) {
            Log.e("LlmInferenceManager", "LLM not initialized. Call initializeIfNeeded() first.")
            return@withContext "Error: AYRA is not ready yet. Please wait for initialization."
        }
        if (ragPipeline == null) { // <<< ADD THIS CHECK
            Log.e("LlmInferenceManager", "ERROR: ragPipeline is NULL before calling generateResponse!")
            return@withContext "Error: RAG pipeline not ready."
        }
        try {
            Log.d("LlmInferenceManager", "Generating response using RAG for prompt: \"$prompt\"")
            val response = ragPipeline?.generateResponse(prompt, null) // Safe call still good
            Log.d("LlmInferenceManager", "LLM + RAG Response: \"$response\"")
            response
        } catch (e: Exception) {
            Log.e("LlmInferenceManager", "Error generating LLM response: ${e.message}", e)
            "Error: I encountered a problem trying to respond."
        }

        val currentLlmInference = llmInference ?: run {
            Log.e("LlmInferenceManager", "LlmInference instance is null in runWithImage.")
            return@withContext "Error: LlmInference instance is null."
        }

        try {
            Log.d("LlmInferenceManager", "Generating response for prompt: \"$prompt\" with image: $imagePath")

            val bitmap: Bitmap = BitmapFactory.decodeFile(imagePath) ?: run {
                Log.e("LlmInferenceManager", "Bitmap could not be decoded from path: $imagePath")
                return@withContext "Error: Could not load the image."
            }

            // Create MPImage from Bitmap
            val mpImage: MPImage = try {
                BitmapImageBuilder(bitmap).build()
            } catch (e: Exception) {
                Log.e("LlmInferenceManager", "Error building MPImage: ${e.message}", e)
                bitmap.recycle() // Recycle bitmap if MPImage creation fails
                return@withContext "Error: Could not create MPImage."
            }

            // Session options, enabling vision modality as per your reference
            val sessionOptions = LlmInferenceSessionOptions.builder()
                .setTopK(10) // From your reference
                .setTemperature(0.4f) // From your reference
                .setGraphOptions(GraphOptions.builder().setEnableVisionModality(true).build())
                .build()

            // Create a session, add query and image, then generate response
            val session = LlmInferenceSession.createFromOptions(currentLlmInference, sessionOptions)
            val response = session.use { llmSession -> // .use ensures session is closed
                llmSession.addQueryChunk(prompt)
                llmSession.addImage(mpImage)
                val result = llmSession.generateResponse()
                Log.d("LlmInferenceManager", "LLM Raw Response with image: \"$result\"")
                result
            }

            // Recycle the bitmap after use, as MPImage might hold a reference or copy.
            // If BitmapImageBuilder.build() copies the data, this is safe.
            // If it holds a direct reference and the session is done, it should also be safe.
            // For safety, recycle if you created it and are done with it.
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
            // MPImage does not have a recycle() method. Its resources are managed internally
            // and by the session.

            response

        } catch (e: Exception) {
            Log.e("LlmInferenceManager", "Error generating LLM response with image: ${e.message}", e)
            "Error: I encountered a problem trying to respond with the image."
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
            ragPipeline = null
            Log.d("LlmInferenceManager", "LlmInference resources released.")
            Log.d("LlmInferenceManager", "RAG Pipeline resources released.")
        }
    }
}
