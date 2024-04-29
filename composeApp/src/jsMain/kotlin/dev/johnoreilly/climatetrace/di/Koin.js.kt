package dev.johnoreilly.climatetrace.di

import dev.johnoreilly.climatetrace.remote.Country
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.storage.storeOf
import io.github.xxfast.kstore.utils.ExperimentalKStoreApi
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun dataModule(): Module = module {
    single<KStore<List<Country>>> {
        storeOf(key = "countries", default = emptyList())
    }
}
