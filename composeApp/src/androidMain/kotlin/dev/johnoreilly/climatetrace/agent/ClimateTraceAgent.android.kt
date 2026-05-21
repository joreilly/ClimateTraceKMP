package dev.johnoreilly.climatetrace.agent

import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.litert.LiteRTClientConfig
import ai.koog.prompt.executor.clients.litert.LiteRTLLMClient
import ai.koog.prompt.executor.clients.litert.LiteRTLLModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import dev.johnoreilly.climatetrace.BuildKonfig

actual fun getLLModel() = GoogleModels.Gemini2_5Flash

actual fun getPromptExecutor(): PromptExecutor {
    return simpleGoogleAIExecutor(BuildKonfig.GEMINI_API_KEY, KtorKoogHttpClient.Factory())
}


/*
adb push gemma-4-E2B-it.litertlm /data/local/tmp

val config = LiteRTClientConfig(
    modelsPath = "/data/local/tmp",
    cacheDir = "/data/local/tmp/cache"
)

val client = LiteRTLLMClient(config)

actual fun getLLModel() = LiteRTLLModels.Gemma4E2B

actual fun getPromptExecutor(): PromptExecutor {
    return MultiLLMPromptExecutor(client)
}
 */


