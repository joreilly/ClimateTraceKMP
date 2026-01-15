package dev.johnoreilly.climatetrace.agent

import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel

import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
import ai.koog.agents.features.opentelemetry.integration.langfuse.addLangfuseExporter



//actual fun getLLModel(): LLModel {
//    return LLModel(
//        provider = LLMProvider.Ollama,
//        //id = "llama3.1:8b",
//        //id = "llama3.2:3b",
//        id = "gpt-oss:20b",
//        capabilities = listOf(
//            LLMCapability.Temperature,
//            LLMCapability.Schema.JSON.Standard,
//            LLMCapability.Tools
//        ),
//        contextLength = 128_000,
//    )
//}

//actual fun getPromptExecutor(apiKey: String): PromptExecutor {
//    return simpleOllamaAIExecutor()
//}

import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import dev.johnoreilly.climatetrace.BuildKonfig

actual fun getLLModel() = GoogleModels.Gemini2_5Flash

actual fun getPromptExecutor(): PromptExecutor {
    return simpleGoogleAIExecutor(BuildKonfig.GEMINI_API_KEY)
}