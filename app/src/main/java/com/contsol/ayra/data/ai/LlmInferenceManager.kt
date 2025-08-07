package com.contsol.ayra.data.ai

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.contsol.ayra.data.source.local.database.model.Tips
import com.contsol.ayra.data.source.rag.RagPipeline
import com.contsol.ayra.data.state.InitializationState
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession.LlmInferenceSessionOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

object LlmInferenceManager {
    private var llmInference: LlmInference? = null
    private val initializationMutex = Mutex() // To prevent concurrent initialization attempts
    private var ragPipeline: RagPipeline? = null

    private var _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    private const val MODEL_ASSET_NAME = "gemma-3n-E2B-it-int4.task"
    private const val MODEL_FILE_NAME = "gemma-3n-E2B-it-int4.task" // Name for the file in internal storage
    private const val EXPECTED_MODEL_CHECKSUM_MD5 = "902207CA56F3D125F3E4807C7D4596CD"

    private const val DB_ASSET_FOLDER = "database" // Folder di dalam assets
    private const val DB_ASSET_NAME = "knowledge_base.db" // Nama file DB di assets
    private const val DB_FILE_NAME = "knowledge_base.db"  // Nama file di storage internal
    /**
     * Initializes the LlmInference engine if it hasn't been already.
     * This method handles model acquisition (download or copy from assets) and setup.
     *
     * @param context Application context.
     *                      once initialization is successful.
     */
    suspend fun initializeIfNeeded(
        context: Context,
        onProgress: (InitializationState) -> Unit,
    ) {
        // Early exit if already ready (outside the mutex for quick check)
        if (_isReady.value) {
            Log.d("LlmInferenceManager", "Already initialized. Skipping.")
            onProgress(InitializationState.Complete) // Notify progress that it's already complete
            return
        }

        initializationMutex.withLock {
            // Double-check readiness after acquiring the lock, in case it completed while waiting
            if (_isReady.value) {
                Log.d("LlmInferenceManager", "Initialized while waiting for lock. Skipping.")
                onProgress(InitializationState.Complete)
                return
            }

            Log.d("LlmInferenceManager", "Initializing LlmInferenceManager...")
            // No need for 'isInitializing = true' if the mutex handles concurrency
            // and _isReady handles the final state.

            try {
                onProgress(InitializationState.NotStarted) // Initial progress update

                // All potentially long-running operations should be within a proper coroutine context
                // The withContext(Dispatchers.IO) here is good.

                // --- Model Acquisition ---
                onProgress(InitializationState.CopyingModel(0))
                val modelFile = acquireModelFromAssets(context.applicationContext) { progressPercentage ->
                    onProgress(InitializationState.CopyingModel(progressPercentage))
                }
                if (modelFile == null || !modelFile.exists()) {
                    val errorMsg = "Failed to acquire model."
                    Log.e("LlmInferenceManager", "Model file not available. Initialization failed.")
                    onProgress(InitializationState.Error(errorMsg))
                    _isReady.value = false // Ensure ready state is false
                    // No need to manage isInitializing manually with mutex and _isReady.value
                    return // Exit from withLock block
                }
                onProgress(InitializationState.CopyingModel(100))

                // --- Database Acquisition ---
                onProgress(InitializationState.CopyingDatabase(0))
                val databaseFile = acquireDatabaseFromAssets(context.applicationContext) { progressPercentage ->
                    onProgress(InitializationState.CopyingDatabase(progressPercentage))
                }
                if (databaseFile == null || !databaseFile.exists()) {
                    val errorMsg = "Failed to acquire database."
                    Log.e("LlmInferenceManager", "Database file not available. Initialization failed.")
                    onProgress(InitializationState.Error(errorMsg))
                    _isReady.value = false
                    return
                }
                onProgress(InitializationState.CopyingDatabase(100))

                // --- LLM and RAG Initialization ---
                onProgress(InitializationState.InitializingLlm)
                // The LlmInference.createFromOptions and RAG setup might also be IO-bound
                // Ensure they are either suspend functions or wrapped appropriately if blocking.
                // The outer withContext(Dispatchers.IO) in your original code handles this block.

                val taskOptions = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxNumImages(1)
                    .setPreferredBackend(LlmInference.Backend.CPU)
                    .setMaxTokens(1024)
                    .build()

                // LlmInference.createFromOptions itself might not be a suspend function.
                // It's good to keep it within Dispatchers.IO.
                llmInference = LlmInference.createFromOptions(context.applicationContext, taskOptions)
                Log.i("LlmInferenceManager", "LlmInference initialized successfully.")

                onProgress(InitializationState.InitializingRag)
                ragPipeline = RagPipeline(context.applicationContext as Application, taskOptions) // Assuming RagPipeline constructor is safe to call like this
                // ragPipeline?.memorizeChunks(context, "knowledge/knowledge-1.txt") // This could be IO intensive
                Log.i("LlmInferenceManager", "RAG Pipeline initialized successfully.")

                // ---- Success ----
                _isReady.value = true // Set the readiness flag to true ONCE everything is successful
                onProgress(InitializationState.Complete)
                Log.i("LlmInferenceManager", "LlmInferenceManager fully initialized successfully.")

                // Clear listeners if you were using them, though StateFlow is now primary
                // initializationListeners.forEach { it() }
                // initializationListeners.clear()

            } catch (e: Exception) {
                Log.e("LlmInferenceManager", "LLM Initialization failed", e)
                _isReady.value = false // Critical: ensure _isReady is false on any failure path
                onProgress(InitializationState.Error(e.message ?: "Unknown error during initialization"))
                // Re-throwing is good if the caller (e.g., ViewModel) needs to react to the specific exception.
                // If you re-throw, the _isReady.value = false above is still important.
                throw e
            }
            // No 'finally' block needed here specifically for 'isInitializing' if you remove it.
        }
    }

    private suspend fun acquireModelFromAssets(
        context: Context,
        onProgress: (Int) -> Unit
    ): File? {
        val modelFile = File(context.filesDir, MODEL_FILE_NAME)
        if (modelFile.exists() && modelFile.length() > 0) {
            Log.d("LlmInferenceManager", "Model already exists at ${modelFile.absolutePath}. Skipping copy.")
            val currentChecksum = calculateMD5(modelFile)
            if (EXPECTED_MODEL_CHECKSUM_MD5.equals(currentChecksum, ignoreCase = true)) {
                Log.d("LlmInferenceManager", "Model integrity verified. Skipping copy.")
                onProgress(100)
                return modelFile
            } else {
                Log.w(
                    "LlmInferenceManager",
                    "Model checksum mismatch. Expected: $EXPECTED_MODEL_CHECKSUM_MD5, Found: $currentChecksum. Deleting and re-copying."
                )
                // Attempt to delete the corrupted file
                withContext(Dispatchers.IO) {
                    try {
                        if (modelFile.exists()) modelFile.delete()
                    } catch (e: Exception) {
                        Log.e("LlmInferenceManager", "Failed to delete corrupted model: ${e.message}", e)
                        // If deletion fails, we might not be able to proceed
                        onProgress(0) // Reset progress
                        // Potentially return null or throw an error to indicate a critical issue
                        return@withContext null
                    }
                }
                onProgress(0) // Reset progress before re-copying
            }
        } else if (modelFile.exists() && modelFile.length() == 0L) {
            Log.w("LlmInferenceManager", "Model file exists but is empty. Deleting and re-copying.")
            withContext(Dispatchers.IO) {
                try {
                    modelFile.delete()
                } catch (e: Exception) {
                    Log.e("LlmInferenceManager", "Failed to delete empty model: ${e.message}", e)
                    onProgress(0)
                    return@withContext null
                }
            }
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
                val copiedChecksum = calculateMD5(modelFile)
                if (EXPECTED_MODEL_CHECKSUM_MD5.equals(copiedChecksum, ignoreCase = true)) {
                    Log.d("LlmInferenceManager", "Model copied from assets successfully and verified.")
                    onProgress(100)
                    modelFile
                } else {
                    Log.e(
                        "LlmInferenceManager",
                        "Copied model checksum mismatch after copy! Expected: $EXPECTED_MODEL_CHECKSUM_MD5, Found: $copiedChecksum. Deleting."
                    )
                    if (modelFile.exists()) modelFile.delete()
                    onProgress(0)
                    null // Indicate failure
                }
            }
        } catch (e: IOException) {
            Log.e("LlmInferenceManager", "Failed to copy model from assets: ${e.message}", e)
            if (modelFile.exists()) modelFile.delete()
            onProgress(0)
            null
        }
    }

    private fun calculateMD5(file: File): String? {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            FileInputStream(file).use { fis ->
                val byteArray = ByteArray(1024)
                var bytesCount: Int
                while (fis.read(byteArray).also { bytesCount = it } != -1) {
                    digest.update(byteArray, 0, bytesCount)
                }
            }
            val bytes = digest.digest()
            val sb = StringBuilder()
            for (byte in bytes) {
                sb.append(String.format("%02x", byte))
            }
            sb.toString().uppercase()
        } catch (e: Exception) {
            Log.e("LlmInferenceManager", "Failed to calculate MD5 checksum for ${file.name}", e)
            null
        }
    }

    private suspend fun acquireDatabaseFromAssets(
        context: Context,
        onProgress: (Int) -> Unit
    ): File? {
        val databaseFile = File(context.getDatabasePath(DB_FILE_NAME).parent, DB_FILE_NAME)

        if (databaseFile.exists() && databaseFile.length() > 0) {
            Log.d("LlmInferenceManager", "Database already exists at ${databaseFile.absolutePath}. Skipping copy.")
            onProgress(100)
            return databaseFile
            /* databaseFile.delete()
            Log.d("LlmInferenceManager", "Database file exists but is empty. Deleting and re-copying.") */
        }
        onProgress(0)

        Log.d("LlmInferenceManager", "Copying database from assets to ${databaseFile.absolutePath}...")
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

    fun isInitialized(): Boolean = _isReady.value

    private suspend fun run(prompt: String): String = withContext(Dispatchers.IO) {
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
        return@withContext run(final_prompt)
    }

    /**
     * Generates health tips based on the provided user context After User Check In.
     * Display in Home Page
     */
    private val jsonParser = Json {
        isLenient = true
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    suspend fun getHealthTips(userContext: String? = null): List<Tips> {
        if (!_isReady.value) {
            Log.w("LlmInferenceManager", "getHealthTips called but LLM not ready. Attempting to initialize...")
            // Optionally, you could throw an IllegalStateException or try to initialize here.
            // For now, let's assume MainActivity triggers initialization.
            // If called directly without prior MainActivity init, this might need a context.
            // Consider what context `getHealthTips` would have if called before `initializeIfNeeded`
            // from MainActivity. It's safer if `initializeIfNeeded` is always called first.
            // For robustness, you could add:
            // currentCoroutineContext().job.cancel("LLM not initialized, cannot get tips.")
            throw IllegalStateException("LLM not initialized. Call initializeIfNeeded first.")
            // return emptyList()
        }

        val contextString = userContext?.takeIf { it.isNotBlank() } ?: "tidak ada konteks spesifik"

        val finalPrompt = """
            Kamu adalah seorang ahli kesehatan manusia. Berikan 5 tips kesehatan singkat untuk satu hari berdasarkan konteks pengguna berikut: "$contextString".

            Format jawabanmu HARUS berupa array JSON valid yang berisi objek-objek tips.
            Setiap objek tips HARUS memiliki dua properti: "title" (String) dan "content" (String).
            Contoh format array JSON yang diinginkan:
            [
              {"title": "Contoh Judul 1", "content": "Contoh isi tips pertama."},
              {"title": "Contoh Judul 2", "content": "Contoh isi tips kedua."}
            ]
            Pastikan tidak ada teks tambahan sebelum atau sesudah array JSON. Hanya array JSON saja.
        """.trimIndent()

        return withContext(Dispatchers.IO) {
            try {
                val jsonResponseString = run(finalPrompt) // This is your existing function that calls the LLM
                Log.d("LlmInferenceManager", "Raw JSON response from LLM: $jsonResponseString")

                if (jsonResponseString.isBlank()) {
                    Log.w("LlmInferenceManager", "Received blank response from LLM for health tips.")
                    return@withContext emptyList()
                }

                // Attempt to parse the JSON string into List<Tips>
                // It's good to clean the string first if the LLM might add markdown or other noise
                val cleanedJsonResponse = cleanLlmJsonResponse(jsonResponseString)
                Log.d("LlmInferenceManager", "Cleaned JSON response: $cleanedJsonResponse")


                if (cleanedJsonResponse.startsWith("[") && cleanedJsonResponse.endsWith("]")) {
                    val tipsList: List<Tips> = jsonParser.decodeFromString(cleanedJsonResponse)
                    Log.i("LlmInferenceManager", "Successfully parsed ${tipsList.size} health tips.")
                    return@withContext tipsList
                } else {
                    Log.e("LlmInferenceManager", "LLM response is not a valid JSON array: $cleanedJsonResponse")
                    // You could try to parse a single object if the LLM messed up the array
                    // or return emptyList / throw specific error
                    return@withContext emptyList()
                }

            } catch (e: SerializationException) {
                Log.e("LlmInferenceManager", "Failed to parse JSON response for health tips: ${e.message}", e)
                // Optionally, you could try to extract content even if parsing fails,
                // or just return an empty list or a default "error" tip.
                return@withContext listOf(Tips("Error", "Gagal memproses tips kesehatan dari AI."))
            } catch (e: Exception) {
                Log.e("LlmInferenceManager", "Error running LLM for health tips: ${e.message}", e)
                return@withContext emptyList()
            }
        }
    }

    private fun cleanLlmJsonResponse(rawResponse: String): String {
        // Try to find the start of the JSON array
        val startIndex = rawResponse.indexOf('[')
        // Try to find the end of the JSON array
        val endIndex = rawResponse.lastIndexOf(']')

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return rawResponse.substring(startIndex, endIndex + 1).trim()
        }

        // Fallback for cases where LLM might just output JSON without markdown
        // and might have surrounding quotes if it's a stringified JSON within a larger string.
        val trimmed = rawResponse.trim()
        if (trimmed.startsWith("\"[") && trimmed.endsWith("]\"")) {
            return trimmed.substring(1, trimmed.length - 1).replace("\\\"", "\"")
        }
        if (trimmed.startsWith("'[") && trimmed.endsWith("]'")) {
            return trimmed.substring(1, trimmed.length - 1).replace("\\\'", "\'")
        }
        return trimmed // Return trimmed original if no clear array boundaries found
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
