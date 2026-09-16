package dev.agner.chameidor.httpapi.controller

/**
 * Error body of the HTTP edge (`401`/`403`/`404`): `{"error": "…"}`.
 */
data class ErrorResponse(val error: String)
