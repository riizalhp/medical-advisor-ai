package com.contsol.ayra

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.contsol.ayra.data.source.rag.RagPipeline
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RagPipelineTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val pipeline = RagPipeline(context as android.app.Application)

    @Test
    fun testMemorizeChunks() {
        // Contoh file: assets/knowledge/test_knowledge.txt
        pipeline.memorizeChunks(context, "knowledge/knowledge_test.txt")
        // Kalau tidak error → pass
        assertTrue(true)
    }

    @Test
    fun testGenerateResponse_Found() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pipeline = RagPipeline(context as android.app.Application)

        pipeline.memorizeChunks(context, "knowledge/test_knowledge.txt")

        val question = "What is the powerhouse of the cell?"
        val answer = pipeline.generateResponse(question)

        println("Answer: $answer")
        assertTrue(answer.contains("mitochondria", ignoreCase = true))
    }

    @Test
    fun testGenerateResponse_NotFound() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pipeline = RagPipeline(context as android.app.Application)

        pipeline.memorizeChunks(context, "knowledge/test_knowledge.txt")

        val question = "Who won the World Cup in 1978?"
        val answer = pipeline.generateResponse(question)

        println("Answer: $answer")
        // Kita expect LLM menjawab "I don't know" atau jawaban kosong
        assertTrue(answer.isEmpty() || answer.contains("I don't know", ignoreCase = true))
    }
}
