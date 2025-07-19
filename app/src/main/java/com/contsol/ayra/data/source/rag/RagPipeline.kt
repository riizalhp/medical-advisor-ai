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
class RagPipeline(application: Application, gemmaPath: String) {
    private val mediaPipeLanguageModelOptions: LlmInferenceOptions =
        LlmInferenceOptions.builder().setModelPath(
            gemmaPath
        ).setPreferredBackend(LlmInference.Backend.CPU).setMaxTokens(1024).build()

    private val mediaPipeLanguageModelSessionOptions: LlmInferenceSession.LlmInferenceSessionOptions =
        LlmInferenceSession.LlmInferenceSessionOptions.builder().setTemperature(1.0f)
            .setTopP(0.95f).setTopK(64).build()

    private val mediaPipeLanguageModel: MediaPipeLlmBackend =
        MediaPipeLlmBackend(
            application.applicationContext, mediaPipeLanguageModelOptions,
            mediaPipeLanguageModelSessionOptions
        )

    private val embedder: Embedder<String> = GeckoEmbeddingModel(
        GECKO_MODEL_PATH,
        Optional.ofNullable(TOKENIZER_MODEL_PATH),
        USE_GPU_FOR_EMBEDDINGS,
    )

    private val config = ChainConfig.create(
        mediaPipeLanguageModel,
        PromptBuilder(PROMPT_TEMPLATE),
        DefaultSemanticTextMemory(
            SqliteVectorStore(768),
            embedder
        )
    )

    private val retrievalAndInferenceChain = RetrievalAndInferenceChain(config)

    init {
        Log.i("RagPipeline", "Initializing MediaPipe language model...")
        Futures.addCallback(
            mediaPipeLanguageModel.initialize(),
            object : FutureCallback<Boolean> {
                override fun onSuccess(result: Boolean) {
                    Log.i("RagPipeline", "MediaPipe language model initialized")
                }

                override fun onFailure(t: Throwable) {
                    Log.e("RagPipeline", "MediaPipe language model initialization failed", t)
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

    private fun memorize(facts: List<String>) {
        val future = config.semanticMemory.getOrNull()?.recordBatchedMemoryItems(ImmutableList.copyOf(facts))
        future?.get()
        Log.d("RagPipeline", "Memorized facts: $facts")
    }

    suspend fun generateResponse(
        prompt: String,
        callback: AsyncProgressListener<LanguageModelResponse>?
    ): String = coroutineScope {
        val retrievalRequest = RetrievalRequest.create(
            prompt,
            RetrievalConfig.create(3, 0.0f, TaskType.QUESTION_ANSWERING)
        )
        retrievalAndInferenceChain.invoke(retrievalRequest, callback).await().text
    }

    companion object {
        private const val CHUNK_SEPARATOR = "<chunk_splitter>"
        private const val TOKENIZER_MODEL_PATH = "/data/local/tmp/sentencepiece.model"
        private const val GECKO_MODEL_PATH = "/data/local/tmp/gecko_1024_quant.tflite"
        private const val USE_GPU_FOR_EMBEDDINGS = false

        const val PROMPT_TEMPLATE: String =
            "Kamu adalah asisten yang menjawab pertanyaan. Ingat informasi ini: {0}. Hanya gunakan informasi yang tertera untuk menjawab pertanyaan ini dalam 1 hingga 3 kalimat: {1}"
    }
}
