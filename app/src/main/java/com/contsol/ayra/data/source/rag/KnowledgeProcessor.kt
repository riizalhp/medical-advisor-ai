package com.contsol.ayra.data.source.rag

import com.contsol.ayra.data.source.local.database.dao.DocumentChunkDao
import com.contsol.ayra.data.source.local.database.entity.KnowledgeBaseEntity
import com.contsol.ayra.data.source.local.database.entity.DocumentChunkEntity
import com.google.ai.edge.localagents.rag.models.Embedder
import com.google.ai.edge.localagents.rag.models.EmbeddingRequest
import com.google.ai.edge.localagents.rag.models.GeckoEmbeddingModel
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.guava.await
import java.util.Optional

class KnowledgeBaseProcessor(
    private val chunkDao: DocumentChunkDao,
    private val GECKO_MODEL_PATH: String,
    private val TOKENIZER_MODEL_PATH: String,
    private val USE_GPU_FOR_EMBEDDINGS: Boolean
) {
    private val embedder: Embedder<String> = GeckoEmbeddingModel(
        GECKO_MODEL_PATH,
        Optional.of(TOKENIZER_MODEL_PATH),
        USE_GPU_FOR_EMBEDDINGS,
    )

    fun chunkContent(content: String, maxChunkSize: Int = 500): List<String> {
        val paragraphs = content.split("\n\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val chunks = mutableListOf<String>()
        var currentChunk = StringBuilder()

        for (para in paragraphs) {
            if (currentChunk.length + para.length > maxChunkSize) {
                chunks.add(currentChunk.toString().trim())
                currentChunk = StringBuilder()
            }
            currentChunk.append(para).append("\n\n")
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString().trim())
        }

        return chunks
    }

    suspend fun embedChunks(chunks: List<String>): List<FloatArray> {
        return chunks.map { chunk ->
            val request = EmbeddingRequest.create(chunk)
            val result: ImmutableList<Float> = embedder.getEmbeddings(request).await()
            result.toFloatArray()
        }
    }

    suspend fun storeChunks(
        knowledgeBaseId: String,
        chunks: List<String>,
        embeddings: List<FloatArray>
    ) {
        val chunkEntities = chunks.mapIndexed { index, chunkText ->
            DocumentChunkEntity(
                knowledgeBaseId = knowledgeBaseId,
                chunkIndex = index,
                content = chunkText,
                embedding = embeddings[index],
                metadata = null
            )
        }
        chunkDao.insertChunks(chunkEntities)
    }

    suspend fun processKnowledgeBaseContent(knowledgeBase: KnowledgeBaseEntity) {
        val chunks = chunkContent(knowledgeBase.content)
        val embeddings = embedChunks(chunks)

        // Remove old chunks if updating
        chunkDao.deleteChunksByKnowledgeBaseId(knowledgeBase.id)

        storeChunks(knowledgeBase.id, chunks, embeddings)
    }
}