package com.example.data

import com.example.BuildConfig
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Centralised, security-hardened configuration for Supabase access from the Android client.
 *
 * Security boundaries:
 * - Reads ONLY `SUPABASE_URL` and `SUPABASE_ANON_KEY` injected via Gradle Secrets Plugin (.env).
 * - NEVER stores, accepts, or transmits `SUPABASE_SERVICE_ROLE_KEY` in Android frontend code.
 * - Rejects placeholder values, non-HTTPS URLs, and accidental service-role JWTs.
 */
object SupabaseConfig {

    const val PRIVATE_CV_BUCKET = "candidate-cvs"

    fun isConfigured(
        url: String = BuildConfig.SUPABASE_URL,
        anonKey: String = BuildConfig.SUPABASE_ANON_KEY
    ): Boolean {
        val cleanUrl = normalizedBaseUrl(url)
        val cleanKey = anonKey.trim().trim('"', '\'')
        if (!cleanUrl.startsWith("https://") || cleanUrl == "YOUR_SUPABASE_URL") return false
        if (cleanKey.isEmpty() || cleanKey == "YOUR_SUPABASE_ANON_KEY") return false
        if (looksLikeServiceRoleKey(cleanKey)) return false
        return true
    }

    fun normalizedBaseUrl(url: String = BuildConfig.SUPABASE_URL): String {
        var clean = url.trim().trim('"', '\'')
        if (clean.contains("google.com/url", ignoreCase = true) && clean.contains("?")) {
            val query = clean.substringAfter('?', "")
            val targetParam = query.split('&')
                .firstOrNull { it.startsWith("q=") || it.startsWith("url=") }
                ?.substringAfter('=')
            if (!targetParam.isNullOrBlank()) {
                clean = try {
                    URLDecoder.decode(targetParam, StandardCharsets.UTF_8.name()).trim()
                } catch (_: Exception) {
                    clean
                }
            }
        }
        if (
            clean.isNotEmpty() &&
            !clean.startsWith("http://", ignoreCase = true) &&
            !clean.startsWith("https://", ignoreCase = true) &&
            clean.contains(".supabase.co", ignoreCase = true)
        ) {
            clean = "https://$clean"
        }
        return clean.trimEnd('/')
    }

    private val serviceRoleRegex = Regex("\"role\"\\s*:\\s*\"service_role\"", RegexOption.IGNORE_CASE)

    /**
     * Defense-in-depth guard: refuses to treat a Supabase `service_role` JWT as a client key.
     */
    fun looksLikeServiceRoleKey(key: String): Boolean {
        val trimmed = key.trim()
        if (trimmed.contains("service_role", ignoreCase = true)) return true
        val parts = trimmed.split('.')
        if (parts.size != 3) return false
        return try {
            val payloadBytes = decodeBase64Url(parts[1])
            val decoded = String(payloadBytes, StandardCharsets.UTF_8)
            serviceRoleRegex.containsMatchIn(decoded)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Pure Kotlin RFC 4648 Base64URL decoder — compatible with Android minSdk 24 and JVM unit tests.
     */
    private fun decodeBase64Url(input: String): ByteArray {
        val clean = input.replace('-', '+').replace('_', '/').filterNot { it.isWhitespace() }
        val out = ByteArray((clean.length * 3) / 4 + 3)
        var outIndex = 0
        var buffer = 0
        var bitsCollected = 0
        for (ch in clean) {
            if (ch == '=') break
            val value = when (ch) {
                in 'A'..'Z' -> ch - 'A'
                in 'a'..'z' -> ch - 'a' + 26
                in '0'..'9' -> ch - '0' + 52
                '+' -> 62
                '/' -> 63
                else -> throw IllegalArgumentException("Invalid Base64URL character")
            }
            buffer = (buffer shl 6) or value
            bitsCollected += 6
            if (bitsCollected >= 8) {
                bitsCollected -= 8
                out[outIndex++] = ((buffer shr bitsCollected) and 0xFF).toByte()
            }
        }
        return out.copyOf(outIndex)
    }

    /**
     * Ensures CV storage paths are private bucket object keys (never public URLs or traversal paths).
     */
    fun isSafePrivateCvPath(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        val p = path.trim()
        return !p.startsWith("/") &&
            !p.startsWith("http://", ignoreCase = true) &&
            !p.startsWith("https://", ignoreCase = true) &&
            !p.contains("/object/public/", ignoreCase = true) &&
            !p.contains("..") &&
            p.contains("/")
    }

    fun encodeQueryValue(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
}
