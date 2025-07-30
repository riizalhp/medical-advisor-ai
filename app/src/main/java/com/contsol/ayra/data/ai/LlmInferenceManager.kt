package com.contsol.ayra.data.ai

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.contsol.ayra.data.source.rag.RagPipeline
import com.contsol.ayra.data.state.InitializationState
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

    private const val DB_ASSET_FOLDER = "database" // Folder di dalam assets
    private const val DB_ASSET_NAME = "knowledge_base.db" // Nama file DB di assets
    private const val DB_FILE_NAME = "knowledge_base.db"  // Nama file di storage internal
    /**
     * Initializes the LlmInference engine if it hasn't been already.
     * This method handles model acquisition (download or copy from assets) and setup.
     *
     * @param context Application context.
     * @param onInitialized Optional callback that is invoked on the main thread
     *                      once initialization is successful.
     */
    fun initializeIfNeeded(
        context: Context,
        onProgress: (InitializationState) -> Unit,
        onInitialized: () -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            initializationMutex.withLock {
                if (isInitialized()) {
                    onProgress(InitializationState.Complete)
                    onInitialized()
                    return@launch
                }
                if (isInitializing) {
                    initializationListeners.add(onInitialized)
                    return@launch
                }
                isInitializing = true
            }

            onProgress(InitializationState.NotStarted)

            onProgress(InitializationState.CopyingModel(0)) // Start model copy
            val modelFile = acquireModelFromAssets(context.applicationContext) { progressPercentage ->
                onProgress(InitializationState.CopyingModel(progressPercentage))
            }

            if (modelFile == null || !modelFile.exists()) {
                Log.e("LlmInferenceManager", "Model file not available. Initialization failed.")
                val errorMsg = "Failed to acquire model."
                onProgress(InitializationState.Error(errorMsg))
                initializationMutex.withLock { isInitializing = false }
                // Potentially notify onInitialized with an error state if you adapt it
                return@launch
            }
            onProgress(InitializationState.CopyingModel(100)) // Finish model copy

            // Database Acquisition Progress
            onProgress(InitializationState.CopyingDatabase(0)) // Start DB copy
            val databaseFile = acquireDatabaseFromAssets(context.applicationContext) { progressPercentage ->
                onProgress(InitializationState.CopyingDatabase(progressPercentage))
            }

            if (databaseFile == null || !databaseFile.exists()) {
                Log.e("LlmInferenceManager", "Database file not available. Initialization failed.")
                val errorMsg = "Failed to acquire database."
                onProgress(InitializationState.Error(errorMsg))
                initializationMutex.withLock { isInitializing = false }
                return@launch
            }
            onProgress(InitializationState.CopyingDatabase(100)) // Finish DB copy

            onProgress(InitializationState.InitializingLlm)
            if (modelFile.exists()) {
                Log.d("LlmInferenceManager", "Model Path: ${modelFile.absolutePath}")
                try {
                    val taskOptions = LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(modelFile.absolutePath)
                        .setMaxNumImages(1)
                        // .setMaxTopK(64) // Moved to session options as per reference
                        // Vision modality might also be enabled here for some models globally,
                        // but your reference shows it at the session level.
                        // .setGraphOptions(GraphOptions.newBuilder().setEnableVisionModality(true).build()) // Example global setting
                        .setPreferredBackend(LlmInference.Backend.CPU)
                        .setMaxTokens(1024)
                        .build()

                    llmInference = withContext(Dispatchers.IO) {
                        LlmInference.createFromOptions(context.applicationContext, taskOptions)
                    }
                    Log.i("LlmInferenceManager", "LlmInference initialized successfully.")

                    onProgress(InitializationState.InitializingRag)
                    ragPipeline = RagPipeline(context.applicationContext as Application, taskOptions)
                    ragPipeline?.memorizeChunks(context, "knowledge/knowledge-1.txt")
                    if (ragPipeline != null) {
                        Log.i("LlmInferenceManager", "RAG Pipeline initialized successfully.")
                    }
                    initializationMutex.withLock {
                        isInitializing = false
                        onInitialized()
                        initializationListeners.forEach { it() }
                        initializationListeners.clear()
                    }
                    onProgress(InitializationState.Complete)
                } catch (e: Exception) {
                    Log.e("LlmInferenceManager", "Failed to initialize LlmInference: ${e.message}", e)
                    val errorMsg = "Error during LLM/RAG setup: ${e.localizedMessage}"
                    onProgress(InitializationState.Error(errorMsg))
                    initializationMutex.withLock { isInitializing = false }
                }
            } else {
                Log.e("LlmInferenceManager", "Model file not available. Initialization failed.")
                onProgress(InitializationState.Error("Model file not available. Initialization failed."))
                initializationMutex.withLock { isInitializing = false }
            }
        }
    }

    private suspend fun acquireModelFromAssets(
        context: Context,
        onProgress: (Int) -> Unit
    ): File? {
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
            onProgress(0)
        }
        Log.d("LlmInferenceManager", "Copying model from assets to ${modelFile.absolutePath}...")
        return try {
            withContext(Dispatchers.IO) {
                context.assets.open(MODEL_ASSET_NAME).use { input ->
                    val totalBytes = input.available().toLong()
                    var bytesCopied: Long = 0
                    FileOutputStream(modelFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesCopied += read
                            val progress = ((bytesCopied * 100) / totalBytes).toInt()
                            // Throttle progress updates if they are too frequent
                            if (progress % 5 == 0 && progress <= 100) { // Update every 5%
                                onProgress(progress)
                            }
                        }
                    }
                }
                Log.d("LlmInferenceManager", "Model copied from assets successfully.")
                onProgress(100)
                modelFile
            }
        } catch (e: IOException) {
            Log.e("LlmInferenceManager", "Failed to copy model from assets: ${e.message}", e)
            if (modelFile.exists()) modelFile.delete()
            onProgress(0)
            null
        }
    }

    private suspend fun acquireDatabaseFromAssets(
        context: Context,
        onProgress: (Int) -> Unit
    ): File? {
        val databaseFile = File(context.getDatabasePath(DB_FILE_NAME).parent, DB_FILE_NAME)

        if (databaseFile.exists() && databaseFile.length() > 0) {
            Log.d("LlmInferenceManager", "Database sudah ada di storage internal: ${databaseFile.absolutePath}")
            onProgress(100)
            return databaseFile
        }
        onProgress(0)

        Log.d("LlmInferenceManager", "Menyalin database dari assets ke ${databaseFile.absolutePath}...")
        return try {
            withContext(Dispatchers.IO) {
                // Pastikan direktori database ada
                databaseFile.parentFile?.mkdirs()
                context.assets.open("$DB_ASSET_FOLDER/$DB_ASSET_NAME").use { inputStream ->
                    val totalBytes = inputStream.available().toLong()
                    var bytesCopied: Long = 0
                    FileOutputStream(databaseFile).use { outputStream ->
                        val buffer = ByteArray(8 * 1024)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            outputStream.write(buffer, 0, read)
                            bytesCopied += read
                            val progress = ((bytesCopied * 100) / totalBytes).toInt()
                            if (progress % 5 == 0 && progress <= 100) {
                                onProgress(progress)
                            }
                        }
                    }
                }
                Log.d("LlmInferenceManager", "Database berhasil disalin dari assets.")
                onProgress(100)
                databaseFile
            }
        } catch (e: IOException) {
            Log.e("LlmInferenceManager", "Gagal menyalin database dari assets: ${e.message}", e)
            databaseFile.delete() // Hapus file jika gagal
            onProgress(0)
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

    suspend fun runWithRag(prompt: String): String? = withContext(Dispatchers.IO) {
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
    }

    /**
     * Generates a conclusion based on the provided user context FROM 'Aktivitas' page.
     * @param userContext The context provided by the user.
     */
    suspend fun getActivityConclusion(userContext: String): String = withContext(Dispatchers.IO) {
        val final_prompt = "Kamu ahli kesehatan manusia. Berikan kesimpulan atau rekomendasi kesehatan berdasarkan data ini: $userContext. \n Jawab dalam 1 hingga 3 kalimat pendek."
        return@withContext run(final_prompt);
    }

    /**
     * Generates health tips based on the provided user context After User Check In.
     * Display in Home Page
     * @param userContext The context provided by the user.
     */
    suspend fun getHealthTips(userContext: String): String = withContext(Dispatchers.IO) {
        val final_prompt = "Kamu ahli kesehatan manusia. Berikan tips kesehatan untuk satu hari berdasarkan data ini: $userContext. \n Jawab dalam 1 kalimat pendek."
        return@withContext run(final_prompt);
    }

    /**
     * Closes the LlmInference engine and releases resources.
     * Should be called when the LLM is no longer needed (e.g., in Application.onTerminate).
     */
    suspend fun runWithImage(prompt: String, imagePath: String): String? = withContext(Dispatchers.IO) {
        if (!isInitialized()) {
            Log.e("LlmInferenceManager", "LLM not initialized. Call initializeIfNeeded() first.")
            return@withContext "Error: AYRA is not ready yet. Please wait for initialization."
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
