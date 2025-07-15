package com.contsol.ayra.data.source.rag

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.ai.edge.localagents.rag.chains.ChainConfig
import com.google.ai.edge.localagents.rag.chains.RetrievalAndInferenceChain
import com.google.ai.edge.localagents.rag.memory.DefaultSemanticTextMemory
import com.google.ai.edge.localagents.rag.memory.SqliteVectorStore
import com.google.ai.edge.localagents.rag.models.AsyncProgressListener
import com.google.ai.edge.localagents.rag.models.Embedder
import com.google.ai.edge.localagents.rag.models.GeckoEmbeddingModel
import com.google.ai.edge.localagents.rag.models.LanguageModelResponse
import com.google.ai.edge.localagents.rag.models.MediaPipeLlmBackend
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.google.ai.edge.localagents.rag.prompt.PromptBuilder
import com.google.ai.edge.localagents.rag.retrieval.RetrievalConfig
import com.google.ai.edge.localagents.rag.retrieval.RetrievalConfig.TaskType
import com.google.ai.edge.localagents.rag.retrieval.RetrievalRequest
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.guava.await
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

/** The RAG pipeline for LLM generation. */
class RagPipeline(private val application: Application) {
    private val mediaPipeLanguageModelOptions: LlmInferenceOptions =
        LlmInferenceOptions.builder()
            .setModelPath(GEMMA_MODEL_PATH)
            .setPreferredBackend(LlmInference.Backend.CPU)
            .setMaxTokens(1024)
            .build()

    private val mediaPipeLanguageModelSessionOptions: LlmInferenceSession.LlmInferenceSessionOptions =
        LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTemperature(1.0f)
            .setTopP(0.95f)
            .setTopK(64)
            .build()

    private val mediaPipeLanguageModel: MediaPipeLlmBackend =
        MediaPipeLlmBackend(
            application.applicationContext,
            mediaPipeLanguageModelOptions,
            mediaPipeLanguageModelSessionOptions
        )

    private val embedder: Embedder<String> = GeckoEmbeddingModel(
        GECKO_MODEL_PATH,
        Optional.of(TOKENIZER_MODEL_PATH),
        USE_GPU_FOR_EMBEDDINGS,
    )

    private val config = ChainConfig.create(
        mediaPipeLanguageModel,
        PromptBuilder(PROMPT_TEMPLATE),
        DefaultSemanticTextMemory(
            SqliteVectorStore(768), // Gecko dim = 768
            embedder
        )
    )

    private val retrievalAndInferenceChain = RetrievalAndInferenceChain(config)

    init {
        Futures.addCallback(
            mediaPipeLanguageModel.initialize(),
            object : FutureCallback<Boolean> {
                override fun onSuccess(result: Boolean) {
                    // no-op
                }

                override fun onFailure(t: Throwable) {
                    // no-op
                }
            },
            Executors.newSingleThreadExecutor(),
        )
    }

    fun memorizeChunks(context: Context, filename: String) {
        val reader = BufferedReader(InputStreamReader(context.assets.open(filename)))
        val sb = StringBuilder()
        val texts = mutableListOf<String>()

        generateSequence { reader.readLine() }
            .forEach { line ->
                if (line.startsWith(CHUNK_SEPARATOR)) {
                    if (sb.isNotEmpty()) {
                        texts.add(sb.toString())
                    }
                    sb.clear()
                    sb.append(line.removePrefix(CHUNK_SEPARATOR).trim())
                } else {
                    sb.append(" ")
                    sb.append(line)
                }
            }

        if (sb.isNotEmpty()) {
            texts.add(sb.toString())
        }
        reader.close()

        if (texts.isNotEmpty()) {
            memorize(texts)
        }
    }

    /** Stores input texts in the semantic text memory. */
    private fun memorize(facts: List<String>) {
        val future = config.semanticMemory.getOrNull()?.recordBatchedMemoryItems(ImmutableList.copyOf(facts))
        future?.get()
        Log.d("RagPipeline", "Memorizing facts: $facts")
    }

    /** Generates the response from the LLM. */
    suspend fun generateResponse(
        prompt: String,
        callback: AsyncProgressListener<LanguageModelResponse>? = null,
    ): String = coroutineScope {
        val retrievalRequest = RetrievalRequest.create(
            prompt,
            RetrievalConfig.create(3, 0.0f, TaskType.QUESTION_ANSWERING)
        )
        retrievalAndInferenceChain.invoke(retrievalRequest, callback).await().text
    }

    companion object {
        private const val COMPUTE_EMBEDDINGS_LOCALLY = true
        private const val USE_GPU_FOR_EMBEDDINGS = false
        private const val CHUNK_SEPARATOR = "<chunk_splitter>"

        private const val GEMMA_MODEL_PATH = "/data/local/tmp/gemma-3n-e2b-it-int4.task"
        private const val TOKENIZER_MODEL_PATH = "/data/local/tmp/sentencepiece.model"
        private const val GECKO_MODEL_PATH = "/data/local/tmp/gecko_1024_quant.tflite"

        // Prompt template for RetrievalAndInferenceChain
        private const val PROMPT_TEMPLATE: String =
            "You are an assistant for question-answering tasks. Here are the things I want to remember: {0} Use the things I want to remember, answer the following question the user has: {1}"
    }
}
