package com.contsol.ayra.data.source.rag

import android.content.Context
import com.google.ai.edge.rag.RagClient
import com.google.ai.edge.rag.RagConfig
import org.mediapipe.tasks.genai.llminference.LlmInference
import org.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions

class OnDeviceRagSource(private val context: Context) {

    // Initialize the RAG client to search the local DB
    private val ragClient: RagClient = RagClient.create(
        RagConfig.builder()
            .setDatabasePath("/data/data/${context.packageName}/files/knowledge_base.db") // You must copy the DB from assets to internal storage first
            .build()
    )

    // Initialize MediaPipe LLM for generation
    private val llmInference: LlmInference

    init {
        // First, copy the DB from assets to a place the RAG client can access
        copyAssetToInternalStorage("knowledge_base.db")

        val options = LlmInferenceOptions.builder()
            .setModelPath("/data/data/${context.packageName}/files/gemma-2b-it-int4.tflite") // Also copy model to internal storage
            .build()
        llmInference = LlmInference.createFromOptions(context, options)
    }

    fun getAnswer(question: String): String {
        // 1. Retrieval: Find relevant chunks from the local vector DB
        val searchResult = ragClient.search(question, 3) // Get top 3 results
        val contextString = searchResult.results.joinToString("\n") { it.text }

        // 2. Generation: Build the prompt and generate an answer
        val prompt = """
        Use the following pieces of context to answer the question at the end. 
        If you don't know the answer, just say that you don't know, don't try to make up an answer.

        Context:
        $contextString

        Question:
        $question

        Answer:
        """.trimIndent()

        return llmInference.generateResponse(prompt)
    }

    // Helper function to copy files from assets to internal storage
    private fun copyAssetToInternalStorage(fileName: String) {
        // ... implementation to copy file from context.assets to context.filesDir ...
    }
}