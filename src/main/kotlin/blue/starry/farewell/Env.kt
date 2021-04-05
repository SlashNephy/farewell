package blue.starry.farewell

import kotlin.properties.ReadOnlyProperty

object Env {
    val TWITTER_CK by string
    val TWITTER_CS by string
    val TWITTER_AT by string
    val TWITTER_ATS by string

    val INTERVAL_SECONDS by long { 60 }
    val DISCORD_WEBHOOK_URL by string

    val DRYRUN by boolean
}

private val string: ReadOnlyProperty<Env, String>
    get() = ReadOnlyProperty { _, property ->
        System.getenv(property.name) ?: error("Env: ${property.name} is not present.")
    }

private fun long(default: () -> Long) = ReadOnlyProperty<Env, Long> { _, property ->
    System.getenv(property.name)?.toLongOrNull() ?: default()
}

private fun String?.toBooleanFazzy(): Boolean {
    return when (this) {
        null -> false
        "1", "yes" -> true
        else -> toLowerCase().toBoolean()
    }
}

private val boolean: ReadOnlyProperty<Env, Boolean>
    get() = ReadOnlyProperty { _, property ->
        System.getenv(property.name).toBooleanFazzy()
    }
