package dev.agner.chameidor.usecase.auth

/**
 * Outcome of [AuthorizationService.authorize]. The [reason] strings are the minimal
 * error contract of the HTTP edge: `401` for [Unauthorized], `403` for [Forbidden].
 */
sealed interface AuthorizationResult {

    data class Authorized(val system: ExternalSystem) : AuthorizationResult

    data class Unauthorized(val reason: String) : AuthorizationResult

    data class Forbidden(val reason: String) : AuthorizationResult

    companion object {
        const val MISSING_CREDENTIAL = "missing bearer credential"
        const val INVALID_CREDENTIAL = "invalid bearer credential"
        const val INACTIVE_SYSTEM = "inactive external system"
    }
}
