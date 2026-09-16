package dev.agner.chameidor.usecase.auth

/**
 * A system allowed to talk to chameidor, registered in the `external_systems` table.
 * The registered token *is* the identity: nothing else about a caller is trusted.
 */
data class ExternalSystem(
    val name: String,
    val active: Boolean,
)
