package com.contsol.ayra.data.ai

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File
import java.io.IOException

object LlmInferenceManager {

    private var llmInference: LlmInference? = null

    fun initialize(context: Context) {
        val modelName = "gemma-3n-E2B-it-int4.task"
        val modelFile = File(context.filesDir, modelName)

        if (!modelFile.exists()) {
            try {
                context.assets.open(modelName).use { inputStream -> // load model dari assets
                    modelFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Log.d("LlmInferenceManager", "Model copied from assets to ${modelFile.absolutePath}")
            } catch (e: IOException) {
                Log.e("LlmInferenceManager", "Failed to copy model from assets: ${e.message}")
                return
            }
        }

        val taskOptions = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelFile.absolutePath)
            .setMaxTopK(64)
            .build()

        try {
            llmInference = LlmInference.createFromOptions(context, taskOptions)
            Log.d("LlmInferenceManager", "LlmInference initialized successfully.")
        } catch (e: Exception) {
            Log.e("LlmInferenceManager", "Failed to initialize LlmInference: ${e.message}")
        }
    }
}
