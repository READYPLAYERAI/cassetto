package com.readyplayerai.cassetto.data.remote.dto

import kotlinx.serialization.Serializable

/** Response of `apiKeys.exchange`. `createdAt` is a superjson ISO-8601 string. */
@Serializable
data class ApiKeyDto(
    val id: String,
    val name: String,
    val key: String,
    val createdAt: String? = null,
    val scopes: List<String> = emptyList(),
)

/** Response of `GET /api/v1/users/me`. */
@Serializable
data class UserMeDto(
    val id: String,
    val name: String? = null,
    val email: String? = null,
    val image: String? = null,
    val localUser: Boolean = false,
)

/** Response of `GET /api/health`. No auth required. */
@Serializable
data class HealthDto(
    val status: String,
    val message: String? = null,
)

@Serializable
internal data class ExchangeRequest(
    val keyName: String,
    val email: String,
    val password: String,
)

@Serializable
internal data class ValidateRequest(val apiKey: String)

@Serializable
internal data class RevokeRequest(val id: String)
