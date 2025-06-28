import dev.johnoreilly.climatetrace.di.initKoin
import kotlinx.coroutines.runBlocking
import kotlin.uuid.ExperimentalUuidApi


val koin = initKoin(enableNetworkLogs = true).koin

@OptIn(ExperimentalUuidApi::class)
fun main() = runBlocking{
    runKoogAgent()
}
