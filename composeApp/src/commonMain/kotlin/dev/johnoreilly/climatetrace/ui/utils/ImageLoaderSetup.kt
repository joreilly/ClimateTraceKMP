package dev.johnoreilly.climatetrace.ui.utils

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders

// OSM's tile usage policy (https://operations.osmfoundation.org/policies/tiles/) asks for a
// descriptive User-Agent identifying the app. Call this from every entry point that can end up
// showing a map; only the first call has any effect.
@Composable
internal fun ConfigureImageLoader() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(
                    KtorNetworkFetcherFactory(
                        httpClient = {
                            HttpClient {
                                defaultRequest {
                                    header(
                                        HttpHeaders.UserAgent,
                                        "ClimateTraceKMP/1.0 (+https://github.com/joreilly/ClimateTraceKMP)"
                                    )
                                }
                            }
                        }
                    )
                )
            }
            .build()
    }
}
