package androidx.navigation3

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass

@DslMarker public annotation class EntryDsl

/** Creates an [EntryProviderBuilder] with the entry providers provided in the builder. */
public inline fun entryProvider(
    noinline fallback: (unknownScreen: Any) -> NavEntry<*> = {
        throw IllegalStateException("Unknown screen $it")
    },
    builder: EntryProviderBuilder.() -> Unit
): (Any) -> NavEntry<*> = EntryProviderBuilder(fallback).apply(builder).build()

/** DSL for constructing a new [NavEntry] */
@Suppress("TopLevelBuilder")
@EntryDsl
public class EntryProviderBuilder(private val fallback: (unknownScreen: Any) -> NavEntry<*>) {
    private val clazzProviders = mutableMapOf<KClass<*>, EntryClassProvider<*>>()
    private val providers = mutableMapOf<Any, EntryProvider<*>>()

    /** Builds a [NavEntry] for the given [key] that displays [content]. */
    @Suppress("SetterReturnsThis", "MissingGetterMatchingBuilder")
    public fun <T : Any> addEntryProvider(
        key: T,
        featureMap: Map<String, Any> = emptyMap(),
        content: @Composable (T) -> Unit,
    ) {
        require(key !in providers) {
            "An `entry` with the key `key` has already been added: ${key}."
        }
        providers[key] = EntryProvider(key, featureMap, content)
    }

    /** Builds a [NavEntry] for the given [clazz] that displays [content]. */
    @Suppress("SetterReturnsThis", "MissingGetterMatchingBuilder")
    public fun <T : Any> addEntryProvider(
        clazz: KClass<T>,
        featureMap: Map<String, Any> = emptyMap(),
        content: @Composable (T) -> Unit,
    ) {
        require(clazz !in clazzProviders) {
            "An `entry` with the same `clazz` has already been added: ${clazz.simpleName}."
        }
        clazzProviders[clazz] = EntryClassProvider(clazz, featureMap, content)
    }

    /**
     * Returns an instance of entryProvider created from the entry providers set on this builder.
     */
    @Suppress("UNCHECKED_CAST")
    public fun build(): (Any) -> NavEntry<*> = { key ->
        val entryClassProvider = clazzProviders[key::class] as? EntryClassProvider<Any>
        val entryProvider = providers[key] as? EntryProvider<Any>
        entryClassProvider?.run { NavEntry(key, featureMap, content) }
            ?: entryProvider?.run { NavEntry(key, featureMap, content) }
            ?: fallback.invoke(key)
    }
}

/** Add an entry provider to the [EntryProviderBuilder] */
public fun <T : Any> EntryProviderBuilder.entry(
    key: T,
    featureMap: Map<String, Any> = emptyMap(),
    content: @Composable (T) -> Unit,
) {
    addEntryProvider(key, featureMap, content)
}

/** Add an entry provider to the [EntryProviderBuilder] */
public inline fun <reified T : Any> EntryProviderBuilder.entry(
    featureMap: Map<String, Any> = emptyMap(),
    noinline content: @Composable (T) -> Unit,
) {
    addEntryProvider(T::class, featureMap, content)
}

/** Holds a Entry class, featureMap, and content for that class */
public data class EntryClassProvider<T : Any>(
    val clazz: KClass<T>,
    val featureMap: Map<String, Any>,
    val content: @Composable (T) -> Unit,
)

/** Holds a Entry class, featureMap, and content for that key */
public data class EntryProvider<T : Any>(
    val key: T,
    val featureMap: Map<String, Any>,
    val content: @Composable (T) -> Unit,
)