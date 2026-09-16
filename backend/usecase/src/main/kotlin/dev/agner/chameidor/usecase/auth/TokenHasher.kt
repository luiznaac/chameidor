package dev.agner.chameidor.usecase.auth

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.HexFormat

/**
 * Tokens are stored as their SHA-256 hex digest, never in the clear. Used for both
 * registration (out of band, by the operator/registry tooling, see
 * `docs/external-systems.md`) and lookup.
 */
object TokenHasher {

    fun sha256(token: String): String =
        HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(token.toByteArray(StandardCharsets.UTF_8)),
        )
}
