package com.kernelbreach.core.content

import java.security.MessageDigest

/**
 * A stable content hash over every raw JSON string, used to decide whether a
 * re-import is needed after an app update. Inputs are sorted by key so the hash
 * is deterministic regardless of read order. (`java.security.MessageDigest` is
 * available on both the JVM and Android.)
 */
object ContentHash {
    fun of(parts: Map<String, String>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        for (key in parts.keys.sorted()) {
            digest.update(key.toByteArray(Charsets.UTF_8))
            digest.update(0)
            digest.update(parts.getValue(key).toByteArray(Charsets.UTF_8))
            digest.update(0)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
