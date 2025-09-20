package dev.johnoreilly.climatetrace.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.java.Java

actual fun createHttpClientEngine(): HttpClientEngine {
    return Java.create()
}