package dev.agner.chameidor.usecase.auth

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

    /**
     * [caller] is the optional claimed identity that accompanies a credential; when
     * present it must match the system the credential resolves to. [target] is the
     * resource being accessed — opaque here for now, meaningful to the future provider.
     */
    suspend fun authorize(caller: String?, target: String, credential: String?): AuthorizationResult {
        if (credential.isNullOrBlank()) {
            return AuthorizationResult.Unauthorized(AuthorizationResult.MISSING_CREDENTIAL)
        }

        return externalSystems.findByTokenHash(TokenHasher.sha256(credential))?.let { system ->
            when {
                caller != null && caller != system.name ->
                    AuthorizationResult.Unauthorized(AuthorizationResult.INVALID_CREDENTIAL)
                !system.active -> AuthorizationResult.Forbidden(AuthorizationResult.INACTIVE_SYSTEM)
                else -> AuthorizationResult.Authorized(system)
            }
        } ?: AuthorizationResult.Unauthorized(AuthorizationResult.INVALID_CREDENTIAL)
    }
}
