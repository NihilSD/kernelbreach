package com.kernelbreach.core.content

import kotlinx.serialization.json.Json

/**
 * The single [Json] configuration used across the content layer.
 *
 * `ignoreUnknownKeys = true` keeps older app versions reading newer content
 * (e.g. future reading-depth fields) without crashing. We keep it strict
 * otherwise so malformed content is caught by the validator rather than
 * silently coerced.
 */
internal val ContentJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = false
    explicitNulls = false
}
