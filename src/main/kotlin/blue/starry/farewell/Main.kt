package blue.starry.farewell

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import mu.KotlinLogging
import kotlin.time.measureTime
import kotlin.time.seconds

val logger = KotlinLogging.logger("farewell")

suspend fun main(): Unit = coroutineScope {
    while (true) {
        val taken = measureTime {
            try {
                Farewell.check()
            } catch (t: Throwable) {
                logger.error(t) { "Error occurred while checking." }
            }
        }
        logger.trace { "Checking finished in $taken." }

        delay(Env.INTERVAL_SECONDS.seconds)
    }
}
