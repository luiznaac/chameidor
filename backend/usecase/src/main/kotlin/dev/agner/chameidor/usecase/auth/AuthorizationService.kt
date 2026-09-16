package dev.agner.chameidor.usecase.auth

import dev.agner.chameidor.usecase.commons.logger
import org.springframework.stereotype.Service

/**
 * The single authorization seam of the service. Today an opaque Bearer credential is
 * resolved against the `external_systems` registry — registered and active means the
 * whole surface is allowed, all-or-nothing. A future central authz provider plugs in
 * here without changing the wire.
 */
@Service
class AuthorizationService(
    private val externalSystems: IExternalSystemRepository,
) {

    private val logger = logger()

    /**
     * [caller] is the identity a request claims in the `X-External-System` header.
     * With a credential, [caller] is an optional claim that must match the system the
     * credential resolves to; without one, [caller] is the deprecated alias that
     * authenticates by name alone (WARNING logged) until every caller has migrated to
     * Bearer — see docs/external-systems.md for the removal plan. [target] is the
     * resource being accessed — opaque here for now, meaningful to the future provider.
     */
    suspend fun authorize(caller: String?, target: String, credential: String?): AuthorizationResult {
        if (!credential.isNullOrBlank()) {
            return authorizeBearer(caller, credential)
        }

        if (caller.isNullOrBlank()) {
            return AuthorizationResult.Unauthorized(AuthorizationResult.MISSING_CREDENTIAL)
        }

        return authorizeAlias(caller)
    }

    private suspend fun authorizeBearer(caller: String?, credential: String): AuthorizationResult =
        externalSystems.findByTokenHash(TokenHasher.sha256(credential))
            ?.resolve(caller)
            ?: AuthorizationResult.Unauthorized(AuthorizationResult.INVALID_CREDENTIAL)

    private suspend fun authorizeAlias(caller: String): AuthorizationResult {
        val system = externalSystems.findByName(caller)
            ?: return AuthorizationResult.Unauthorized(AuthorizationResult.INVALID_CREDENTIAL)

        logger.warn(
            "System '{}' used the deprecated X-External-System alias; migrate to Authorization: Bearer",
            caller,
        )

        return system.resolve(caller)
    }

    private fun ExternalSystem.resolve(claimed: String?): AuthorizationResult = when {
        claimed != null && claimed != name -> AuthorizationResult.Unauthorized(AuthorizationResult.INVALID_CREDENTIAL)
        !active -> AuthorizationResult.Forbidden(AuthorizationResult.INACTIVE_SYSTEM)
        else -> AuthorizationResult.Authorized(this)
    }
}
